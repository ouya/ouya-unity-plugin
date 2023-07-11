using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

#if UNITY_ANDROID && INCONTROL_OUYA && !UNITY_EDITOR
using tv.ouya.console.api;
#endif


namespace InControl
{
	public class OuyaEverywhereDevice : InputDevice
	{
		const float LowerDeadZone = 0.2f;
		const float UpperDeadZone = 0.9f;

		public int DeviceIndex { get; private set; }


		public OuyaEverywhereDevice( int deviceIndex )
			: base( "OUYA Controller" )
		{
			DeviceIndex = deviceIndex;
			SortOrder = deviceIndex;

			Meta = "OUYA Everywhere Device #" + deviceIndex;

			AddControl( InputControlType.LeftStickX, "LeftStickX" );
			AddControl( InputControlType.LeftStickY, "LeftStickY" );
			AddControl( InputControlType.RightStickX, "RightStickX" );
			AddControl( InputControlType.RightStickY, "RightStickY" );

			AddControl( InputControlType.LeftTrigger, "LeftTrigger" );
			AddControl( InputControlType.RightTrigger, "RightTrigger" );

			AddControl( InputControlType.DPadUp, "DPadUp" );
			AddControl( InputControlType.DPadDown, "DPadDown" );
			AddControl( InputControlType.DPadLeft, "DPadLeft" );
			AddControl( InputControlType.DPadRight, "DPadRight" );

			AddControl( InputControlType.Action1, "O" );
			AddControl( InputControlType.Action2, "A" );
			AddControl( InputControlType.Action3, "Y" );
			AddControl( InputControlType.Action4, "U" );

			AddControl( InputControlType.LeftBumper, "LeftBumper" );
			AddControl( InputControlType.RightBumper, "RightBumper" );

			AddControl( InputControlType.LeftStickButton, "LeftStickButton" );
			AddControl( InputControlType.RightStickButton, "RightStickButton" );

			AddControl( InputControlType.Menu, "Menu" );
		}


		public void BeforeAttach()
		{
			#if UNITY_ANDROID && INCONTROL_OUYA && !UNITY_EDITOR
			Name = OuyaController.getControllerByPlayer( DeviceIndex ).getDeviceName();
			#endif
		}


		public override void Update( ulong updateTick, float deltaTime )
		{

			#if UNITY_ANDROID && INCONTROL_OUYA && !UNITY_EDITOR
			var lsv = Utility.ApplyCircularDeadZone( 
				          OuyaSDK.OuyaInput.GetAxisRaw( DeviceIndex, OuyaController.AXIS_LS_X ), 
				          -OuyaSDK.OuyaInput.GetAxisRaw( DeviceIndex, OuyaController.AXIS_LS_Y ), 
				          LowerDeadZone, 
				          UpperDeadZone
			          );
			UpdateWithValue( InputControlType.LeftStickX, lsv.x, updateTick );
			UpdateWithValue( InputControlType.LeftStickY, lsv.y, updateTick );

			var rsv = Utility.ApplyCircularDeadZone( 
				          OuyaSDK.OuyaInput.GetAxisRaw( DeviceIndex, OuyaController.AXIS_RS_X ), 
				          -OuyaSDK.OuyaInput.GetAxisRaw( DeviceIndex, OuyaController.AXIS_RS_Y ), 
				          LowerDeadZone, 
				          UpperDeadZone
			          );
			UpdateWithValue( InputControlType.RightStickX, rsv.x, updateTick );
			UpdateWithValue( InputControlType.RightStickY, rsv.y, updateTick );

			var lt = Utility.ApplyDeadZone(
				         OuyaSDK.OuyaInput.GetAxisRaw( DeviceIndex, OuyaController.AXIS_L2 ),
				         LowerDeadZone,
				         UpperDeadZone 
			         );
			UpdateWithValue( InputControlType.LeftTrigger, lt, updateTick );

			var rt = Utility.ApplyDeadZone(
				         OuyaSDK.OuyaInput.GetAxisRaw( DeviceIndex, OuyaController.AXIS_R2 ),
				         LowerDeadZone,
				         UpperDeadZone 
			         );
			UpdateWithValue( InputControlType.RightTrigger, rt, updateTick );

			UpdateWithState( InputControlType.DPadUp, OuyaSDK.OuyaInput.GetButton( DeviceIndex, OuyaController.BUTTON_DPAD_UP ), updateTick );
			UpdateWithState( InputControlType.DPadDown, OuyaSDK.OuyaInput.GetButton( DeviceIndex, OuyaController.BUTTON_DPAD_DOWN ), updateTick );
			UpdateWithState( InputControlType.DPadLeft, OuyaSDK.OuyaInput.GetButton( DeviceIndex, OuyaController.BUTTON_DPAD_LEFT ), updateTick );
			UpdateWithState( InputControlType.DPadRight, OuyaSDK.OuyaInput.GetButton( DeviceIndex, OuyaController.BUTTON_DPAD_RIGHT ), updateTick );

			UpdateWithState( InputControlType.Action1, OuyaSDK.OuyaInput.GetButton( DeviceIndex, OuyaController.BUTTON_O ), updateTick );
			UpdateWithState( InputControlType.Action2, OuyaSDK.OuyaInput.GetButton( DeviceIndex, OuyaController.BUTTON_A ), updateTick );
			UpdateWithState( InputControlType.Action3, OuyaSDK.OuyaInput.GetButton( DeviceIndex, OuyaController.BUTTON_U ), updateTick );
			UpdateWithState( InputControlType.Action4, OuyaSDK.OuyaInput.GetButton( DeviceIndex, OuyaController.BUTTON_Y ), updateTick );

			UpdateWithState( InputControlType.LeftBumper, OuyaSDK.OuyaInput.GetButton( DeviceIndex, OuyaController.BUTTON_L1 ), updateTick );
			UpdateWithState( InputControlType.RightBumper, OuyaSDK.OuyaInput.GetButton( DeviceIndex, OuyaController.BUTTON_R1 ), updateTick );

			UpdateWithState( InputControlType.LeftStickButton, OuyaSDK.OuyaInput.GetButton( DeviceIndex, OuyaController.BUTTON_L3 ), updateTick );
			UpdateWithState( InputControlType.RightStickButton, OuyaSDK.OuyaInput.GetButton( DeviceIndex, OuyaController.BUTTON_R3 ), updateTick );

			UpdateWithState( InputControlType.Menu, OuyaSDK.OuyaInput.GetButtonDown( DeviceIndex, OuyaController.BUTTON_MENU ), updateTick );
			#endif
		}


		public bool IsConnected
		{
			get
			{ 
				#if UNITY_ANDROID && INCONTROL_OUYA && !UNITY_EDITOR
				return OuyaSDK.OuyaInput.IsControllerConnected( DeviceIndex ); 
				#else
				return false;
				#endif
			}
		}
	}
}

