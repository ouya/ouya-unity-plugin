using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using UnityEngine;


namespace InControl
{
	[ExecuteInEditMode]
	public class TouchManager : SingletonMonoBehavior<TouchManager>
	{
		const int MaxTouches = 16;

		public enum GizmoShowOption
		{
			Never,
			WhenSelected,
			UnlessPlaying,
			Always
		}

		[Space( 10 )]

		public Camera touchCamera;
		public GizmoShowOption controlsShowGizmos = GizmoShowOption.Always;

		[SerializeField, HideInInspector]
		bool _controlsEnabled = true;

		public static event Action OnSetup;

		InputDevice device;
		Vector3 viewSize;
		Vector2 screenSize;
		Vector2 halfScreenSize;
		float percentToWorld;
		float halfPercentToWorld;
		float pixelToWorld;
		float halfPixelToWorld;
		TouchControl[] touchControls;
		Touch[] cachedTouches;
		Touch mouseTouch;
		List<Touch> activeTouches;
		ReadOnlyCollection<Touch> readOnlyActiveTouches;
		Vector2 lastMousePosition;


		protected TouchManager()
		{
		}


		void OnEnable()
		{
			SetSingletonInstance();

			touchControls = GetComponentsInChildren<TouchControl>( true );

			if (Application.isPlaying)
			{
				InputManager.OnSetup += Setup;
				InputManager.OnUpdate += UpdateTouches;
			}
		}


		void OnDisable()
		{
			if (Application.isPlaying)
			{
				InputManager.OnSetup -= Setup;
				InputManager.OnUpdate -= UpdateTouches;
			}

			Reset();
		}


		void Setup()
		{
			Input.simulateMouseWithTouches = false;

			UpdateScreenSize( new Vector2( Screen.width, Screen.height ) );

			CreateDevice();
			CreateTouches();

			if (OnSetup != null)
			{
				OnSetup.Invoke();
				OnSetup = null;
			}
		}


		void Reset()
		{
			device = null;
			mouseTouch = null;
			cachedTouches = null;
			activeTouches = null;
			readOnlyActiveTouches = null;
			touchControls = null;
		}


		void Update()
		{
			if (OnSetup != null)
			{
				OnSetup.Invoke();
				OnSetup = null;
			}

			var currentScreenSize = new Vector2( Screen.width, Screen.height );
			if (screenSize != currentScreenSize)
			{
				UpdateScreenSize( currentScreenSize );
			}
		}


		void CreateDevice()
		{
			device = new InputDevice( "TouchDevice" );

			device.AddControl( InputControlType.LeftStickX, "LeftStickX" );
			device.AddControl( InputControlType.LeftStickY, "LeftStickY" );
			device.AddControl( InputControlType.RightStickX, "RightStickX" );
			device.AddControl( InputControlType.RightStickY, "RightStickY" );
			device.AddControl( InputControlType.LeftTrigger, "LeftTrigger" );
			device.AddControl( InputControlType.RightTrigger, "RightTrigger" );
			device.AddControl( InputControlType.DPadUp, "DPadUp" );
			device.AddControl( InputControlType.DPadDown, "DPadDown" );
			device.AddControl( InputControlType.DPadLeft, "DPadLeft" );
			device.AddControl( InputControlType.DPadRight, "DPadRight" );
			device.AddControl( InputControlType.Action1, "Action1" );
			device.AddControl( InputControlType.Action2, "Action2" );
			device.AddControl( InputControlType.Action3, "Action3" );
			device.AddControl( InputControlType.Action4, "Action4" );
			device.AddControl( InputControlType.LeftBumper, "LeftBumper" );
			device.AddControl( InputControlType.RightBumper, "RightBumper" );
			device.AddControl( InputControlType.Menu, "Menu" );

			InputManager.AttachDevice( device );
		}


		void UpdateScreenSize( Vector2 currentScreenSize )
		{
			screenSize = currentScreenSize;
			halfScreenSize = screenSize / 2.0f;

			viewSize = ConvertViewToWorldPoint( Vector2.one ) * 0.02f;
			percentToWorld = Mathf.Min( viewSize.x, viewSize.y );
			halfPercentToWorld = percentToWorld / 2.0f;

			halfPixelToWorld = touchCamera.orthographicSize / screenSize.y;
			pixelToWorld = halfPixelToWorld * 2.0f;

			if (touchControls != null)
			{
				var touchControlCount = touchControls.Length;
				for (int i = 0; i < touchControlCount; i++)
				{
					touchControls[i].ConfigureControl();
				}
			}
		}


