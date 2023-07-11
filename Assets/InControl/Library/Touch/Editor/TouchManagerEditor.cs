#if UNITY_EDITOR
using UnityEditor;
using UnityEngine;
using System.IO;


namespace InControl
{
	[CustomEditor( typeof(TouchManager) )]
	public class TouchManagerEditor : Editor
	{
		TouchManager touchManager;
		Texture headerTexture;
		
		
		void OnEnable()
		{
			touchManager = target as TouchManager;

			var path = AssetDatabase.GetAssetPath( MonoScript.FromScriptableObject( this ) );
			headerTexture = AssetDatabase.LoadAssetAtPath<Texture>( Path.GetDirectoryName( path ) + "/Images/TouchManagerHeader.png" );
		}
		
		
		public override void OnInspectorGUI()
		{
			GUILayout.Space( 5.0f );
			
			var headerRect = GUILayoutUtility.GetRect( 0.0f, -22.0f );
			headerRect.width = headerTexture.width;
			headerRect.height = headerTexture.height;
			GUILayout.Space( headerRect.height );

			DrawDefaultInspector();

			touchManager.controlsEnabled = EditorGUILayout.Toggle( "Controls Enabled", touchManager.controlsEnabled );

			GUI.DrawTexture( headerRect, headerTexture );

			GUILayout.Space( 5.0f );
			GUILayout.Label( "Add Controls", EditorStyles.boldLabel );

			if (GUILayout.Button( "Create Button Control" ))
			{
				TouchBuilder.CreateTouchButtonControl();
			}

			if (GUILayout.Button( "Create Stick Control" ))
			{
				TouchBuilder.CreateTouchStickControl();
			}

			if (GUILayout.Button( "Create Swipe Control" ))
			{
				TouchBuilder.CreateTouchSwipeControl();
			}

			if (GUILayout.Button( "Create Track Control" ))
			{
				TouchBuilder.CreateTouchTrackControl();
			}
		}
	}
}
#endif

