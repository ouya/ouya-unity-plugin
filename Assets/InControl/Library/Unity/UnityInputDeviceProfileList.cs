using System;
using UnityEngine;


namespace InControl
{
	public class UnityInputDeviceProfileList : ScriptableObject
	{
		public static string[] Profiles = new string[] 
		{
			"InControl.AmazonFireGameController",
			"InControl.AmazonFireTvRemote",
			"InControl.GameStickLinuxProfile",
			"InControl.GameStickProfile",
			"InControl.GenericAndroidProfile",
			"InControl.OuyaLinuxProfile",
			"InControl.OuyaWinProfile",
			"InControl.Xbox360AndroidProfile",
			"InControl.Xbox360LinuxProfile",
			"InControl.Xbox360MacProfile",
			"InControl.Xbox360WinProfile",
			"InControl.XboxOneProfile",
			"InControl.XboxOneWinProfile",
		};
	}
}