		/*
		void OnDrawGizmos()
		{
			Gizmos.color = Color.white;
			Gizmos.DrawLine( Vector3.zero, Vector3.one * PercentToWorld * -50.0f );

			Gizmos.color = Color.red;
			Gizmos.DrawLine( Vector3.zero, Vector3.one * PixelToWorld * 64.0f );

			Utility.DrawRectGizmo( PercentToWorldRect( new Rect( 0, 0, 100, 100 ) ), Color.cyan );
			Utility.DrawRectGizmo( PixelToWorldRect( new Rect( 0, 0, 64, 64 ) ), Color.magenta );
		}
		*/


		public bool controlsEnabled
		{
			get
			{
				return _controlsEnabled;
			}

			set
			{
				if (_controlsEnabled != value)
				{
					var touchControlCount = touchControls.Length;
					for (int i = 0; i < touchControlCount; i++)
					{
						touchControls[i].enabled = value;
					}

					_controlsEnabled = value;
				}
			}
		}


		void SendTouchBegan( Touch touch )
		{
			var touchControlCount = touchControls.Length;
			for (int i = 0; i < touchControlCount; i++)
			{
				var touchControl = touchControls[i];
				if (touchControl.enabled && touchControl.gameObject.activeInHierarchy)
				{
					touchControl.TouchBegan( touch );
				}
			}
		}


		void SendTouchMoved( Touch touch )
		{
			var touchControlCount = touchControls.Length;
			for (int i = 0; i < touchControlCount; i++)
			{
				var touchControl = touchControls[i];
				if (touchControl.enabled && touchControl.gameObject.activeInHierarchy)
				{
					touchControl.TouchMoved( touch );
				}
			}
		}


		void SendTouchEnded( Touch touch )
		{
			var touchControlCount = touchControls.Length;
			for (int i = 0; i < touchControlCount; i++)
			{
				var touchControl = touchControls[i];
				if (touchControl.enabled && touchControl.gameObject.activeInHierarchy)
				{
					touchControl.TouchEnded( touch );
				}
			}
		}


		void SubmitControlStates( ulong updateTick )
		{
			var touchControlCount = touchControls.Length;
			for (int i = 0; i < touchControlCount; i++)
			{
				var touchControl = touchControls[i];
				if (touchControl.enabled && touchControl.gameObject.activeInHierarchy)
				{
					touchControl.SubmitControlState( updateTick );
				}
			}
		}


		void CreateTouches()
		{
			cachedTouches = new Touch[MaxTouches];
			for (int i = 0; i < MaxTouches; i++)
			{
				cachedTouches[i] = new Touch( i );
			}
			mouseTouch = cachedTouches[MaxTouches - 1];
			activeTouches = new List<Touch>( MaxTouches );
			readOnlyActiveTouches = new ReadOnlyCollection<Touch>( activeTouches );
		}


		void UpdateTouches( ulong updateTick, float deltaTime )
		{
			activeTouches.Clear();

			if (mouseTouch.SetWithMouseData( updateTick, deltaTime ))
			{
				activeTouches.Add( mouseTouch );
			}

			for (int i = 0; i < Input.touchCount; i++)
			{
				var unityTouch = Input.GetTouch( i );
				var cacheTouch = cachedTouches[unityTouch.fingerId];
				cacheTouch.SetWithTouchData( unityTouch, updateTick, deltaTime );
				activeTouches.Add( cacheTouch );
			}

			// Find any touches that Unity may have "forgotten" to end properly.
			for (int i = 0; i < MaxTouches; i++)
			{
				var touch = cachedTouches[i];
				if (touch.phase != TouchPhase.Ended && touch.updateTick != updateTick)
				{
					touch.phase = TouchPhase.Ended;
					activeTouches.Add( touch );
				}
			}

			InvokeTouchEvents();
			SubmitControlStates( updateTick );
		}


