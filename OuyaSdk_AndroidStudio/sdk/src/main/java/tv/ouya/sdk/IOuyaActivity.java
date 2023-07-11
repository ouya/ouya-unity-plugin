/*
 * Copyright (C) 2012-2014 OUYA, Inc.
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

package tv.ouya.sdk;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;
import com.unity3d.player.UnityPlayer;
import java.util.*;
import tv.ouya.console.api.content.OuyaContent;
import tv.ouya.console.api.content.OuyaMod;

public class IOuyaActivity
{
	// save reference to the activity
	protected static Activity m_activity = null;
	public static Activity GetActivity()
	{
		return m_activity;
	}
	public static void SetActivity(Activity activity)
	{
		m_activity = activity;
	}
	
	// save reference to the main activity
	protected static MainActivity s_mainActivity = null;
	public static MainActivity GetMainActivity()
	{
		return s_mainActivity;
	}
	public static void SetMainActivity(MainActivity activity)
	{
		s_mainActivity = activity;
	}	

	// save reference to the bundle
	protected static Bundle m_savedInstanceState = null;
	public static Bundle GetSavedInstanceState()
	{
		return m_savedInstanceState;
	}
	public static void SetSavedInstanceState(Bundle savedInstanceState)
	{
		m_savedInstanceState = savedInstanceState;
	}

	// save reference to the UnityOuyaFacade
	protected static UnityOuyaFacade m_unityOuyaFacade = null;
	public static UnityOuyaFacade GetUnityOuyaFacade()
	{
		return m_unityOuyaFacade;
	}
	public static void SetUnityOuyaFacade(UnityOuyaFacade unityOuyaFacade)
	{
		m_unityOuyaFacade = unityOuyaFacade;
	}

	/*
	* The application key. This is used to decrypt encrypted receipt responses. This should be replaced with the
	* application key obtained from the OUYA developers website.
	*/
	protected static byte[] m_applicationKey = null;
	public static byte[] GetApplicationKey()
	{
		return m_applicationKey;
	}
	public static void SetApplicationKey(byte[] applicationKey)
	{
		m_applicationKey = applicationKey;
	}

	protected static OuyaContent m_ouyaContent = null;
	public static OuyaContent GetOuyaContent()
	{
		return m_ouyaContent;
	}
	public static void SetOuyaContent(OuyaContent ouyaContent)
	{
		m_ouyaContent = ouyaContent;
	}

	protected static List<OuyaMod> m_ouyaContentInstalledResults = null;
	public static List<OuyaMod> GetOuyaContentInstalledResults()
	{
		return m_ouyaContentInstalledResults;
	}
	public static void SetOuyaContentInstalledResults(List<OuyaMod> results)
	{
		m_ouyaContentInstalledResults = results;
	}

	protected static List<OuyaMod> m_ouyaContentPublishedResults = null;
	public static List<OuyaMod> GetOuyaContentPublishedResults()
	{
		return m_ouyaContentPublishedResults;
	}
	public static void SetOuyaContentPublishedResults(List<OuyaMod> results)
	{
		m_ouyaContentPublishedResults = results;
	}
}