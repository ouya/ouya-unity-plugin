/*
 * Copyright (C) 2012-2015 OUYA, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#if UNITY_ANDROID && !UNITY_EDITOR
using tv.ouya.console.api;
#endif
using System;
using UnityEngine;

public class VirtualController : MonoBehaviour
#if UNITY_ANDROID && !UNITY_EDITOR
	,
	OuyaSDK.IPauseListener,
	OuyaSDK.IResumeListener
#endif
{
    public SpriteRenderer button_menu = null;
    public SpriteRenderer button_a = null;
    public SpriteRenderer button_dpad_down = null;
    public SpriteRenderer button_dpad_left = null;
    public SpriteRenderer button_dpad_right = null;
    public SpriteRenderer button_dpad_up = null;
    public SpriteRenderer button_lb = null;
    public SpriteRenderer axis_lt = null;
    public SpriteRenderer axis_l_stick = null;
    public SpriteRenderer button_o = null;
    public SpriteRenderer button_rb = null;
    public SpriteRenderer axis_rt = null;
    public SpriteRenderer axis_r_stick = null;
    public SpriteRenderer axis_thumbl = null;
    public SpriteRenderer axis_thumbr = null;
    public SpriteRenderer button_u = null;
    public SpriteRenderer button_y = null;

    public int _PlayerNumber = 0; // controller #1

#pragma warning disable CS0414 //
    private DateTime _timerMenu = DateTime.MinValue;
#pragma warning restore CS0414 //

    private const float AXIS_SCALER = 0.05f;

    private const float DEADZONE = 0.25f;

#if UNITY_ANDROID && !UNITY_EDITOR

	void Awake()
	{
		OuyaSDK.registerPauseListener (this);
		OuyaSDK.registerResumeListener (this);
	}

	void OnDestroy()
	{
		OuyaSDK.unregisterPauseListener(this);
		OuyaSDK.unregisterResumeListener(this);
	}

#endif

	public void OuyaOnPause()
	{
		Debug.Log ("Example paused!");
	}
	
	public void OuyaOnResume()
	{
		Debug.Log ("Example resumed!");
	}

    public void OnGUI()
    {
        GUILayout.BeginVertical(GUILayout.Height(Screen.height));
        GUILayout.FlexibleSpace();
        GUILayout.BeginHorizontal(GUILayout.Width(Screen.width));
        GUILayout.FlexibleSpace();
		GUILayout.Label(string.Format ("Virtual Controller for Unity on {0}", OuyaSDK.getDeviceHardwareName()));
        GUILayout.FlexibleSpace();
        GUILayout.EndHorizontal();
        GUILayout.FlexibleSpace();
        GUILayout.EndVertical();
    }

#if UNITY_ANDROID && !UNITY_EDITOR

    private void HelperUpdateSprite(SpriteRenderer sr, int button)
    {
        if (OuyaSDK.OuyaInput.GetButtonDown(_PlayerNumber, button))
        {
            sr.gameObject.SetActive(true);
        }

        if (OuyaSDK.OuyaInput.GetButtonUp(_PlayerNumber, button))
        {
            sr.gameObject.SetActive(false);
        }
    }

    private void RotateInput(ref Vector2 input)
    {
        //rotate input by N degrees to match image
        float degrees = 225;
        float radians = degrees/180f*Mathf.PI;
        float cos = (float) Mathf.Cos(radians);
        float sin = (float) Mathf.Sin(radians);

        float x = input.x*cos - input.y*sin;
        float y = input.x*sin + input.y*cos;

        input.x = x;
        input.y = y;
    }

    public void Update()
    {
        HelperUpdateSprite(button_o, OuyaController.BUTTON_O);
        HelperUpdateSprite(button_u, OuyaController.BUTTON_U);
        HelperUpdateSprite(button_y, OuyaController.BUTTON_Y);
        HelperUpdateSprite(button_a, OuyaController.BUTTON_A);
        HelperUpdateSprite(button_lb, OuyaController.BUTTON_L1);
        HelperUpdateSprite(button_dpad_down, OuyaController.BUTTON_DPAD_DOWN);
        HelperUpdateSprite(button_dpad_left, OuyaController.BUTTON_DPAD_LEFT);
        HelperUpdateSprite(button_dpad_right, OuyaController.BUTTON_DPAD_RIGHT);
        HelperUpdateSprite(button_dpad_up, OuyaController.BUTTON_DPAD_UP);

        HelperUpdateSprite(button_rb, OuyaController.BUTTON_R1);

        Vector2 inputLeft;
        inputLeft.x = Mathf.Clamp(OuyaSDK.OuyaInput.GetAxis(_PlayerNumber, OuyaController.AXIS_LS_X), -1f, 1f);
        inputLeft.y = -Mathf.Clamp(OuyaSDK.OuyaInput.GetAxis(_PlayerNumber, OuyaController.AXIS_LS_Y), -1f, 1f);
        RotateInput(ref inputLeft);
        axis_l_stick.transform.localPosition = inputLeft * AXIS_SCALER;
        axis_thumbl.transform.localPosition = inputLeft * AXIS_SCALER;

        Vector2 inputRight;
        inputRight.x = Mathf.Clamp(OuyaSDK.OuyaInput.GetAxis(_PlayerNumber, OuyaController.AXIS_RS_X), -1f, 1f);
        inputRight.y = -Mathf.Clamp(OuyaSDK.OuyaInput.GetAxis(_PlayerNumber, OuyaController.AXIS_RS_Y), -1f, 1f);
        RotateInput(ref inputRight);
        axis_r_stick.transform.localPosition = inputRight * AXIS_SCALER;
        axis_thumbr.transform.localPosition = inputRight * AXIS_SCALER;

        HelperUpdateSprite(axis_thumbl, OuyaController.BUTTON_L3);
        HelperUpdateSprite(axis_thumbr, OuyaController.BUTTON_R3);

        float leftTrigger = Mathf.Clamp(OuyaSDK.OuyaInput.GetAxis(_PlayerNumber, OuyaController.AXIS_L2), 0f, 1f);
        if (Mathf.Abs(leftTrigger) > DEADZONE)
        {
            axis_lt.gameObject.SetActive(true);
        }
        else
        {
            axis_lt.gameObject.SetActive(false);
        }

        float leftRight = Mathf.Clamp(OuyaSDK.OuyaInput.GetAxis(_PlayerNumber, OuyaController.AXIS_R2), 0f, 1f);
        if (Mathf.Abs(leftRight) > DEADZONE)
        {
            axis_rt.gameObject.SetActive(true);
        }
        else
        {
            axis_rt.gameObject.SetActive(false);
        }

        if (OuyaSDK.OuyaInput.GetButtonDown(_PlayerNumber, OuyaController.BUTTON_MENU) ||
            OuyaSDK.OuyaInput.GetButtonUp(_PlayerNumber, OuyaController.BUTTON_MENU))
        {
            _timerMenu = DateTime.Now + TimeSpan.FromSeconds(1);
        }
        if (_timerMenu < DateTime.Now)
        {
            button_menu.gameObject.SetActive(false);
        }
        else
        {
            button_menu.gameObject.SetActive(true);
        }
    }
#endif
}