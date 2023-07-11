#if UNITY_4_6
using UnityEngine;
using UnityEngine.EventSystems;
using InControl;


namespace InControl
{
	[AddComponentMenu( "Event/InControl Input Module" )]
	public class InControlInputModule : PointerInputModule
	{
		public enum Button : int
		{
			Action1 = InputControlType.Action1,
			Action2 = InputControlType.Action2,
			Action3 = InputControlType.Action3,
			Action4 = InputControlType.Action4
		}


		private enum InputSource : int
		{
			InControl,
			Mouse
		}


		public Button submitButton = Button.Action1;
		public Button cancelButton = Button.Action2;
		[Range( 0, 1 )]
		public float analogMoveThreshold = 0.5f;
		public float moveRepeatFirstDuration = 0.8f;
		public float moveRepeatDelayDuration = 0.1f;
		public bool allowActivationOnMobileDevice = true;

		InputSource currentInputSource;
		Vector3 thisMousePosition;
		Vector3 lastMousePosition;
		Vector2 thisVectorState;
		Vector2 lastVectorState;
		bool thisSubmitState;
		bool lastSubmitState;
		bool thisCancelState;
		bool lastCancelState;
		float nextMoveRepeatTime;
		float lastVectorPressedTime;


		protected InControlInputModule()
		{
			TwoAxisInputControl.StateThreshold = analogMoveThreshold;
			currentInputSource = InputSource.InControl;
		}


		public override bool IsModuleSupported()
		{
			return allowActivationOnMobileDevice || !Application.isMobilePlatform;
		}


		public override bool ShouldActivateModule()
		{
			if (!base.ShouldActivateModule())
			{
				return false;
			}

			UpdateInputState();

			var shouldActivate = false;
			shouldActivate |= SubmitWasPressed;
			shouldActivate |= CancelWasPressed;
			shouldActivate |= VectorWasPressed;
			shouldActivate |= MouseHasMoved();
			shouldActivate |= MouseButtonIsPressed();
			return shouldActivate;
		}


		void UpdateInputState()
		{
			lastVectorState = thisVectorState;
			thisVectorState = Vector2.zero;

			if (Mathf.Abs( Direction.X ) > analogMoveThreshold)
			{
				thisVectorState.x = Mathf.Sign( Direction.X );
			}

			if (Mathf.Abs( Direction.Y ) > analogMoveThreshold)
			{
				thisVectorState.y = Mathf.Sign( Direction.Y );
			}

			if (VectorIsReleased)
			{
				nextMoveRepeatTime = 0.0f;
			}

			if (VectorIsPressed)
			{
				if (lastVectorState == Vector2.zero)
				{
					if (Time.realtimeSinceStartup > lastVectorPressedTime + 0.1f)
					{
						nextMoveRepeatTime = Time.realtimeSinceStartup + moveRepeatFirstDuration;
					}
					else
					{
						nextMoveRepeatTime = Time.realtimeSinceStartup + moveRepeatDelayDuration;
					}
				}

				lastVectorPressedTime = Time.realtimeSinceStartup;
			}

			lastSubmitState = thisSubmitState;
			thisSubmitState = SubmitButton.IsPressed;

			lastCancelState = thisCancelState;
			thisCancelState = CancelButton.IsPressed;
		}


		void SetVectorRepeatTimer()
		{
			nextMoveRepeatTime = Mathf.Max( nextMoveRepeatTime, Time.realtimeSinceStartup + moveRepeatDelayDuration );
		}


		bool VectorIsPressed
		{
			get
			{
				return thisVectorState != Vector2.zero;
			}
		}


		bool VectorIsReleased
		{
			get
			{
				return thisVectorState == Vector2.zero;
			}
		}


		bool VectorHasChanged
		{
			get
			{
				return thisVectorState != lastVectorState;
			}
		}


		bool VectorWasPressed
		{
			get
			{
				if (VectorIsPressed && Time.realtimeSinceStartup > nextMoveRepeatTime)
				{
					return true;
				}

				return VectorIsPressed && lastVectorState == Vector2.zero;
			}
		}


		bool SubmitWasPressed
		{
			get
			{
				return thisSubmitState && thisSubmitState != lastSubmitState;
			}
		}


		bool CancelWasPressed
		{
			get
			{
				return thisCancelState && thisCancelState != lastCancelState;
			}
		}


