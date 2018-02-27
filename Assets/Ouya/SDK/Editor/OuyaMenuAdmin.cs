/*
 * Copyright (C) 2012, 2013 OUYA, Inc.
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

using System;
using System.Collections.Generic;
using System.IO;
using UnityEditor;
using UnityEngine;
using Debug = UnityEngine.Debug;

public class OuyaMenuAdmin : MonoBehaviour
{
    private static Vector3 m_pos = Vector3.zero;
    private static Vector3 m_euler = Vector3.zero;

    [MenuItem("OUYA/Export Core Package", priority = 100)]
    public static void MenuPackageCore()
    {
        string[] paths =
            {
                "Assets/Ouya/SDK",
                "Assets/Plugins/Bitmap.cs",
                "Assets/Plugins/BitmapDrawable.cs",
                "Assets/Plugins/BitmapFactory.cs",
                "Assets/Plugins/ByteArrayOutputStream.cs",
                "Assets/Plugins/DebugInput.cs",
                "Assets/Plugins/Drawable.cs",
                "Assets/Plugins/InputStream.cs",
                "Assets/Plugins/JniHandleOwnership.cs",
                "Assets/Plugins/JSONArray.cs",
                "Assets/Plugins/JSONObject.cs",
                "Assets/Plugins/OutputStream.cs",
                "Assets/Plugins/OuyaContent.cs",
                "Assets/Plugins/OuyaController.cs",
                "Assets/Plugins/OuyaMod.cs",
                "Assets/Plugins/OuyaModScreenshot.cs",
                "Assets/Plugins/OuyaSDK.cs",
                "Assets/Plugins/OuyaUnityActivity.cs",
                "Assets/Plugins/OuyaUnityPlugin.cs",
                "Assets/Plugins/UnityPlayer.cs",
                "Assets/Plugins/Android/AndroidManifest.xml",
                "Assets/Plugins/Android/assets/key.der",
                "Assets/Plugins/Android/jni/Android.mk",
                "Assets/Plugins/Android/jni/Application.mk",
                "Assets/Plugins/Android/jni/jni.cpp",
                "Assets/Plugins/Android/libs/armeabi-v7a/lib-ouya-ndk.so",
                "Assets/Plugins/Android/libs/armeabi-v7a/lib-ouya-ndk.so.meta",
                "Assets/Plugins/Android/libs/ouya-sdk.jar",
                "Assets/Plugins/Android/libs/OuyaUnityPlugin.jar",
                "Assets/Plugins/Android/res/drawable/app_icon.png",
                "Assets/Plugins/Android/res/drawable/icon.png",
                "Assets/Plugins/Android/res/drawable-xhdpi/ouya_icon.png",
                "Assets/Plugins/Android/res/values/strings.xml",
            };
        AssetDatabase.ExportPackage(paths, "OuyaSDK-Core.unitypackage", ExportPackageOptions.IncludeDependencies | ExportPackageOptions.Recurse | ExportPackageOptions.Interactive);
        Debug.Log(string.Format("Export OuyaSDK-Core.unitypackage success in: {0}", Directory.GetCurrentDirectory()));
    }

    [MenuItem("OUYA/Export Examples Package", priority = 110)]
    public static void MenuPackageExamples()
    {
        string[] paths =
            {
                "Assets/Ouya/Examples",
            };
        AssetDatabase.ExportPackage(paths, "OuyaSDK-Examples.unitypackage", ExportPackageOptions.Recurse | ExportPackageOptions.Recurse | ExportPackageOptions.Interactive);
        Debug.Log(string.Format("Export OuyaSDK-Examples.unitypackage success in: {0}", Directory.GetCurrentDirectory()));
    }

    [MenuItem("OUYA/Export StarterKit Package", priority = 120)]
    public static void MenuPackageStarterKit()
    {
        string[] paths =
            {
                "Assets/Ouya/StarterKit",
            };
        AssetDatabase.ExportPackage(paths, "OuyaSDK-StarterKit.unitypackage", ExportPackageOptions.Recurse | ExportPackageOptions.Recurse | ExportPackageOptions.Interactive);
        Debug.Log(string.Format("Export OuyaSDK-StarterKit.unitypackage success in: {0}", Directory.GetCurrentDirectory()));
    }

    [MenuItem("OUYA/Unset Symbol (UNITY_EDITOR)", priority = 1000)]
    public static void MenuSymbolUnsetUnityEditor()
    {
        try
        {
            DirectoryInfo pathUnityProject = new DirectoryInfo(Directory.GetCurrentDirectory());
            foreach (FileInfo file in pathUnityProject.GetFiles())
            {
                if (!file.Extension.ToUpper().Equals(".CSPROJ"))
                {
                    continue;
                }
                //Debug.Log(string.Format("Examine: {0}", file.Name));
                string content;
                using (StreamReader sr = new StreamReader(file.FullName))
                {
                    content = sr.ReadToEnd();
                    content = content.Replace("UNITY_EDITOR;", string.Empty);
                }
                using (StreamWriter sw = new StreamWriter(file.FullName))
                {
                    sw.Write(content);
                    sw.Flush();
                }
                Debug.Log(string.Format("Updated: {0}", file.Name));
            }
        }
        catch (System.Exception)
        {

        }
    }

    [MenuItem("OUYA/Copy Object Transform", priority = 2010)]
    public static void MenuCopyObjectTransform()
    {
        if (Selection.activeGameObject)
        {
            m_pos = Selection.activeGameObject.transform.position;
            m_euler = Selection.activeGameObject.transform.rotation.eulerAngles;
        }
    }

    [MenuItem("OUYA/Copy Scene Transform", priority = 2020)]
    public static void MenuCopySceneTransform()
    {
        if (SceneView.currentDrawingSceneView &&
            SceneView.currentDrawingSceneView.camera &&
            SceneView.currentDrawingSceneView.camera.transform)
        {
            m_pos = SceneView.currentDrawingSceneView.camera.transform.position;
            m_euler = SceneView.currentDrawingSceneView.camera.transform.rotation.eulerAngles;
        }
    }

    [MenuItem("OUYA/Paste Stored Transform", priority = 2030)]
    public static void MenuSetTransform()
    {
        if (Selection.activeGameObject)
        {
            Selection.activeGameObject.transform.position = m_pos;
            Selection.activeGameObject.transform.rotation = Quaternion.Euler(m_euler);
        }
    }

#pragma warning disable CS0414 //
    private static string m_pathUnityProject = string.Empty;
    private static string m_pathUnityEditor = string.Empty;
    private static string m_pathUnityJar = string.Empty;
    private static string m_pathJDK = string.Empty;
    private static string m_pathToolsJar = string.Empty;
    private static string m_pathJar = string.Empty;
    private static string m_pathJavaC = string.Empty;
    private static string m_pathJavaP = string.Empty;
    private static string m_pathSDK = string.Empty;
#pragma warning restore CS0414 //

    private static void UpdatePaths()
    {
        m_pathUnityProject = new DirectoryInfo(Directory.GetCurrentDirectory()).FullName;
        switch (Application.platform)
        {
            case RuntimePlatform.OSXEditor:
                m_pathUnityEditor = EditorApplication.applicationPath;
                OuyaPanel.FindFile(new DirectoryInfo(string.Format("{0}", EditorApplication.applicationPath)), OuyaPanel.FILE_UNITY_JAR, ref m_pathUnityJar);
                m_pathUnityJar = m_pathUnityJar.Replace(@"\", "/");
                break;
            case RuntimePlatform.WindowsEditor:
                m_pathUnityEditor = new FileInfo(EditorApplication.applicationPath).Directory.FullName;
                OuyaPanel.FindFile(new DirectoryInfo(string.Format("{0}/{1}", m_pathUnityEditor, OuyaPanel.PATH_UNITY_JAR_WIN)), OuyaPanel.FILE_UNITY_JAR, ref m_pathUnityJar);
                break;
        }
        m_pathSDK = EditorPrefs.GetString(OuyaPanel.KEY_PATH_ANDROID_SDK);
        m_pathJDK = EditorPrefs.GetString(OuyaPanel.KEY_PATH_JAVA_JDK);
        switch (Application.platform)
        {
            case RuntimePlatform.OSXEditor:
                m_pathToolsJar = string.Format("{0}/Contents/Classes/classes.jar", m_pathJDK);
                m_pathJar = string.Format("{0}/Contents/Commands/{1}", m_pathJDK, OuyaPanel.FILE_JAR_MAC);
                m_pathJavaC = string.Format("{0}/Contents/Commands/{1}", m_pathJDK, OuyaPanel.FILE_JAVAC_MAC);
                m_pathJavaP = string.Format("{0}/Contents/Commands/{1}", m_pathJDK, OuyaPanel.FILE_JAVAP_MAC);
                break;
            case RuntimePlatform.WindowsEditor:
                m_pathToolsJar = string.Format("{0}/lib/tools.jar", m_pathJDK);
                m_pathJar = string.Format("{0}/{1}/{2}", m_pathJDK, OuyaPanel.REL_JAVA_PLATFORM_TOOLS, OuyaPanel.FILE_JAR_WIN);
                m_pathJavaC = string.Format("{0}/{1}/{2}", m_pathJDK, OuyaPanel.REL_JAVA_PLATFORM_TOOLS, OuyaPanel.FILE_JAVAC_WIN);
                m_pathJavaP = string.Format("{0}/{1}/{2}", m_pathJDK, OuyaPanel.REL_JAVA_PLATFORM_TOOLS, OuyaPanel.FILE_JAVAP_WIN);
                break;
        }
    }

    private static string GetPathAndroidJar()
    {
        return string.Format("{0}/platforms/android-{1}/android.jar", m_pathSDK, (int)PlayerSettings.Android.minSdkVersion);
    }

    public static void GetAssets(string extension, Dictionary<string, string> files, DirectoryInfo directory)
    {
        if (null == directory)
        {
            return;
        }
        foreach (FileInfo file in directory.GetFiles(extension))
        {
            if (string.IsNullOrEmpty(file.FullName) ||
                files.ContainsKey(file.FullName.ToLower()))
            {
                continue;
            }
            files.Add(file.FullName.ToLower(), file.FullName);
        }
        foreach (DirectoryInfo subDir in directory.GetDirectories())
        {
            if (null == subDir)
            {
                continue;
            }
            if (subDir.Name.ToUpper().Equals(".SVN"))
            {
                continue;
            }
            //Debug.Log(string.Format("Directory: {0}", subDir));
            GetAssets(extension, files, subDir);
        }
    }
}
