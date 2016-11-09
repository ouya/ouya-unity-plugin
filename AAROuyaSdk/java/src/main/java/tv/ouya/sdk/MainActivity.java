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
import android.content.*;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.razerzone.turretmouse.TurretMouseService;
import com.unity3d.player.UnityPlayer;

import java.io.InputStream;
import java.io.IOException;

import tv.ouya.console.api.OuyaController;
import tv.ouya.console.api.OuyaIntent;

public class MainActivity extends Activity
{
	private static final String TAG = "MainActivity";

	private static final String PLUGIN_VERSION = "2.1.0.6";

    private static final int TURRET_MOUSE_BUTTON_INDEX = 0;

    private static final int TURRET_MOUSE_Y_INDEX = 5;

    private static final int TURRET_MOUSE_Z_INDEX = 3;

	private static final boolean sEnableLogging = false;

	protected UnityPlayer mUnityPlayer;		// don't change the name of this variable; referenced from native code

    private OuyaInputView mInputView = null;

    private TurretMouseService mMouseService = null;

    boolean mMouseServiceBound = false;

    public native void setTurretMouseInfoNative(int index, int value);

    private static int sDisplayWidth = 1920;

    private static int sDisplayHeight = 1080;

    TurretMouseService.mouseReceiver mMouseReceiver = new TurretMouseService.mouseReceiver() {
        @Override
        public void onMouseAction(final int[] mouseInfo) {
            if (sEnableLogging) {
                Log.v(TAG, "Calling mouseReceiver: " + mouseInfo.length);
                if (0 != ( TurretMouseService.BUTTON_LEFT & mouseInfo[0] ))
                    Log.v(TAG, "BUTTON_LEFT" + "\n");
                if (0 != ( TurretMouseService.BUTTON_RIGHT & mouseInfo[0] ))
                    Log.v(TAG, "BUTTON_RIGHT" + "\n");
                if (0 != ( TurretMouseService.BUTTON_MIDDLE & mouseInfo[0] ))
                    Log.v(TAG, "BUTTON_MIDDLE" + "\n");
                if (0 != ( TurretMouseService.BUTTON_BACK & mouseInfo[0] ))
                    Log.v(TAG, "BUTTON_BACK" + "\n");
                if (0 != ( TurretMouseService.BUTTON_FORWARD & mouseInfo[0] ))
                    Log.v(TAG, "BUTTON_FORWARD" + "\n");
                if (0 != ( TurretMouseService.BUTTON_6 & mouseInfo[0] ))
                    Log.v(TAG, "BUTTON_6" + "\n");
                if (0 != ( TurretMouseService.BUTTON_7 & mouseInfo[0] ))
                    Log.v(TAG, "BUTTON_7" + "\n");
                if (0 != ( TurretMouseService.BUTTON_8 & mouseInfo[0] ))
                    Log.v(TAG, "BUTTON_8" + "\n");
            }

            int y = mouseInfo[TURRET_MOUSE_Y_INDEX];
            int invertY = sDisplayHeight - y;
            int z = mouseInfo[TURRET_MOUSE_Z_INDEX];

            long downTime = 0;
            long eventTime = 0;

            int action = 0;

            int buttons = mouseInfo[TURRET_MOUSE_BUTTON_INDEX];
            if (0 != (TurretMouseService.BUTTON_LEFT & buttons)) {
                action = MotionEvent.ACTION_DOWN;
            } else {
                action = MotionEvent.ACTION_UP;
            }

            int pointerCount = 1;

            MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[1];
            MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[1];

            long pointerIndex = 0;

            MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
            properties.id = (int)pointerIndex;
            properties.toolType = 0;
            pointerProperties[0] = properties;

            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.orientation = 0;
            coords.pressure = 0;
            coords.size = 0;
            coords.toolMajor = 0;
            coords.toolMinor = 0;
            coords.touchMajor = 0;
            coords.touchMinor = 0;
            coords.x = mouseInfo[4];
            coords.y = y;
            coords.setAxisValue(MotionEvent.AXIS_X, mouseInfo[4]);
            coords.setAxisValue(MotionEvent.AXIS_Y, y);
            coords.setAxisValue(MotionEvent.AXIS_VSCROLL, z);
            pointerCoords[0] = coords;

            int metaState = 0;
            int buttonState = 0;
            float xPrecision = 0;
            float yPrecision = 0;
            int deviceId = 0;
            int edgeFlags = 0;
            int source = InputDevice.SOURCE_MOUSE;
            int flags = 0;
            MotionEvent motionEvent = MotionEvent.obtain(downTime, eventTime, action, pointerCount, pointerProperties,
                    pointerCoords, metaState, buttonState, xPrecision, yPrecision, deviceId,
                    edgeFlags, source, flags);

            // inject the mouse event into Unity PLayer
            mUnityPlayer.injectEvent(motionEvent);

            // populate the Turret Mouse API
            for (int i = 0; i < TURRET_MOUSE_Y_INDEX; i++) {
                setTurretMouseInfoNative(i, mouseInfo[i]);
            }
            setTurretMouseInfoNative(TURRET_MOUSE_Y_INDEX, invertY);

            if (sEnableLogging) {
                for (int i = 0; i < mouseInfo.length; i++) {
                    switch (i) {
                        case 1:
                            Log.v(TAG, "Mouse X: " + Integer.toString(mouseInfo[i]));
                            break;
                        case 2:
                            Log.v(TAG, "Mouse Y: " + Integer.toString(mouseInfo[i]));
                            break;
                        case 3:
                            Log.v(TAG, "Mouse Wheel: " + Integer.toString(mouseInfo[i]));
                            break;
                        case 4:
                            Log.v(TAG, "Mouse X Screen Position: " + Integer.toString(mouseInfo[i]));
                            break;
                        case 5:
                            Log.v(TAG, "Mouse Y Screen Position: " + Integer.toString(mouseInfo[i]));
                            break;
                    }
                }
            }
        }
    };

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mMouseConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to BLEmouseDriver, cast the IBinder and get LocalService instance
            TurretMouseService.LocalBinder binder = (TurretMouseService.LocalBinder) service;
            mMouseService = binder.getService();