		public override void ActivateModule()
		{
			base.ActivateModule();

			thisMousePosition = Input.mousePosition;
			lastMousePosition = Input.mousePosition;

			var baseEventData = GetBaseEventData();
			var gameObject = eventSystem.currentSelectedObject;
			if (gameObject == null)
			{
				gameObject = eventSystem.lastSelectedObject;
			}
			if (gameObject == null)
			{
				gameObject = eventSystem.firstSelectedObject;
			}
			eventSystem.SetSelectedGameObject( null, baseEventData );
			eventSystem.SetSelectedGameObject( gameObject, baseEventData );
		}


		public override void DeactivateModule()
		{
			base.DeactivateModule();
			base.ClearSelection();
		}


		public override void Process()
		{
			var used = SendUpdateEventToSelectedObject();

			if (!used)
			{
				used = SendVectorEventToSelectedObject();
			}

			if (!used)
			{
				SendButtonEventToSelectedObject();
			}

			ProcessMouseEvent();
		}


		bool SendButtonEventToSelectedObject()
		{
			if (eventSystem.currentSelectedObject == null || currentInputSource != InputSource.InControl)
			{
				return false;
			}

			var baseEventData = GetBaseEventData();

			if (SubmitWasPressed)
			{
				ExecuteEvents.Execute( eventSystem.currentSelectedObject, baseEventData, ExecuteEvents.submitHandler );
			}

			if (CancelWasPressed)
			{
				ExecuteEvents.Execute( eventSystem.currentSelectedObject, baseEventData, ExecuteEvents.cancelHandler );
			}

			return baseEventData.used;
		}


		bool SendVectorEventToSelectedObject()
		{
			if (!VectorWasPressed)
			{
				return false;
			}

			var axisEventData = GetAxisEventData( thisVectorState.x, thisVectorState.y, 0.5f );

			if (axisEventData.moveDir != MoveDirection.None)
			{
				if (currentInputSource != InputSource.InControl)
				{
					currentInputSource = InputSource.InControl;
					if (ResetSelection())
					{
						return true;
					}
				}

				ExecuteEvents.Execute( eventSystem.currentSelectedObject, axisEventData, ExecuteEvents.moveHandler );

				SetVectorRepeatTimer();
			}

			return axisEventData.used;
		}


		void ProcessMouseEvent()
		{
			var mouseButtonDown = Input.GetMouseButtonDown( 0 );
			var mouseButtonUp = Input.GetMouseButtonUp( 0 );
			var mousePointerEventData = GetMousePointerEventData();
			var mouseScrollAxis = Input.GetAxis( "mouse z" ) * 0.01f;
			mousePointerEventData.scrollDelta.x = 0;
			mousePointerEventData.scrollDelta.y = mouseScrollAxis;
			if (!UseMouse( mouseButtonDown, mouseButtonUp, mousePointerEventData ))
			{
				return;
			}
			ProcessMousePress( mousePointerEventData, mouseButtonDown, mouseButtonUp );
			ProcessMove( mousePointerEventData );
			if (!Mathf.Approximately( mouseScrollAxis, 0.0f ))
			{
				var eventHandler = ExecuteEvents.GetEventHandler<IScrollHandler>( mousePointerEventData.pointerCurrentRaycast.go );
				ExecuteEvents.ExecuteHierarchy<IScrollHandler>( eventHandler, mousePointerEventData, ExecuteEvents.scrollHandler );
			}
		}


