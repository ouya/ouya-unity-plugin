#if UNITY_EDITOR
using System.IO;
using UnityEditor;
using UnityEngine;


namespace InControl
{
	public class TouchControlEditor : Editor
	{
		Texture headerTexture;
		Rect headerTextureRect;

		
		protected void LoadHeaderImage( string fileName )
		{
			var path = AssetDatabase.GetAssetPath( MonoScript.FromScriptableObject( this ) );
			headerTexture = AssetDatabase.LoadAssetAtPath<Texture>( Path.GetDirectoryName( path ) + "/" + fileName );
		}


		protected void AddHeaderImageSpace()
		{
			GUILayout.Space( 5 );

			headerTextureRect = GUILayoutUtility.GetRect( 0.0f, -22.0f );
			headerTextureRect.width = headerTexture.width;
			headerTextureRect.height = headerTexture.height;

			GUILayout.Space( headerTextureRect.height );
		}


		protected void DrawHeaderImage()
		{
			GUI.DrawTexture( headerTextureRect, headerTexture );
		}


		public override void OnInspectorGUI()
		{			
			AddHeaderImageSpace();
			
			if (DrawDefaultInspector())
			{
				if (Application.isPlaying)
				{
					foreach (var target in targets)
					{
						(target as TouchControl).SendMessage( "ConfigureControl" );
					}
				}
			}
			
			DrawHeaderImage();
		}
	}
}
#endif