            mMouseService.setMouseReceiver(mMouseReceiver);
            mMouseService.setDisplayResolution(sDisplayWidth, sDisplayHeight);
            mMouseService.setSensitivity(1, 1);
            mMouseService.setCursorPosition(0, 0);
            mMouseService.setPolling(false);

            mMouseServiceBound = true;
            //Log.v("ON MOUSE ACTION BODY", "mMouseService.startScanForMouse()");
            mMouseService.startScanForMouse();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mMouseServiceBound = false;
            mMouseService = null;
        }
    };

	// Setup activity layout
	@Override protected void onCreate (Bundle savedInstanceState)
	{
		Log.i(TAG, "OuyaUnityPlugin: VERSION="+PLUGIN_VERSION);

		//make activity accessible to Unity
		IOuyaActivity.SetActivity(this);
		IOuyaActivity.SetMainActivity(this);

		//make bundle accessible to Unity
		if (null != savedInstanceState)
		{
			IOuyaActivity.SetSavedInstanceState(savedInstanceState);
		}

		// load the signing key from assets
		try {
			Context context = getApplicationContext();
			AssetManager assetManager = context.getAssets();
			InputStream inputStream = assetManager.open("key.der", AssetManager.ACCESS_BUFFER);
			byte[] applicationKey = new byte[inputStream.available()];
			inputStream.read(applicationKey);
			inputStream.close();
			IOuyaActivity.SetApplicationKey(applicationKey);
		} catch (IOException e) {
			e.printStackTrace();
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		getWindow().takeSurface(null);
		setTheme(android.R.style.Theme_NoTitleBar_Fullscreen);
		getWindow().setFormat(PixelFormat.RGB_565);

		mUnityPlayer = new UnityPlayer(this);
		if (mUnityPlayer.getSettings ().getBoolean ("hide_status_bar", true))
			getWindow ().setFlags (WindowManager.LayoutParams.FLAG_FULLSCREEN,
			                       WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(mUnityPlayer);

        mInputView = new OuyaInputView(this);
        
		if (sEnableLogging) {
			Log.d(TAG, "disable screensaver");
		}
        mInputView.setKeepScreenOn(true);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	// Quit Unity
	@Override protected void onDestroy ()
	{
		mUnityPlayer.quit();
		super.onDestroy();
		if (null != mInputView) {
			mInputView.shutdown();
		}
	}

	/**
     * Broadcast listener to handle menu appearing
     */

    private BroadcastReceiver mMenuAppearingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
			if (sEnableLogging) {
				Log.i(TAG, "BroadcastReceiver intent=" + intent.getAction());
			}
			if(intent.getAction().equals(OuyaIntent.ACTION_MENUAPPEARING)) {
				if (sEnableLogging) {
					Log.i(TAG, "OuyaGameObject->onMenuAppearing");
				}
				UnityPlayer.UnitySendMessage("OuyaGameObject", "onMenuAppearing", "");
			}
        }
    };

    @Override
    public void onStart() {
        super.onStart();

		IntentFilter menuAppearingFilter = new IntentFilter();
		menuAppearingFilter.addAction(OuyaIntent.ACTION_MENUAPPEARING);
		registerReceiver(mMenuAppearingReceiver, menuAppearingFilter);
    }

    @Override
    public void onStop() {
		unregisterReceiver(mMenuAppearingReceiver);
        super.onStop();
    }

	// Pause Unity
	@Override protected void onPause()
	{
		super.onPause();
		if (sEnableLogging) {
			Log.d(TAG, "OuyaGameObject->onPause");
		}
		UnityPlayer.UnitySendMessage("OuyaGameObject", "onPause", "");
		mUnityPlayer.pause();
		if (null != mInputView) {
			mInputView.requestFocus();
		}

        if (mMouseServiceBound) {
            unbindService(mMouseConnection);
            mMouseServiceBound = false;
        }
	}

	// Resume Unity
	@Override protected void onResume()
	{
		super.onResume();
		mUnityPlayer.resume();
		if (sEnableLogging) {
			Log.d(TAG, "OuyaGameObject->onResume");
		}
		UnityPlayer.UnitySendMessage("OuyaGameObject", "onResume", "");
		if (null != mInputView) {
			mInputView.requestFocus();
		}

        Intent intent = new Intent(this, TurretMouseService.class);
        bindService(intent, mMouseConnection, Context.BIND_AUTO_CREATE);
	}

	// This ensures the layout will be correct.
	@Override public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		mUnityPlayer.configurationChanged(newConfig);
	}

	// Notify Unity of the focus change.
	@Override public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		mUnityPlayer.windowFocusChanged(hasFocus);
		UnityPlayer.UnitySendMessage("OuyaGameObject", "onResume", "");
		if (null != mInputView) {
			mInputView.requestFocus();
		}
	}

	@Override
    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
    	if (sEnableLogging) {
			Log.i(TAG, "dispatchGenericMotionEvent");
		}
		if (null == mInputView) {
			return super.dispatchGenericMotionEvent(motionEvent);
		} else {
			mInputView.dispatchGenericMotionEvent(motionEvent);
		}
		return false;
    }

	private void raiseVolume() {
		AudioManager audioMgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		int stream = AudioManager.STREAM_SYSTEM;
		int maxVolume = audioMgr.getStreamMaxVolume(stream);
		int volume = audioMgr.getStreamVolume(stream);
		volume = Math.min(volume + 1, maxVolume);
		audioMgr.setStreamVolume(stream, volume, 0);
	}

	private void lowerVolume() {
		AudioManager audioMgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		int stream = AudioManager.STREAM_SYSTEM;
		int maxVolume = audioMgr.getStreamMaxVolume(stream);
		int volume = audioMgr.getStreamVolume(stream);
		volume = Math.max(volume - 1, 0);
		audioMgr.setStreamVolume(stream, volume, 0);
	}
	
	@Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
    	if (sEnableLogging) {
			Log.i(TAG, "dispatchKeyEvent keyCode="+keyEvent.getKeyCode());
		}
		if (null == mInputView) {
			return super.dispatchKeyEvent(keyEvent);
		}
		InputDevice device = keyEvent.getDevice();
		if (null != device) {
			String name = device.getName();
			if (null != name &&
				name.equals("aml_keypad")) {
				switch (keyEvent.getKeyCode()) {
				case 24:
					if (sEnableLogging) {
						Log.i(TAG, "Volume Up detected.");
					}
					//raiseVolume();
					//return true; //the volume was handled
					return false; //show the xiaomi volume overlay
				case 25:
					if (sEnableLogging) {
						Log.i(TAG, "Volume Down detected.");
					}
					//lowerVolume();
					//return true; //the volume was handled
					return false; //show the xiaomi volume overlay
				case 66:
					if (sEnableLogging) {
						Log.i(TAG, "Remote button detected.");
					}
					if (null != mInputView) {
						if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
							mInputView.onKeyDown(OuyaController.BUTTON_O, keyEvent);
						} else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
							mInputView.onKeyUp(OuyaController.BUTTON_O, keyEvent);
						}
					}
					return false;
				case 4:
					if (sEnableLogging) {
						Log.i(TAG, "Remote back button detected.");
					}
					if (null != mInputView) {
						if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
							mInputView.onKeyDown(OuyaController.BUTTON_A, keyEvent);
						} else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
							mInputView.onKeyUp(OuyaController.BUTTON_A, keyEvent);
						}
					}
					return true;
				}
			}
		}
    	if (null != mInputView) {
			mInputView.dispatchKeyEvent(keyEvent);
		}
		return true;
    }

	@Override
	public boolean onGenericMotionEvent(MotionEvent motionEvent) {
    	if (sEnableLogging) {
			Log.i(TAG, "onGenericMotionEvent");
		}
		if (null == mInputView) {
			return super.onGenericMotionEvent(motionEvent);
		} else {
			mInputView.requestFocus();
		}
		return true;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
    	if (sEnableLogging) {
			Log.i(TAG, "onKeyUp");
		}
    	if (null == mInputView) {
			return super.onKeyUp(keyCode, keyEvent);
		} else {
			mInputView.requestFocus();
		}
		return true;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
    	if (sEnableLogging) {
			Log.i(TAG, "onKeyDown");
		}
		if (null == mInputView) {
			return super.onKeyDown(keyCode, keyEvent);
		} else {
			mInputView.requestFocus();
		}
		return true;
	}

    @Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (sEnableLogging) {
			Log.i(TAG, "onActivityResult");
		}
		super.onActivityResult(requestCode, resultCode, data);
		UnityOuyaFacade unityOuyaFacade = IOuyaActivity.GetUnityOuyaFacade();
		if (null != unityOuyaFacade)
		{
			// Forward this result to the facade, in case it is waiting for any activity results
			if (unityOuyaFacade.processActivityResult(requestCode, resultCode, data)) {
				return;
			}
		} else {
			Log.e(TAG, "UnityOuyaFacade is null");
		}
	}
	
	public void useDefaultInput() {
		Runnable runnable = new Runnable()
		{
			public void run()
			{
				if (null == mInputView) {
					Log.i(TAG, "useDefaultInput: Focus the Unity Player");
					giveUnityFocus();
					return;
				}
				mInputView.shutdown();
				FrameLayout content = (FrameLayout)findViewById(android.R.id.content);
				if (null != content) {
					content.removeView(mInputView);
				} else {
					Log.e(TAG, "Content view is missing");
				}
				mInputView = null;
				Log.i(TAG, "useDefaultInput: Request focus for the Unity Player");
				giveUnityFocus();
			}
		};
		runOnUiThread(runnable);
	}
	
	private void giveUnityFocus() {
		takeKeyEvents(false);
		mUnityPlayer.setFocusable(true);
		mUnityPlayer.requestFocus();
	}
}