		void ProcessMousePress( PointerEventData pointerEvent, bool pressed, bool released )
		{
			var pointerGameObject = pointerEvent.pointerCurrentRaycast.go;

			if (pressed)
			{
				pointerEvent.eligibleForClick = true;
				pointerEvent.delta = Vector2.zero;
				pointerEvent.pressPosition = pointerEvent.position;
				pointerEvent.pointerPressRaycast = pointerEvent.pointerCurrentRaycast;
				var gameObject = ExecuteEvents.ExecuteHierarchy<IPointerDownHandler>( pointerGameObject, pointerEvent, ExecuteEvents.pointerDownHandler );
				if (gameObject == null)
				{
					gameObject = ExecuteEvents.GetEventHandler<IPointerClickHandler>( pointerGameObject );
				}
				if (gameObject != pointerEvent.pointerPress)
				{
					pointerEvent.pointerPress = gameObject;
					pointerEvent.rawPointerPress = pointerGameObject;
					pointerEvent.clickCount = 0;
				}
				pointerEvent.pointerDrag = ExecuteEvents.GetEventHandler<IDragHandler>( pointerGameObject );
				if (pointerEvent.pointerDrag != null)
				{
					ExecuteEvents.Execute<IBeginDragHandler>( pointerEvent.pointerDrag, pointerEvent, ExecuteEvents.beginDragHandler );
				}
				var eventHandler = ExecuteEvents.GetEventHandler<ISelectHandler>( pointerGameObject );
				eventSystem.SetSelectedGameObject( eventHandler, pointerEvent );
			}

			if (released)
			{
				ExecuteEvents.Execute<IPointerUpHandler>( pointerEvent.pointerPress, pointerEvent, ExecuteEvents.pointerUpHandler );
				GameObject eventHandler2 = ExecuteEvents.GetEventHandler<IPointerClickHandler>( pointerGameObject );
				if (pointerEvent.pointerPress == eventHandler2 && pointerEvent.eligibleForClick)
				{
					var unscaledTime = Time.unscaledTime;
					if (unscaledTime - pointerEvent.clickTime < 0.3f)
					{
						pointerEvent.clickCount++;
					}
					else
					{
						pointerEvent.clickCount = 1;
					}
					pointerEvent.clickTime = unscaledTime;
					ExecuteEvents.Execute<IPointerClickHandler>( pointerEvent.pointerPress, pointerEvent, ExecuteEvents.pointerClickHandler );
				}
				else
				{
					if (pointerEvent.pointerDrag != null)
					{
						ExecuteEvents.ExecuteHierarchy<IDropHandler>( pointerGameObject, pointerEvent, ExecuteEvents.dropHandler );
					}
				}
				pointerEvent.eligibleForClick = false;
				pointerEvent.pointerPress = null;
				pointerEvent.rawPointerPress = null;
				if (pointerEvent.pointerDrag != null)
				{
					ExecuteEvents.Execute<IEndDragHandler>( pointerEvent.pointerDrag, pointerEvent, ExecuteEvents.endDragHandler );
				}
				pointerEvent.pointerDrag = null;
				HandlePointerExitAndEnter( pointerEvent, null );
				HandlePointerExitAndEnter( pointerEvent, pointerGameObject );
			}
		}


		protected override void ProcessMove( PointerEventData pointerEvent )
		{
			base.ProcessMove( pointerEvent );
			var pointerGameObject = pointerEvent.pointerCurrentRaycast.go;
			base.HandlePointerExitAndEnter( pointerEvent, pointerGameObject );
		}


		bool ResetSelection()
		{
			var baseEventData = GetBaseEventData();
			var lastPointerEventData = base.GetLastPointerEventData( -1 );
			var rootGameObject = (lastPointerEventData != null) ? lastPointerEventData.pointerEnter : null;
			base.HandlePointerExitAndEnter( lastPointerEventData, null );
			base.eventSystem.SetSelectedGameObject( null, baseEventData );
			var result = false;
			var gameObject = ExecuteEvents.GetEventHandler<ISelectHandler>( rootGameObject );
			if (gameObject == null)
			{
				gameObject = eventSystem.lastSelectedObject;
				result = true;
			}
			base.eventSystem.SetSelectedGameObject( gameObject, baseEventData );
			return result;
		}


		bool SendUpdateEventToSelectedObject()
		{
			if (base.eventSystem.currentSelectedObject == null)
			{
				return false;
			}
			var baseEventData = GetBaseEventData();
			ExecuteEvents.Execute<IUpdateSelectedHandler>( base.eventSystem.currentSelectedObject, baseEventData, ExecuteEvents.updateSelectedHandler );
			return baseEventData.used;
		}


		public override void UpdateModule()
		{
			lastMousePosition = thisMousePosition;
			thisMousePosition = Input.mousePosition;
		}


		bool UseMouse( bool pressed, bool released, PointerEventData pointerData )
		{
			if (currentInputSource == InputSource.Mouse)
			{
				return true;
			}

			if (pressed || released || pointerData.IsPointerMoving() || pointerData.IsScrolling())
			{
				currentInputSource = InputSource.Mouse;
				base.eventSystem.SetSelectedGameObject( null, pointerData );
			}

			return currentInputSource == InputSource.Mouse;
		}


		bool MouseHasMoved()
		{
			return (thisMousePosition - lastMousePosition).sqrMagnitude > 0.0f;
		}


		bool MouseButtonIsPressed()
		{
			return Input.GetMouseButtonDown( 0 );
		}


		InputDevice Device
		{
			get
			{
				return InputManager.ActiveDevice;
			}
		}


		TwoAxisInputControl Direction
		{
			get
			{
				return Device.Direction;
			}
		}


		InputControl SubmitButton
		{
			get
			{
				return Device.GetControl( (InputControlType) submitButton );
			}
		}


		InputControl CancelButton
		{
			get
			{
				return Device.GetControl( (InputControlType) cancelButton );
			}
		}
	}
}
#endif

