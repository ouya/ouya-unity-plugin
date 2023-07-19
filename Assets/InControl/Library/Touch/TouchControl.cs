using UnityEngine;


namespace InControl
{
	public abstract class TouchControl : MonoBehaviour
	{
		public enum ButtonTarget : int
		{
			None = 0,
			Action1 = InputControlType.Action1,
			Action2 = InputControlType.Action2,
			Action3 = InputControlType.Action3,
			Action4 = InputControlType.Action4,
			LeftTrigger = InputControlType.LeftTrigger,
			RightTrigger = InputControlType.RightTrigger,
			LeftBumper = InputControlType.LeftBumper,
			RightBumper = InputControlType.RightBumper,
			DPadDown = InputControlType.DPadDown,
			DPadLeft = InputControlType.DPadLeft,
			DPadRight = InputControlType.DPadRight,
			DPadUp = InputControlType.DPadUp,
			Menu = InputControlType.Menu,
			Button0 = InputControlType.Button0,
			Button1 = InputControlType.Button1,
			Button2 = InputControlType.Button2,
			Button3 = InputControlType.Button3,
			Button4 = InputControlType.Button4,
			Button5 = InputControlType.Button5,
			Button6 = InputControlType.Button6,
			Button7 = InputControlType.Button7,
			Button8 = InputControlType.Button8,
			Button9 = InputControlType.Button9
		}


		public enum AnalogTarget : int
		{
			None,
			LeftStick,
			RightStick,
			Both
		}


		public enum SnapAngles : int
		{
			None = 0,
			Four = 4,
			Eight = 8,
			Sixteen = 16
		}


		public abstract void CreateControl();
		public abstract void DestroyControl();
		public abstract void ConfigureControl();
		public abstract void SubmitControlState( ulong updateTick );
		public abstract void TouchBegan( Touch touch );
		public abstract void TouchMoved( Touch touch );
		public abstract void TouchEnded( Touch touch );
		public abstract void DrawGizmos();


		void OnEnable()
		{
			TouchManager.OnSetup += Setup;
		}


		void OnDisable()
		{
			DestroyControl();
			Resources.UnloadUnusedAssets();
		}


		void Setup()
		{
			CreateControl();
			ConfigureControl();
		}


		protected GameObject CreateSpriteGameObject( string name )
		{
			var spriteGameObject = new GameObject( name );
			spriteGameObject.transform.parent = transform;
			spriteGameObject.transform.localPosition = Vector3.zero;
			spriteGameObject.transform.localScale = Vector3.one;
			spriteGameObject.layer = LayerMask.NameToLayer( "UI" );
			return spriteGameObject;
		}


		protected SpriteRenderer CreateSpriteRenderer( GameObject spriteGameObject, Sprite sprite, int sortingOrder )
		{
			var spriteRenderer = spriteGameObject.AddComponent<SpriteRenderer>();
			spriteRenderer.sprite = sprite;
			spriteRenderer.sortingOrder = sortingOrder;
			spriteRenderer.sharedMaterial = new Material( Shader.Find( "Sprites/Default" ) );
			spriteRenderer.sharedMaterial.SetFloat( "PixelSnap", 1.0f );
			return spriteRenderer;
		}


		protected Vector3 OffsetToWorldPosition( TouchControlAnchor anchor, Vector2 offset, TouchUnitType offsetUnitType )
		{
			Vector3 worldOffset;
			if (offsetUnitType == TouchUnitType.Pixels)
			{
				worldOffset = TouchUtility.RoundVector( offset ) * TouchManager.PixelToWorld;
			}
			else
			{
				worldOffset = (Vector3) offset * TouchManager.PercentToWorld;
			}
			return TouchManager.ViewToWorldPoint( TouchUtility.AnchorToViewPoint( anchor ) ) + worldOffset;
		}


		protected void SubmitButtonState( ButtonTarget target, bool state, ulong updateTick )
		{
			if (TouchManager.Device == null || target == ButtonTarget.None)
			{
				return;
			}

			var control = TouchManager.Device.GetControl( (InputControlType) target );
			if (control != null)
			{
				control.UpdateWithState( state, updateTick );
			}
		}


