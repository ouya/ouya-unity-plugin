/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.razerzone.turretmouse;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;


public class HidJni
{
    private static String TAG = "HidJni";
    private static int DISCOVERY_RATE = 3000;

    private Thread mMainJNIThread;
    private Handler mMainHandler;

    private boolean mAllowDiscovery = false;


    /** Called when the activity is first created. */
    public void HidJni()
    {
    }

    public void discoverMouse() {
        if(mAllowDiscovery) {
            Log.e(TAG, "discoverMouse() has already been called!");
        }
        else {
            mAllowDiscovery = true;
            mMainJNIThread = new Thread(new Runnable() {
                public void run() {
                    Looper.prepare();
                    mMainHandler = new Handler();
                    scanForMouse();
                    Looper.loop();
                }
            });
            mMainJNIThread.start();
        }
    }

    public void stopDiscoverMouse() {
        if(!mAllowDiscovery)
            Log.e(TAG, "stopDiscoverMouse() called when not discovering mice!");
        else {
            mAllowDiscovery = false;
            //stopReadReportLoopJNI();
        }
    }

    public void stopMouse() {
        stopDiscoverMouse();
        stopReadReportLoopNative();
    }

    public void mouseDiscovered() {
        Log.v(TAG, "mouseDiscovered() called");
        //stopDiscoverMouse();
        if(TurretMouseService.getInstance() != null)
            TurretMouseService.getInstance().showToast("Connected to a compatible mouse", Toast.LENGTH_LONG);
            TurretMouseService.getInstance().stopScanForMouse();
        mMainHandler.post(new Runnable() {
            public void run() {
                readReportLoopNative();
            }
        });
    }

    public void reportReceived(byte[] reportBytes) {
        if(TurretMouseService.getInstance() != null)
            TurretMouseService.getInstance().parseRazerReport(reportBytes);
        //LOGD("\n\n");
        //TODO
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    public void mouseDisconnected() {
        Log.v(TAG, "mouseDisconnected() called");
        TurretMouseService.getInstance().startScanForMouse();
    }

    public void scanForMouse() {
        Log.v(TAG, "scanForMouse() called");
        if(mAllowDiscovery) {
            mMainHandler.post(new Runnable() {
                public void run() {
                    discoverMouseNative();
                }
            });
            mMainHandler.postDelayed(new Runnable() {
                public void run() {
                    scanForMouse();
                }
            }, DISCOVERY_RATE);
        }
    }

    /* A native method that is implemented by the
     * 'hid-jni' native library, which is packaged
     * with this application.
     */
    public native int discoverMouseNative();

    public native int readReportLoopNative();

    public native int stopReadReportLoopNative();
}