		void InvokeTouchEvents()
		{
			var touchCount = activeTouches.Count;
			for (int i = 0; i < touchCount; i++)
			{
				var touch = activeTouches[i];
				switch (touch.phase)
				{
					case TouchPhase.Began:
						SendTouchBegan( touch );
						break;

					case TouchPhase.Moved:
						SendTouchMoved( touch );
						break;

					case TouchPhase.Ended:
						SendTouchEnded( touch );
						break;

					case TouchPhase.Canceled:
						SendTouchEnded( touch );
						break;
				}
			}
		}


		Vector3 ConvertScreenToWorldPoint( Vector2 point )
		{
			if (touchCamera == null)
			{
				return Vector3.zero;
			}
			return touchCamera.ScreenToWorldPoint( new Vector3( point.x, point.y, -touchCamera.transform.position.z ) );
		}

		
		Vector3 ConvertViewToWorldPoint( Vector2 point )
		{
			if (touchCamera == null)
			{
				return Vector3.zero;
			}
			return touchCamera.ViewportToWorldPoint( new Vector3( point.x, point.y, -touchCamera.transform.position.z ) );
		}


		#region Static interface.

		public static ReadOnlyCollection<Touch> Touches
		{
			get
			{ 
				return Instance.readOnlyActiveTouches; 
			}
		}


		public static int TouchCount
		{
			get
			{ 
				return Instance.activeTouches.Count; 
			}
		}


		public static Touch GetTouch( int touchIndex )
		{
			return Instance.activeTouches[touchIndex];
		}


		public static Touch GetTouchByFingerId( int fingerId )
		{
			return Instance.cachedTouches[fingerId];
		}


		public static Vector3 ScreenToWorldPoint( Vector2 point )
		{
			return Instance.ConvertScreenToWorldPoint( point );
		}

		
		public static Vector3 ViewToWorldPoint( Vector2 point )
		{
			return Instance.ConvertViewToWorldPoint( point );
		}


		public static float ConvertToWorld( float value, TouchUnitType unitType )
		{
			return value * (unitType == TouchUnitType.Pixels ? PixelToWorld : PercentToWorld);
		}


		public static Rect PercentToWorldRect( Rect rect )
		{
			return new Rect(
				(rect.xMin - 50.0f) * ViewSize.x,
				(rect.yMin - 50.0f) * ViewSize.y,
				rect.width * ViewSize.x,
				rect.height * ViewSize.y
			);
		}


		public static Rect PixelToWorldRect( Rect rect )
		{
			return new Rect(
				Mathf.Round( rect.xMin - HalfScreenSize.x ) * PixelToWorld,
				Mathf.Round( rect.yMin - HalfScreenSize.y ) * PixelToWorld,
				Mathf.Round( rect.width ) * PixelToWorld,
				Mathf.Round( rect.height ) * PixelToWorld
			);
		}


		public static Rect ConvertToWorld( Rect rect, TouchUnitType unitType )
		{
			return unitType == TouchUnitType.Pixels ? PixelToWorldRect( rect ) : PercentToWorldRect( rect );
		}


		public static Camera Camera
		{ 
			get
			{ 
				return Instance.touchCamera; 
			} 
		}


		public static InputDevice Device
		{ 
			get
			{ 
				return Instance.device; 
			} 
		}


		public static Vector3 ViewSize
		{ 
			get
			{ 
				return Instance.viewSize; 
			} 
		}


		public static float PercentToWorld
		{ 
			get
			{ 
				return Instance.percentToWorld; 
			} 
		}


		public static float HalfPercentToWorld
		{ 
			get
			{ 
				return Instance.halfPercentToWorld; 
			} 
		}


		public static float PixelToWorld
		{ 
			get
			{ 
				return Instance.pixelToWorld; 
			} 
		}


		public static float HalfPixelToWorld
		{ 
			get
			{ 
				return Instance.halfPixelToWorld; 
			} 
		}


		public static Vector2 ScreenSize
		{ 
			get
			{ 
				return Instance.screenSize; 
			} 
		}


		public static Vector2 HalfScreenSize
		{ 
			get
			{ 
				return Instance.halfScreenSize; 
			} 
		}


		public static GizmoShowOption ControlsShowGizmos
		{ 
			get
			{ 
				return Instance.controlsShowGizmos; 
			} 
		}


		public static bool ControlsEnabled
		{
			get
			{
				return Instance.controlsEnabled;
			}
			
			set
			{
				Instance.controlsEnabled = value;
			}
		}

		#endregion


		public static implicit operator bool( TouchManager instance )
		{
			return instance != null;
		}
	}
}

