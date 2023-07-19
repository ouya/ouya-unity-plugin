package com.razerzone.turretmouse;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Establishes a connection and retrieves information from the Turret mouse.
 * <p>
 * See README.txt for examples and instructions on how to integrate this library with your project.
 */

public class TurretMouseService extends Service {
    private final static String TAG = TurretMouseService.class.getSimpleName();
    private final static boolean ENABLE_TOASTS = false;
    private Context mTurretMouseServiceContext = this;
    private static TurretMouseService sTurretMouseServiceInstance;
    private HidJni mHidJni = new HidJni();

    private static IntentFilter makeGattUpdateIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ACTION_BOND_STATE_CHANGED);
        return intentFilter;
    }

    public static TurretMouseService getInstance() {
        return sTurretMouseServiceInstance;
    }

    // ------------------------------------------------------
    // -----------------  PUBLIC VARIABLES  -----------------
    // ------------------------------------------------------

    /** Bitmask which indicates that the left mouse button is pressed.
     * <p>
     * @see #pollMouse()
     * @see mouseReceiver#onMouseAction(int[]) */
    public final static int BUTTON_LEFT = 0x01 << 0;
    /** Bitmask which indicates that the right mouse button is pressed.
     * <p>
     * @see #pollMouse()
     * @see mouseReceiver#onMouseAction(int[]) */
    public final static int BUTTON_RIGHT = 0x01 << 1;
    /** Bitmask which indicates that the middle mouse button is pressed.
     * <p>
     * @see #pollMouse()
     * @see mouseReceiver#onMouseAction(int[]) */
    public final static int BUTTON_MIDDLE = 0x01 << 2;
    /** Bitmask which indicates that the back mouse button is pressed.
     * <p>
     * @see #pollMouse()
     * @see mouseReceiver#onMouseAction(int[]) */
    public final static int BUTTON_BACK = 0x01 << 3;
    /** Bitmask which indicates that the forward mouse button is pressed.
     * <p>
     * @see #pollMouse()
     * @see mouseReceiver#onMouseAction(int[]) */
    public final static int BUTTON_FORWARD = 0x01 << 4;
    /** Bitmask which indicates that button 6 on the mouse is pressed.
     * <p>
     * @see #pollMouse()
     * @see mouseReceiver#onMouseAction(int[]) */
    public final static int BUTTON_6 = 0x01 << 5;
    /** Bitmask which indicates that button 7 on the mouse is pressed.
     * <p>
     * @see #pollMouse()
     * @see mouseReceiver#onMouseAction(int[]) */
    public final static int BUTTON_7 = 0x01 << 6;
    /** Bitmask which indicates that button 8 on the mouse is pressed.
     * <p>
     * @see #pollMouse()
     * @see mouseReceiver#onMouseAction(int[]) */
    public final static int BUTTON_8 = 0x01 << 7;

    private final static String CLIENT_CHARACTERISTIC = "00002902-0000-1000-8000-00805f9b34fb";

    private final static String ACTION_GATT_CONNECTED =
            "com.razer.ble_mousetest.app.ACTION_GATT_CONNECTED";
    private final static String ACTION_GATT_DISCONNECTED =
            "com.razer.ble_mousetest.app.ACTION_GATT_DISCONNECTED";
    private final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.razer.ble_mousetest.app.ACTION_GATT_SERVICES_DISCOVERED";
    private final static String ACTION_DATA_AVAILABLE =
            "com.razer.ble_mousetest.app.ACTION_DATA_AVAILABLE";
    private final static String EXTRA_DATA =
            "com.razer.ble_mousetest.app.EXTRA_DATA";
    private final static String ACTION_BOND_STATE_CHANGED =
            "android.bluetooth.device.action.BOND_STATE_CHANGED";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private int mConnectionState = STATE_DISCONNECTED;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 180000;

    // -----------------------------------------------------------
    // -----------------  VARIABLE DECLARATIONS  -----------------
    // -----------------------------------------------------------

    private boolean mConnected = false;
    private boolean mCallbackEnabled = true;

    private String mDeviceName = "";
    private int mDeviceProductId = 0;
    private int mDeviceVendorId = 0;
    private String mPnPID = "";
    private int mCounter133 = 0;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt = null;
    private BluetoothLeScanner mBluetoothScanner;
    private BluetoothGattCallback mGattCallback;
    private List<ScanFilter> mScanFilters;
    private ScanSettings mScanSettings;

    private List<BluetoothGattService> mReportServices = new ArrayList<BluetoothGattService>();

    private boolean mScanning;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Thread mProcessingThread;
    private Handler mProcessingHandler;
    private BluetoothDevice mDevice;
    private BluetoothDevice mConnectedDevice = null;

    private boolean mJustRead = false;
    private boolean mBonded = false;
    private boolean mPairingLock = false;
    private boolean mRebooting = false;

    private int mMouseClickInfo = 0;
    private int mMouseWheelInfo = 0;
    private int mMouseXDiff = 0;
    private int mouseYDiff = 0;
    private int mDisplayResolutionX = 0;
    private int mDisplayResolutionY = 0;
    private double mMousePosX = 0;
    private double mMousePosY = 0;
    private double mSensitivityX = 1;
    private double mSensitivityY = 1;

    private mouseReceiver mMouseReceiver;
    private final IBinder mBinder = new LocalBinder();

    // ----------------------------------------------------
    // -----------------  INITIALIZATION  -----------------
    // ----------------------------------------------------

    /** Called when the TurretMouseService is first created.
     *  <p>
     *  Retrieves and initializes the Bluetooth Low Energy system service.
     *
     */
    @Override
    public void onCreate() {
        if(sTurretMouseServiceInstance == null)
            sTurretMouseServiceInstance = this;
        initializeBLE();

        mProcessingThread = new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                mProcessingHandler = new Handler();
                Looper.loop();
            }
        });
        mProcessingThread.start();
    }

    private boolean initializeBLE() {
        boolean rvalue = false;

        //enable bluetooth
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!defaultAdapter.isEnabled()) {
            defaultAdapter.enable();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (null == mBluetoothManager) {
                mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                if (null != mBluetoothManager) {
                    mBluetoothAdapter = mBluetoothManager.getAdapter();
                    if (null != mBluetoothAdapter) {
                        rvalue = true;
                    }
                }
            }
            mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        newGattCallback();

        /*if (false == rvalue) {
            Log.e(TAG, "Could not access bluetooth adapter.");
        }*/

        return rvalue;
    }

    @Override
    public void onDestroy() {
        stopScanForMouse();
        sTurretMouseServiceInstance = null;
        super.onDestroy();
    }

    // ------------------------------------------------------
    // -----------------  "SETTER" METHODS  -----------------
    // ------------------------------------------------------

    /** Use to indicate whether you are polling or handling callbacks to retrieve mouse updates.
     * <p>
     * TurretMouseService allows you to gather mouse output data either
     * via polling or by handling the onMouseAction callback method.
     * Use the following function to indicate which of these two modes
     * you wish to use.
     * If setPolling(false) is called, you must handle the
     * {@link mouseReceiver#onMouseAction(int[])} callback.
     * <p>
     * Default value: true.
     *
     * @param polling boolean */
    public void setPolling (boolean polling) {
        mCallbackEnabled = !polling;
    }

    /**Indicate what resolution you would like the service to compute a
     * mouse position over.
     * <p>
     * Default value: [0,0]. */
    public void setDisplayResolution(int displayResX, int displayResY) {
        mMousePosX *= ((double) displayResX) / ((double) mDisplayResolutionX);
        mMousePosY *= ((double) displayResY) / ((double) mDisplayResolutionY);
        mDisplayResolutionX = displayResX;
        mDisplayResolutionY = displayResY;
    }

    /** Set the mouse cursor to be at a particular position.
     * <p>
     * Must be within the bounds set by
     * {@link #setDisplayResolution(int, int)}.
     * <p>
     * Default value: [0,0]. */
    public void setCursorPosition(int cursorX, int cursorY) {
        mMousePosX = cursorX;
        mMousePosY = cursorY;
    }

    /** Set the sensitivity of the mouse.
     * <p>
     * This should only be used if the sensitivity of the X-axis
     * is equal to the sensitivity of the Y-axis.
     * <p>
     * Default value: 1. */
    public void setSensitivity(double sensitivity) {
        mSensitivityX = sensitivity;
        mSensitivityY = sensitivity;
    }

    /** Set the sensitivity of the mouse.
     * <p>
     * This should only be used if the sensitivity of the X-axis
     * does NOT equal the sensitivity of the Y-axis.
     * <p>
     * Default value: [1,1].*/
    public void setSensitivity(double sensX, double sensY) {
        mSensitivityX = sensX;
        mSensitivityY = sensY;
    }

    /** Indicates to the TurretMouseService the receiver
     * which will handle the {@link mouseReceiver#onMouseAction(int[])} callback.
     * <p>
     * DO NOT USE THIS IF YOU ARE USING THE POLLING
     * METHOD OF RETRIEVING DATA! */
    public void setMouseReceiver(mouseReceiver receiver) {
        Log.d("SET MOUSE RECEIVER","");
        mMouseReceiver = receiver;
    }
    public mouseReceiver getMouseReceiver() {
        return mMouseReceiver;
    }

    // ------------------------------------------------------
    // -----------------  "GETTER" METHODS  -----------------
    // ------------------------------------------------------

    /** Returns output received from the Bluetooth Low Energy mouse. This function should be called
     * on a regular basis, otherwise the mouse movements in your application will be jerky.
     * <p>
     * DO NOT USE THIS IF YOU ARE RETRIEVING MOUSE
     * UPDATES VIA THE {@link mouseReceiver#onMouseAction(int[])} CALLBACK!
     * <p>
     * To process mMouseClickInfo information, perform a bitwise AND with
     * any of the following constants:
     * {@link #BUTTON_LEFT}, {@link #BUTTON_RIGHT}, {@link #BUTTON_MIDDLE}, {@link #BUTTON_BACK},
     * {@link #BUTTON_FORWARD}, {@link #BUTTON_6}, {@link #BUTTON_7}, {@link #BUTTON_8}.
     * <p>
     * mMouseXDiff and mouseYDiff represent the translational distance along the X-axis
     * and Y-axis respectively that the mouse has moved since it was last polled.
     * <p>
     * mMouseWheelInfo represents the amount the user has moved the mouseWheel since the mouse
     * was last polled.  It will be an integer between -127 and +127.
     * <p>
     * mMousePosX and mMousePosY indicate the position of the mouse cursor on the
     * display.  If {@link #setDisplayResolution(int,int)} has not been called, then
     * mMousePosX=mMousePosY=0.
     * @return {mMouseClickInfo, mMouseXDiff, mouseYDiff, mMouseWheelInfo, mMousePosX, mMousePosY}*/
    public int[] pollMouse() {
        if (!mCallbackEnabled) {
            int[] mouseInfo = {mMouseClickInfo,
                    mMouseXDiff, mouseYDiff, mMouseWheelInfo,
                    (int) mMousePosX, (int) mMousePosY};
            mMouseXDiff = 0;
            mouseYDiff = 0;
            mMouseWheelInfo = 0;
            return mouseInfo;
        } else {
            throw new Error("Polling BLE mouse when polling is disabled!");
        }
    }

    /** Retrieve the resolution of the display set by {@link #setDisplayResolution(int, int)}.
     * @return {mDisplayResolutionX, mDisplayResolutionY}
     */
    public int[] getDisplayResolution() {
        int[] displayRes = {mDisplayResolutionX, mDisplayResolutionY};
        return displayRes;
    }

    /** Returns the service's current mouse sensitivity setting.  If this is
     * called when mSensitivityX != mSensitivityY, then this returns
     * the value of mSensitivityX.
     * @return sensitivity */
    public double getSensitivity() {
        // When this is used, mSensitivityX should equal mSensitivityY
        return mSensitivityX;
    }

    /** Returns the service's current X-axis mouse sensitivity setting.
     * @return mSensitivityX */
    public double getmSensitivityX() {
        return mSensitivityX;
    }

    /** Returns the service's current Y-axis mouse sensitivity setting.
     * @return mSensitivityY */
    public double getmSensitivityY() {
        return mSensitivityY;
    }

    /** Indicates whether the service is currently mConnected to a mouse.
     * If it is not, you would call {@link #scanLeDevice} with "true" to begin device
     * discovery.
     * @return mConnected */
    public boolean ismConnected() {
        return mConnected;
    }

    /** Returns the name of the device.
     * @return mouseName */
    public String getName() {
        if (!mDeviceName.equals(""))
            return mDeviceName;
        else
            return "Not mConnected to a BLE mouse!";
    }

    /**Returns the product ID of the mConnected mouse. CURRENTLY ALWAYS
     * RETURNS 0.
     * @return mDeviceProductId
     */
    public int getProductId() {
        return mDeviceProductId;
    }

    /**Returns the vendor ID of the mConnected mouse. CURRENTLY ALWAYS
    * RETURNS 0.
    * @return mDeviceVendorId */
    public int getVendorId() {
        return mDeviceVendorId;
    }

    /**Returns the MAC address of the mConnected mouse.
     * IF NOT CONNECTED, RETURNS "No mConnected device!"
     * @return macAddress */
    public String getAddress() {
        if(mConnectedDevice != null)
            return mConnectedDevice.getAddress();
        else
            return "No mConnected device!";
    }

    public String toString() {
        return "Application layer input device: " + mDeviceName;
    }

    // -----------------------------------------------------------------------------
    // -----------------  MOUSE STATE CHANGE CALLBACK DECLARATION  -----------------
    // -----------------------------------------------------------------------------

    /** Interface that must be used to receive mouse information via callback.
     */
    public interface mouseReceiver {
        /** Callback that must be handled to receive mouse information via callback.  DO NOT
         * IMPLEMENT IF YOU ARE RETRIEVING MOUSE OUTPUT VIA POLLING!
         * <p>
         * To process mMouseClickInfo information, see documentation for the constants
         * at the beginning of this file.
         * <p>
         * mMouseXDiff and mouseYDiff represent the translational distance along the X-axis
         * and Y-axis respectively that the mouse has moved since it was last polled.
         * <p>
         * mMouseWheelInfo represents the amount the user has moved the mouseWheel since the mouse
         * was last polled.  It will be an integer between -127 and +127.
         * <p>
         * mMousePosX and mMousePosY indicate the position of the mouse cursor on the
         * display.  If setDisplayResolution(int,int) has not been called, then
         * mMousePosX=mMousePosY=0.
         *
         * @param mouseInfo {mMouseClickInfo, mMouseXDiff, mouseYDiff, mMouseWheelInfo, mMousePosX, mMousePosY} */
        void onMouseAction(final int[] mouseInfo);
    }

    // -------------------------------------------------------------------------
    // -----------------  MOUSE POLLING SERVICE METHODS  -----------------------
    // -------------------------------------------------------------------------

    /** This class allows your application to retrieve the instantiated Bluetooth
   * Low Energy mouse service.  DO NOT USE IF YOU ARE RETRIEVING MOUSE OUTPUT
   * VIA POLLING! */
    public class LocalBinder extends Binder {
        /** Return this instance of BLEmouseDriver so clients can call public methods
         * @return TurretMouseService*/
        public TurretMouseService getService() {
            return TurretMouseService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Binding to driver");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Unbinding from driver");
        stopScanForMouse();
        disconnect();
        return super.onUnbind(intent);
    }

    // -----------------------------------------------------------------------------
    // -----------------------------  UI HELPER FUNCTIONS  -------------------------
    // -----------------------------------------------------------------------------

    public void showToast(final String message, final int duration) {
        if(ENABLE_TOASTS) {
            mHandler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(
                                    mTurretMouseServiceContext,
                                    message,
                                    duration
                            ).show();
                        }
                    }
            );
        }
    }

    // --------------------------------------------------------------------------------
    // -----------------  BLE MOUSE CONNECTION ESTABLISHMENT METHODS  -----------------
    // --------------------------------------------------------------------------------

    /** Used to start device discovery for a new Bluetooth Low Energy mouse.*/
    public void startScanForMouse() {

        scanLeDevice(false);
        disconnect();
        mHidJni.discoverMouse();
        showToast("Scanning for a compatible mouse",Toast.LENGTH_LONG);

        while (mBluetoothGatt != null) {
            ;
        } //TODO: REMOVE BUSY WAIT

        initializeBLE();
        scanLeDevice(true);
        mRebooting = false;
    }

    /** Used to stop device discovery for a new Bluetooth Low Energy mouse.
     *  This is automatically called if {@link #startScanForMouse()} establishes
     *  a connection with a mouse.  It is not necessary to call this function
     *  unless you wish to stop device discovery prematurely.  Calling this
     *  also does not prevent receiving input from Bluetooth Low Energy
     *  mice that the system has already paired with.*/
    public void stopScanForMouse() {
        //showToast("Stopped scanning for a compatible mouse", Toast.LENGTH_LONG);
        scanLeDevice(false);
        mHidJni.stopDiscoverMouse();
    }

    private void scanLeDevice(final boolean enable) {

        if (enable) {
            //final String myUUID = "00002a33-0000-1000-8000-00805f9b34fb"; // Boot Mouse Input Report
            final String myUUID = "00001812-0000-1000-8000-00805f9b34fb"; // Boot Mouse HID Report
            UUID scanuuid[] = new UUID[]{(UUID.fromString(myUUID))};

            mScanning = true;

            /*ScanFilter.Builder scanFilterBuilder = new ScanFilter.Builder();
            scanFilterBuilder.setServiceUuid(new ParcelUuid(scanuuid[0]));
            mScanFilters = new ArrayList<ScanFilter>();
            mScanFilters.add(scanFilterBuilder.build());

            ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
            //scanSettingsBuilder.setScanMode()
            mScanSettings = scanSettingsBuilder.build();

            mBluetoothScanner.startScan(mScanFilters, mScanSettings, mLeScanCallback);*/
            //mBluetoothScanner.startScan(mLeScanCallback);
        } else {
            mScanning = false;
            //if(mBluetoothScanner != null)
            //   mBluetoothScanner.stopScan(mLeScanCallback);
        }
    }

    /*private ScanCallback mLeScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            // Scan on a different thread to avoid locking up the UI

            //final String myUUID = "00002a33-0000-1000-8000-00805f9b34fb"; // HID Device
            // Information UUID
            final String myUUID = "00001812-0000-1000-8000-00805f9b34fb"; // Boot Mouse HID Report
            final String razerUUID = "5240263a-f97c-7f90-0e7f-6c6f4e36db1c"; // Turret Mouse Custom Service
            final String repUUID = "00002a4d-0000-1000-8000-00805f9b34fb"; // Boot Mouse Input
            final String razerRepUUID = "52401526-f97c-7f90-0e7f-6c6f4e36db1c"; // Turret Mouse Custom Report
            final String devNameUUID = "00002a00-0000-1000-8000-00805f9b34fb"; // Report UUID
            final String pnpidUUID = "00002a50-0000-1000-8000-00805f9b34fb"; // PnP ID UUID

            final UUID reportuuid = UUID.fromString(razerRepUUID);
            final UUID devnameuuid = UUID.fromString(devNameUUID);
            final UUID pnpiduuid = UUID.fromString(pnpidUUID);

            //SCAN SERVICES TO SEE IF BOOT MOUSE IS ONE OF THE SECONDARY SERVICES
            List<ParcelUuid> supportedServices;
            supportedServices = result.getScanRecord().getServiceUuids();
            String devName = result.getDevice().getName();
            if (supportedServices != null) {
                for (ParcelUuid curService : supportedServices) {
                    Log.i(TAG, devName+": "+curService.toString());
                    if (curService.equals(ParcelUuid.fromString(myUUID)) && devName.equals("Turret Mouse")) {
                        Log.v(TAG, "found mouse");
                        if(!mPairingLock && !ismConnected()) {
                            mPairingLock = true;
                            mHandler.postDelayed(new Runnable() {
                                public void run() {
                                    if(!mConnected)
                                        disconnect();
                                    mPairingLock = false;
                                }
                            },10000);
                            new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        //scanLeDevice(false);

                                        mDevice = result.getDevice();
                                        connect(mDevice);

                                        while (mReportServices.isEmpty() && mPairingLock) {
                                            ;
                                        } //TODO: REMOVE BUSY WAIT

                                        if (mDeviceName.equals("")) {
                                            for (BluetoothGattService reportService : mReportServices) {
                                                BluetoothGattCharacteristic devNameChara = reportService.getCharacteristic(devnameuuid);
                                                BluetoothGattCharacteristic pnpIdChara = reportService.getCharacteristic(pnpiduuid);
                                                Log.i(TAG, "PNPID: ");
                                                if (devNameChara != null) {
                                                    readCharacteristic(devNameChara);
                                                }
                                                if (pnpIdChara != null) {
                                                    readCharacteristic(pnpIdChara);
                                                    if (mPnPID.equals("5021730")) {
                                                        scanLeDevice(false);
                                                        showToast("Pairing to " + mDeviceName, Toast.LENGTH_SHORT);
                                                        mHidJni.stopDiscoverMouse();
                                                        for (BluetoothGattService usefulReportService : mReportServices) {
                                                            if (usefulReportService.getCharacteristic(reportuuid) != null) {

                                                                //readCharacteristic(usefulReportService.getCharacteristic(reportuuid));
                                                                setNotifications(usefulReportService);

                                                                mConnectedDevice = result.getDevice();
                                                                mConnected = true;
                                                                showToast("Connected to " + mDeviceName, Toast.LENGTH_SHORT);
                                                            }
                                                        }
                                                        break;
                                                    }
                                                }
                                            }
                                            mReportServices = new ArrayList<BluetoothGattService>();
                                        }
                                    } finally {
                                        mPairingLock =false;
                                    }
                                }
                            }).start();
                        }
                        else {
                            Log.i(TAG, "Tried to pair when pairing lock was on!");
                        }
                    }
                }
            }
        };
    };*/

    // Set notifications for relevant Boot Mouse characteristics
    private void setNotifications(BluetoothGattService curService) {
        final String myUUID = "00002a4d-0000-1000-8000-00805f9b34fb"; // Boot Mouse Input
        final String razerUUID = "52401526-f97c-7f90-0e7f-6c6f4e36db1c"; // Razer Custom Mouse Input
        // Report UUID
        UUID reportuuid = UUID.fromString(razerUUID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            for (BluetoothGattCharacteristic chara : curService.getCharacteristics()) {
                if (chara.getUuid().equals(reportuuid)) {
                    setCharacteristicNotification(chara, true);
                    Log.v(TAG, "characteristicNotificationSet");
                }
            }
        }
    }

    private boolean connect(BluetoothDevice device) {
        mDeviceName = "";
        boolean rvalue = false;


        if ((null != mBluetoothAdapter) && (null != device)) {

            mBonded = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                device.createBond();
            }
            while(device.getBondState() != device.BOND_BONDED && mPairingLock) {;} //TODO: REMOVE BUSY WAIT

            if(mBluetoothGatt != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mBluetoothGatt.close();
            }
        /* Set up a new callback */

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            }
            //mBluetoothGatt = device.connectGatt(this, false, mGattCallback); //Why twice?

            Log.d(TAG, "Create a new GATT connection.");
            mConnectionState = STATE_CONNECTING;
            rvalue = true;
        }

        return rvalue;
    }

    // --------------------------------------------------------------------------------
    // -----------------  BLE MOUSE INFORMATION UPDATE METHODS  -----------------------
    // --------------------------------------------------------------------------------

    private void broadcastUpdate(final String action) {
        ;
    }

    private String toBinary( byte[] bytes )
    {
        StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
        for( int i = 0; i < Byte.SIZE * bytes.length; i++ )
            sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
        return sb.toString();
    }

    // Helper function to help convert bit strings that represent
    // negative integers
    private String flipBits(String bitString) {
        String newString = "1";
        for(int i=1; i<bitString.length(); i++) {
            if (bitString.charAt(i) == '0')
                newString = newString + "1";
            else
                newString = newString + "0";
        }
        return newString;
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            final byte[] data = characteristic.getValue();
            final String devNameUUID = "00002a00-0000-1000-8000-00805f9b34fb";
            UUID devnameuuid = UUID.fromString(devNameUUID);

            final String pnpidUUID = "00002a50-0000-1000-8000-00805f9b34fb"; // PnP ID UUID
            final UUID pnpiduuid = UUID.fromString(pnpidUUID);

            final String repUUID = "00002a4d-0000-1000-8000-00805f9b34fb"; // Boot Mouse Input
            final String razerUUID = "52401526-f97c-7f90-0e7f-6c6f4e36db1c"; // Razer Custom Mouse Input
            final UUID repuuid = UUID.fromString(razerUUID);

            if ((characteristic.getUuid()).equals(devnameuuid)) {
                mDeviceName = characteristic.getStringValue(0);
            } else if ((characteristic.getUuid()).equals(pnpiduuid)) {
                byte[] pnpid = characteristic.getValue();
                for (int i = 1; i <= 4; i++) {
                    mPnPID += pnpid[i];
                }
            } else if ((characteristic.getUuid()).equals(repuuid)) {
                //Log.v(TAG, "boot mouse input report read successful");

                if (null != data) {
                    if (data.length > 0) {
                        if (mDeviceName.equals("Lenovo Mice N700")) {
                            byte[] dataBytes = null;

                            if (characteristic.getInstanceId() == 0) {
                                dataBytes = Arrays.copyOf(data, 1);
                                mMouseClickInfo = (int) dataBytes[0];

                            } else if (characteristic.getInstanceId() == 1) {
                                dataBytes = Arrays.copyOf(data, 3);
                                String xLastByte = toBinary(new byte[]{dataBytes[0]});
                                String mixedByte = toBinary(new byte[]{dataBytes[1]});
                                String yFirstByte = toBinary(new byte[]{dataBytes[2]});
                                String xString = mixedByte.substring(4, 8) + xLastByte;
                                String yString = yFirstByte + mixedByte.substring(0, 4);

                                char xSign = xString.charAt(0);
                                char ySign = yString.charAt(0);

                                if (xSign == '1') // xString represents a negative number
                                    xString = flipBits(xString);
                                if (ySign == '1') // yString represents a negative number
                                    yString = flipBits(yString);

                                int xDiff = Integer.parseInt(xString.substring(1, xString.length()), 2);
                                int yDiff = Integer.parseInt(yString.substring(1, yString.length()), 2);

                                if (xSign == '1') // xString represents a negative number
                                    xDiff = -1 * xDiff;
                                if (ySign == '1') // yString represents a negative number
                                    yDiff = -1 * yDiff;

                                updateMousePosition(xDiff, yDiff);
                                updateWheelPosition(0);

                                processRazerReport();
                            }
                        } else if (mDeviceName.equals("Pearlyn Mouse") || mDeviceName.equals("Turret Mouse")) {
                            byte[] dataBytes = Arrays.copyOf(data, 8);

                            if (characteristic.getInstanceId() == 0) {
                                parseRazerReport(dataBytes);
                            }
                        }

                    }
                }
                //mJustRead = true;
            }
        }
    }

    private boolean processReportLock = false;
    public void parseRazerReport(final byte[] dataBytes) {
        mProcessingHandler.post(new Runnable() {
            public void run() {
                mMouseClickInfo = (int) dataBytes[0];
                int mouseWheelChange = (int) dataBytes[3];
                String xLastByte = toBinary(new byte[]{dataBytes[4]});
                String xFirstByte = toBinary(new byte[]{dataBytes[5]});
                String yLastByte = toBinary(new byte[]{dataBytes[6]});
                String yFirstByte = toBinary(new byte[]{dataBytes[7]});
                String xString = xFirstByte + xLastByte;
                String yString = yFirstByte + yLastByte;

                char xSign = xString.charAt(0);
                char ySign = yString.charAt(0);

                if (xSign == '1') // xString represents a negative number
                    xString = flipBits(xString);
                if (ySign == '1') // yString represents a negative number
                    yString = flipBits(yString);

                int xDiff = Integer.parseInt(xString.substring(1, xString.length()), 2);
                int yDiff = Integer.parseInt(yString.substring(1, yString.length()), 2);

                if (xSign == '1') // xString represents a negative number
                    xDiff = -1 * xDiff;
                if (ySign == '1') // yString represents a negative number
                    yDiff = -1 * yDiff;

                updateMousePosition(xDiff, yDiff);
                updateWheelPosition(mouseWheelChange);

                if(!processReportLock) {
                    processRazerReport();
                    processReportLock=true;
                    mProcessingHandler.postDelayed(new Runnable() {
                        public void run() {
                            processReportLock = false;
                        }
                    },16);
                }
            }
        });
    }

    private void processRazerReport() {
        if (mCallbackEnabled) {  // throw callback
            int[] mouseInfo = {mMouseClickInfo,
                    mMouseXDiff, mouseYDiff, mMouseWheelInfo,
                    (int) mMousePosX, (int) mMousePosY};
            if(mMouseReceiver != null)
                mMouseReceiver.onMouseAction(mouseInfo);
            mMouseXDiff = 0;
            mouseYDiff = 0;
            mMouseWheelInfo = 0;
        }
    }

    private void updateMousePosition(int diffX, int diffY) {
        mMouseXDiff += diffX;
        mouseYDiff += diffY;

        double newMousePosX = mMousePosX + diffX * mSensitivityX;
        double newMousePosY = mMousePosY + diffY * mSensitivityY;

        if (newMousePosX > mDisplayResolutionX)
            mMousePosX = mDisplayResolutionX;
        else if (newMousePosX < 0)
            mMousePosX = 0;
        else
            mMousePosX = newMousePosX;

        if (newMousePosY > mDisplayResolutionY)
            mMousePosY = mDisplayResolutionY;
        else if (newMousePosY < 0)
            mMousePosY = 0;
        else
            mMousePosY = newMousePosY;
    }

    private void updateWheelPosition(int mouseWheelChange) {
        mMouseWheelInfo += mouseWheelChange;

        if (mMouseWheelInfo > 127)
            mMouseWheelInfo = 127;
        if (mMouseWheelInfo < -127)
            mMouseWheelInfo = -127;
    }

    // --------------------------------------------------------------------
    // -----------------  BLE SERVICE MANAGEMENT METHODS  -----------------
    // --------------------------------------------------------------------

    //gatt service callbacks
    private void newGattCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mGattCallback = new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    String intentAction;

                    Log.i(TAG, "State change ->" + newState + "    status ->" + status);

                    if (BluetoothGatt.GATT_SUCCESS == status) {
                        if (BluetoothProfile.STATE_CONNECTED == newState) {
                            intentAction = ACTION_GATT_CONNECTED;
                            mConnectionState = STATE_CONNECTED;
                            broadcastUpdate(intentAction);
                            Log.i(TAG, "Connected to GATT server.");
                            // Attempts to discover services after successful connection.
                            while(mBluetoothGatt == null && mPairingLock) {;} //TODO: REMOVE BUSY WAIT
                            Log.i(TAG, "Attempting to start service discovery:" +
                                    mBluetoothGatt.discoverServices());

                        } else if (BluetoothProfile.STATE_DISCONNECTED == newState) {
                            intentAction = ACTION_GATT_DISCONNECTED;
                            mConnectionState = STATE_DISCONNECTED;
                            Log.i(TAG, "Disconnected from GATT server.");
                            disconnect();
                            startScanForMouse();
                            broadcastUpdate(intentAction);
                        }
                    } else {
                        if(!mRebooting) {
                            mRebooting = true;
                            intentAction = ACTION_GATT_DISCONNECTED;
                            mConnectionState = STATE_DISCONNECTED;
                            Log.i(TAG, "Disconnected from GATT server due to error.");
                            broadcastUpdate(intentAction);

                            startScanForMouse();
                        }

                    /*//if (8 == status || 133 == status) { //timeout from GATT server, so reconnect and set notifications
                        if(status == 133) {
                            mCounter133++;
                        }
                        if (mCounter133 <= 3) {
                            final String repUUID = "00002a4d-0000-1000-8000-00805f9b34fb"; // Boot Mouse Input
                            // Report UUID
                            UUID reportuuid = UUID.fromString(repUUID);

                            disconnect();
                            mReportServices = new ArrayList<BluetoothGattService>();
                            connect(mDevice);
                            while (mReportServices.isEmpty()) {;} //TODO: REMOVE BUSY WAIT

                            for (BluetoothGattService reportService : mReportServices) {
                                if (reportService.getCharacteristic(reportuuid) != null) {
                                    setNotifications(reportService);
                                    mConnected = true;
                                }
                            }
                        }
                        else {
                            Log.e(TAG, "PERSISTENT 133 ERROR!");
                        }
                    }*/
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (BluetoothGatt.GATT_SUCCESS == status) {
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                        mReportServices = mBluetoothGatt.getServices();
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    //final String devNameUUID = "00002a00-0000-1000-8000-00805f9b34fb";
                    //UUID devnameuuid = UUID.fromString(devNameUUID);
                    if (BluetoothGatt.GATT_SUCCESS == status) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                        mJustRead = true;
                    }
                    /*else if ((characteristic.getUuid()).equals(devnameuuid)) {
                        mBluetoothGatt.readCharacteristic(characteristic);
                    }*/
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    if(status != BluetoothGatt.GATT_SUCCESS) {
                        Log.i(TAG, "DESCRIPTOR WRITE FAILED!!");
                    }
                    else {
                        Log.i(TAG, "DESCRIPTOR WRITE SUCCEEDED!!");
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    System.out.println("rssi ->" + rssi + " status  ->" + status);
                }
            };
        }
    }

    private List<BluetoothGattService> getSupportedGattServices() {
        List<BluetoothGattService> rvalue = null;

        if (null != mBluetoothGatt && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            rvalue = mBluetoothGatt.getServices();
        }

        return rvalue;
    }

    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if ((null == mBluetoothAdapter) || (null == mBluetoothGatt)) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            //initializeBLE();
        } else {
            mJustRead = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBluetoothGatt.readCharacteristic(characteristic);
            }
            while (!mJustRead && mPairingLock) {;} //TODO: REMOVE BUSY WAIT
        }
    }

    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                               boolean enabled) {

        //int property = 0;
        //property = characteristic.getProperties();

        /* Clear out any old notify */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothGatt.setCharacteristicNotification(characteristic, false);
        }
        /*if (0 != (property & BluetoothGattCharacteristic.PROPERTY_READ)) {
            //readCharacteristic(characteristic);
        }*/

        BluetoothGattDescriptor descriptor = null;

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        }
        /* Do a proper notify setup */
        if (characteristic.PROPERTY_NOTIFY > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC));
            }
            //descriptor = characteristic.getDescriptor(UUID.fromString(reportuuid));
            if (null != descriptor) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                }
            } else {
                Log.w(TAG, "Notification Error, descriptor doesn't exist");
            }
        }
    }

    /** Call this if you want the service to disconnect from the Bluetooth Low Energy mouse
     * that is currently in use. */
    public void disconnect() {

        Log.w(TAG, "disconnect() called");
        mDeviceName = "";
        mPnPID = "";
        if ((null == mBluetoothAdapter) || (null == mBluetoothGatt)) {
            Log.w(TAG, "BluetoothAdapter not initialized");
        } else {
            mConnectedDevice = null;
            mConnected = false;
            if(mBluetoothGatt != null)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mBluetoothGatt.close();
                }

            mBluetoothGatt = null;
        }
        mHidJni.stopMouse();
        //showToast("All compatible mice have been disconnected", Toast.LENGTH_LONG);
    }
}

// ************************************************************************************