using UnityEngine;


namespace InControl
{
	public class Touch
	{
		public int fingerId;

		public TouchPhase phase;
		public int tapCount;

		public Vector2 position;
		public Vector2 deltaPosition;
		public Vector2 lastPosition;

		public float deltaTime;
		public ulong updateTick;


		internal Touch( int fingerId )
		{
			this.fingerId = fingerId;
			phase = TouchPhase.Ended;
		}


		internal void SetWithTouchData( UnityEngine.Touch touch, ulong updateTick, float deltaTime )
		{
			phase = touch.phase;
			tapCount = touch.tapCount;

			if (phase == TouchPhase.Began)
			{
				deltaPosition =  Vector2.zero;
				lastPosition = touch.position;
				position = touch.position;
			}
			else
			{
				if (phase == TouchPhase.Stationary)
				{
					phase = TouchPhase.Moved;
				}

				deltaPosition = touch.position - lastPosition;
				lastPosition = position;
				position = touch.position;
			}

			this.deltaTime = deltaTime;
			this.updateTick = updateTick;
		}


		internal bool SetWithMouseData( ulong updateTick, float deltaTime )
		{
			var mousePosition = new Vector2( Mathf.Round( Input.mousePosition.x ), Mathf.Round( Input.mousePosition.y ) );
			
			if (Input.GetMouseButtonDown( 0 ))
			{
				phase = TouchPhase.Began;
				tapCount = 1;

				deltaPosition = Vector2.zero;
				lastPosition = mousePosition;
				position = mousePosition;

				this.deltaTime = deltaTime;
				this.updateTick = updateTick;

				return true;
			}

			if (Input.GetMouseButtonUp( 0 ))
			{
				phase = TouchPhase.Ended;	

				tapCount = 1;

				deltaPosition = mousePosition - lastPosition;
				lastPosition = position;
				position = mousePosition;

				this.deltaTime = deltaTime;
				this.updateTick = updateTick;

				return true;
			}

			if (Input.GetMouseButton( 0 ))
			{
				phase = TouchPhase.Moved;

				tapCount = 1;

				deltaPosition = mousePosition - lastPosition;
				lastPosition = position;
				position = mousePosition;

				this.deltaTime = deltaTime;
				this.updateTick = updateTick;

				return true;
			}

			return false;
		}
	}
}
