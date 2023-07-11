using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

#if UNITY_ANDROID && INCONTROL_OUYA && !UNITY_EDITOR
using tv.ouya.console.api;
#endif


namespace InControl
{
	public class OuyaEverywhereDeviceManager : InputDeviceManager
	{
		bool[] deviceConnected = new bool[] { false, false, false, false };


		public OuyaEverywhereDeviceManager()
		{
			for (int deviceIndex = 0; deviceIndex < 4; deviceIndex++)
			{
				devices.Add( new OuyaEverywhereDevice( deviceIndex ) );
			}
		}


		public override void Update( ulong updateTick, float deltaTime )
		{
			for (int deviceIndex = 0; deviceIndex < 4; deviceIndex++)
			{
				var device = devices[deviceIndex] as OuyaEverywhereDevice;

				if (device.IsConnected != deviceConnected[deviceIndex])
				{
					if (device.IsConnected)
					{
						device.BeforeAttach();
						InputManager.AttachDevice( device );
					}
					else
					{
						InputManager.DetachDevice( device );
					}

					deviceConnected[deviceIndex] = device.IsConnected;
				}
			}
		}


		public static void Enable()
		{
			#if UNITY_ANDROID && INCONTROL_OUYA && !UNITY_EDITOR
			if (OuyaSDK.isRunningOnOUYASupportedHardware())
			{
				InputManager.AddDeviceManager<OuyaEverywhereDeviceManager>();
			}
			#endif
		}
	}
}