		protected void SubmitAnalogValue( AnalogTarget target, Vector2 value, float lowerDeadZone, float upperDeadZone, ulong updateTick )
		{
			if (TouchManager.Device == null)
			{
				return;
			}

			if (target == AnalogTarget.LeftStick || target == AnalogTarget.Both)
			{
				TouchManager.Device.LeftStickX.LowerDeadZone = lowerDeadZone;
				TouchManager.Device.LeftStickX.UpperDeadZone = upperDeadZone;
				TouchManager.Device.LeftStickX.UpdateWithValue( value.x, updateTick );

				TouchManager.Device.LeftStickY.LowerDeadZone = lowerDeadZone;
				TouchManager.Device.LeftStickY.UpperDeadZone = upperDeadZone;
				TouchManager.Device.LeftStickY.UpdateWithValue( value.y, updateTick );
			}
			
			if (target == AnalogTarget.RightStick || target == AnalogTarget.Both)
			{
				TouchManager.Device.RightStickX.LowerDeadZone = lowerDeadZone;
				TouchManager.Device.RightStickX.UpperDeadZone = upperDeadZone;
				TouchManager.Device.RightStickX.UpdateWithValue( value.x, updateTick );
				
				TouchManager.Device.RightStickY.LowerDeadZone = lowerDeadZone;
				TouchManager.Device.RightStickY.UpperDeadZone = upperDeadZone;
				TouchManager.Device.RightStickY.UpdateWithValue( value.y, updateTick );
			}
		}


		protected void SubmitRawAnalogValue( AnalogTarget target, Vector2 rawValue, ulong updateTick )
		{
			if (TouchManager.Device == null)
			{
				return;
			}
			
			if (target == AnalogTarget.LeftStick || target == AnalogTarget.Both)
			{
				TouchManager.Device.LeftStickX.UpdateWithValue( rawValue.x, updateTick );				
				TouchManager.Device.LeftStickY.UpdateWithValue( rawValue.y, updateTick );
			}
			
			if (target == AnalogTarget.RightStick || target == AnalogTarget.Both)
			{
				TouchManager.Device.RightStickX.UpdateWithValue( rawValue.x, updateTick );				
				TouchManager.Device.RightStickY.UpdateWithValue( rawValue.y, updateTick );
			}
		}


		protected static Vector2 SnapTo( Vector2 vector, SnapAngles snapAngles )
		{
			if (snapAngles == SnapAngles.None)
			{
				return vector;
			}
			
			var snapAngle = 360.0f / ((int) snapAngles);
			
			return SnapTo( vector, snapAngle );
		}

		
		protected static Vector2 SnapTo( Vector2 vector, float snapAngle )
		{
			float angle = Vector2.Angle( vector, Vector2.up );
			
			if (angle < snapAngle / 2.0f)
			{
				return Vector2.up * vector.magnitude;
			}
			
			if (angle > 180.0f - snapAngle / 2.0f)
			{
				return -Vector2.up * vector.magnitude;
			}
			
			var t = Mathf.Round( angle / snapAngle );
			var deltaAngle = (t * snapAngle) - angle;
			var axis = Vector3.Cross( Vector2.up, vector );
			var q = Quaternion.AngleAxis( deltaAngle, axis );
			
			return q * vector;
		}


		void OnDrawGizmosSelected()
		{
			if (!enabled)
			{
				return;
			}

			if (TouchManager.ControlsShowGizmos != TouchManager.GizmoShowOption.WhenSelected)
			{
				return;
			}

			if (Utility.GameObjectIsCulledOnCurrentCamera( gameObject ))
			{
				return;
			}

			if (!Application.isPlaying)
			{
				ConfigureControl();
			}

			DrawGizmos();
		}


		void OnDrawGizmos()
		{
			if (!enabled)
			{
				return;
			}

			if (TouchManager.ControlsShowGizmos == TouchManager.GizmoShowOption.UnlessPlaying)
			{
				if (Application.isPlaying)
				{
					return;
				}
			}
			else
			if (TouchManager.ControlsShowGizmos != TouchManager.GizmoShowOption.Always)
			{
				return;
			}

			if (Utility.GameObjectIsCulledOnCurrentCamera( gameObject ))
			{
				return;
			}

			if (!Application.isPlaying)
			{
				ConfigureControl();
			}

			DrawGizmos();
		}
	}
}

