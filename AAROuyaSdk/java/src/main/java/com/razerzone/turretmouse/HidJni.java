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
        stopReadReportLoopJNI();
    }

    public void mouseDiscovered() {
        Log.v(TAG, "mouseDiscovered() called");
        //stopDiscoverMouse();
        if(TurretMouseService.getInstance() != null)
            TurretMouseService.getInstance().showToast("Connected to a compatible mouse", Toast.LENGTH_LONG);
            TurretMouseService.getInstance().stopScanForMouse();
        mMainHandler.post(new Runnable() {
            public void run() {
                readReportLoopJNI();
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
                    discoverMouseJNI();
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
    public native int discoverMouseJNI();

    public native int readReportLoopJNI();

    public native int stopReadReportLoopJNI();

    /* This is another native method declaration that is *not*
     * implemented by 'hid-jni'. This is simply to show that
     * you can declare as many native methods in your Java code
     * as you want, their implementation is searched in the
     * currently loaded native libraries only the first time
     * you call them.
     *
     * Trying to call this function will result in a
     * java.lang.UnsatisfiedLinkError exception !
     */
    //public native String  unimplementedStringFromJNI();

    /* this is used to load the 'hid-jni' library on application
     * startup. The library has already been unpacked into
     * /data/data/com.example.hidjni/lib/libhid-jni.so at
     * installation time by the package manager.
     */
    static {
        System.loadLibrary("hid-jni");
    }
}
