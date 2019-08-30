/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.docobo.bluetooth;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.docobo.device.PlatformInfo;
import com.docobo.logger.Logger;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static com.docobo.bluetooth.LocalBluetoothManager.ACTION_GATT_CLIENT_CONNECTED;
import static com.docobo.bluetooth.LocalBluetoothManager.ACTION_GATT_CLIENT_CONNECTION_STATE_CHANGED;
import static com.docobo.bluetooth.LocalBluetoothManager.ACTION_GATT_CLIENT_DISCONNECTED;
import static com.docobo.bluetooth.LocalBluetoothManager.EXTRA_CONNECTION_STATE;
import static com.docobo.bluetooth.LocalBluetoothManager.EXTRA_PREVIOUS_CONNECTION_STATE;
import static com.docobo.bluetooth.LocalBluetoothManager.STATE_CONNECTED;
import static com.docobo.bluetooth.LocalBluetoothManager.STATE_CONNECTING;
import static com.docobo.bluetooth.LocalBluetoothManager.STATE_DISCONNECTED;
import static com.docobo.bluetooth.LocalBluetoothManager.STATE_DISCONNECTING;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@SuppressLint({"NewApi"})
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private class BluetoothLeListener extends BluetoothEventListener {
        @Override
        public boolean onPairingEventOccurred(BluetoothDevice device, boolean requestedOrCancelled, int pairingVariant) {
            boolean result = false;

            if (device.equals(mBluetoothDevice)) {
                result = processPairingRequest(device, pairingVariant);
            }

            return result;
        }

        @Override
        public void onBondingStateChanged(BluetoothDevice device, int prevBondingState, int newBondingState) {
            if (device.equals(mBluetoothDevice)) {
                if (newBondingState == BluetoothDevice.BOND_BONDED) {
                    mHandler.scheduleServiceDiscoveryStart(device);
                }
            }
        }
    }

    private class BluetoothEventHandler extends Handler {
        final int MESSAGE_DISCOVER_SERVICES = 0;
        final int MESSAGE_PROCESS_PAIRING_REQUEST = 1;

        public BluetoothEventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_DISCOVER_SERVICES: {
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    if (device != null && device.equals(mBluetoothDevice)) {
                        boolean discoverServices = mBluetoothGatt != null && mBluetoothGatt.discoverServices();
                        Logger.d(TAG, "Device %s[%s] bonded successfully, starting service discovery: ", device.getName(), device.getAddress(), discoverServices);
                    }
                    break;
                }
                case MESSAGE_PROCESS_PAIRING_REQUEST: {
                    processPairingRequest((BluetoothDevice) msg.obj, msg.arg1);
                    break;
                }
                default: {
                    Logger.w(TAG, "Unexpected message: %d", msg.what);
                }
            }
        }

        public void scheduleServiceDiscoveryStart(BluetoothDevice device) {
            removeMessages(MESSAGE_DISCOVER_SERVICES);
            Message message = obtainMessage(MESSAGE_DISCOVER_SERVICES, device);
            sendMessageDelayed(message, 500);
        }

        public void schedulePairingProcessCompletion(BluetoothDevice device, int pairingVariant) {
            removeMessages(MESSAGE_DISCOVER_SERVICES);
            Message message = obtainMessage(MESSAGE_DISCOVER_SERVICES, device);
            sendMessageDelayed(message, 500);
        }
    }

    //private BluetoothManager mBluetoothManager;
    private LocalBluetoothManager mBluetoothManager;
    private BluetoothLeListener mBluetoothLeListener;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    //private BluetoothDeviceHelper mBluetoothDeviceHelper;
    private BluetoothEventHandler mHandler;
    private boolean listening;
    private int mConnectionState = STATE_DISCONNECTED;

    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.docobo.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.docobo.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.docobo.bluetooth.le.EXTRA_DATA";

    // Implements callback methods for GATT events that the app cares about. For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "+++ onConnectionStateChange +++");

            String intentAction;
            if (newState == STATE_CONNECTED) {
                intentAction = ACTION_GATT_CLIENT_CONNECTED;
                setConnectionState(STATE_CONNECTED);
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                //Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_BONDED) {
                    mHandler.scheduleServiceDiscoveryStart(gatt.getDevice());
                }
            } else if (newState == STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_CLIENT_DISCONNECTED;
                setConnectionState(STATE_DISCONNECTED);
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }

            Log.i(TAG, "--- onConnectionStateChange ---");
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "+++ onServicesDiscovered +++");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }

            Log.i(TAG, "--- onServicesDiscovered ---");
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "+++ onCharacteristicRead +++");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "+++ onCharacteristicChanged +++");

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "+++ onDescriptorWrite +++");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onDescriptorWrite BluetoothGatt.GATT_SUCCESS");

                setListening(true);

                handleDescriptorWriteSuccess(descriptor);
            } else {
                Log.i(TAG, String.format("onDescriptorWrite BluetoothGatt.GATT_FAILED (%d)", status));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "+++ onCharacteristicWrite +++");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onCharacteristicWrite BluetoothGatt.GATT_SUCCESS");
            } else {
                Log.i(TAG, String.format("onCharacteristicWrite BluetoothGatt.GATT_FAILED (%d)", status));
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "+++ onDescriptorRead +++");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onDescriptorRead BluetoothGatt.GATT_SUCCESS");
            } else {
                Log.i(TAG, String.format("onDescriptorRead BluetoothGatt.GATT_FAILED (%d)", status));
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.i(TAG, "+++ onReadRemoteRssi +++");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, String.format("onReadRemoteRssi BluetoothGatt.GATT_SUCCESS (RSSI: %d)", rssi));
            } else {
                Log.i(TAG, String.format("onReadRemoteRssi BluetoothGatt.GATT_FAILED (%d)", status));
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.i(TAG, "+++ onReliableWriteCompleted +++");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onReliableWriteCompleted BluetoothGatt.GATT_SUCCESS");
            } else {
                Log.i(TAG, String.format("onReliableWriteCompleted BluetoothGatt.GATT_FAILED (%d)", status));
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        intent.setPackage(getPackageName());

        String characteristicID = characteristic.getUuid().toString();
        if (characteristicID.equals(GattAttributes.HEART_RATE_MEASUREMENT)) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else if (characteristicID.equals(GattAttributes.NONIN_OXIMETRY_MEASUREMENT)) {
            int flag = characteristic.getProperties();
            Log.d(TAG, "characteristic.getProperties " + flag);
            if ((flag & 0x01) != 0) {
                Log.d(TAG, "Pulse Oximetry format UINT16.");
            } else {
                Log.d(TAG, "Pulse Oximetry format UINT8.");
            }
            int pulseRateMSB = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 8);
            int pulseRateLSB = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 9);
            int pulseRate = unsignedBytesToInt((byte) pulseRateLSB, (byte) pulseRateMSB);

            int spo2Rate = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 7);

            int statusMsg = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            boolean syncModeEnabled = getNthBitValue(statusMsg, 0) != 0;
            boolean lowPulseSignalDetected = getNthBitValue(statusMsg, 1) != 0;
            boolean highQualityCheckPassed = getNthBitValue(statusMsg, 2) != 0;
            boolean searchingForPulseSignals = getNthBitValue(statusMsg, 3) != 0;
            boolean fingerPositionCorrect = getNthBitValue(statusMsg, 4) != 0;
            boolean lowBattery = getNthBitValue(statusMsg, 5) != 0;

            int batteryVoltageInt = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2);
            float batteryVoltage = 0.1f * batteryVoltageInt;

            int packetNumber = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5);

            StringBuilder result = new StringBuilder();
            result.append(String.format("SpO2: %d%%%%, ", spo2Rate));
            result.append(String.format("Pulse: %d, ", pulseRate));
            result.append(String.format("Volt: %.1f, ", batteryVoltage));
            result.append(String.format("Seq: %d, ", packetNumber));
            result.append(String.format("Status: %d", statusMsg));
            String readingString = result.toString();

            String statusString = "SyncMode: " + syncModeEnabled;
            statusString += ", LowPulse: " + lowPulseSignalDetected;
            statusString += ", SmartPoint: " + highQualityCheckPassed;
            statusString += ", PulseSearching: " + searchingForPulseSignals;
            statusString += ", FingerPosCorrect: " + fingerPositionCorrect;
            statusString += ", LowBattery: " + lowBattery;


            intent.putExtra(EXTRA_DATA, String.format(readingString + "(%s)", Integer.toBinaryString(statusMsg)));
            readingString = String.format(readingString + " (%s)", statusString);
            intent.putExtra(EXTRA_DATA, readingString);

            Log.d(TAG, "Reading Value: " + readingString);
        } else if (characteristicID.equals(GattAttributes.BATTERY_LEVEL_PERCENTAGE)) {
            byte[] value = characteristic.getValue();
            if (value != null) {
                int percentage = value[0] & 0xff;
                if (percentage > 100) {
                    percentage = 100;
                } else if (percentage < 0) {
                    percentage = 0;
                }

                intent.putExtra(EXTRA_DATA, String.format("Battery Percentage: %d", percentage));
                Log.d(TAG, "Battery percentage: " + percentage + "%");
            } else {
                Log.d(TAG, "Battery percentage reading Value was null");
                intent.putExtra(EXTRA_DATA, String.format("Battery Percentage: Null"));
            }
//        } else if (characteristicID.equals(GattAttributes.MARSDEN_WEIGHT_CHARACTERISTIC)) {
//            byte[] byteData = characteristic.getValue();
//            String marsdenData = new String(byteData);
//            if (buffer == null || marsdenData.startsWith("MODEL"))
//                buffer = new StringBuilder();
//
//            Logger.d(TAG, "MarsdenData (Length=%d/%d): %s", marsdenData.length(), byteData.length, marsdenData);
//            Logger.d(TAG, "MarsdenData Buffer Length: %d --> %d", buffer.length(), (buffer.length() + marsdenData.length()));
//
//            buffer.append(marsdenData);
//            if (buffer.length() >= 290) {
//                String receivedData = buffer.toString();
//                boolean parseDataNow = false;
//                if (receivedData.startsWith("MODEL")) {
//                    if (buffer.length() == 310)
//                        parseDataNow = true;
//                } else {
//                    parseDataNow = true;
//                }
//
//                if (parseDataNow) {
//                    MarsdenMeasurementData data = new MarsdenMeasurementData(buffer.toString());
//
//                    Logger.d(TAG, "MarsdenData (Length=%d): %s", marsdenData.length(), data.getRawDataString());
//                    Logger.d(TAG, "Marsden Measurement: %s", data.isValid() ? data.toDisplayableString() : "Invalid");
//
//                    buffer = null;
//                    intent.putExtra(EXTRA_DATA, data.isValid() ? data.toDisplayableString() : data.getRawDataString());
//                }
//            }
//        } else if (characteristicID.equals(GattAttributes.CURRENT_TIME_CHARACTERISTIC)) {
//            intent.putExtra(EXTRA_DATA, new GattCurrentTime(characteristic).toString());
//        } else if (characteristicID.equals(GattAttributes.LOCAL_TIME_CHARACTERISTIC)) {
//            intent.putExtra(EXTRA_DATA, new GattLocalTimeInformation(characteristic).toString());
//        } else if (characteristicID.equals(GattAttributes.REFERENCE_TIME_CHARACTERISTIC)) {
//            intent.putExtra(EXTRA_DATA, new GattReferenceTime(characteristic).toString());
//        } else if (characteristicID.equals(GattAttributes.PULSE_OXIMETER_FEATURES)) {
//            intent.putExtra(EXTRA_DATA, new GattPulseOximetryFeatures(characteristic).toString());
//        } else if (characteristicID.equals(GattAttributes.PULSE_OXIMETRY_CONTINUOUS_MEASUREMENT)) {
//            intent.putExtra(EXTRA_DATA, new GattPulseOximetryContinuousMeasurement(characteristic).toString());
//        } else if (characteristicID.equals(GattAttributes.GATT_CHARACTERISTIC_BODY_WEIGHT_MEASUREMENT)) {
//            intent.putExtra(EXTRA_DATA, new GattCharacteristicBodyWeightMeasurement(characteristic).toString());
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }

        sendBroadcast(intent);
    }

    private StringBuilder buffer = null;

    private int getNthBitValue(int intValue, int bitNumber) {
        return (int) (intValue & (1l << bitNumber));
    }

    /**
     * Convert a signed byte to an unsigned int.
     */
    private int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    /**
     * Convert signed bytes to a 16-bit unsigned int.
     */
    private int unsignedBytesToInt(byte b0, byte b1) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8));
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();
    private final boolean docoboDevice;

    public BluetoothLeService() {
        this.docoboDevice = PlatformInfo.isDocoboDevice();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBluetoothManager != null && mBluetoothLeListener != null) {
            mBluetoothManager.unregisterBluetoothEventListener(mBluetoothLeListener);
            mBluetoothLeListener = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler.getLooper().quit();
            mHandler = null;
        }
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = LocalBluetoothManager.getInstance(getApplicationContext());
            if (mBluetoothManager == null) {
                //Log.e(TAG, "Unable to initialize BluetoothManager.");
                Log.e(TAG, "Unable to initialize CompatibilityBluetoothAdapter");
                return false;
            }
        }

        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
                return false;
            }
        }

        if (mBluetoothLeListener == null) {
            mBluetoothLeListener = new BluetoothLeListener();
            mBluetoothManager.registerBluetoothEventListener(mBluetoothLeListener);
        }

        if (mHandler == null) {
            HandlerThread handlerThread = new HandlerThread("BluetoothLeEventHandler");
            handlerThread.start();
            mHandler = new BluetoothEventHandler(handlerThread.getLooper());
        }

        //mBluetoothDeviceHelper = new BluetoothDeviceHelper();
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     * <p>
     * This is the same as calling {@link #connect(String, boolean)} with false for pairWithDevice.
     * </p>
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        return connect(address, false);
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address        The device address of the destination device.
     * @param pairWithDevice Flag indicating whether this is a pairing process.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address, boolean pairWithDevice) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (mBluetoothDevice != null && address.equals(mBluetoothDevice.getAddress()) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                setConnectionState(STATE_CONNECTING);
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

//        if (pairWithDevice && isANDBT40Device(device)) {
//            boolean removeBondSuccess = mBluetoothDeviceHelper.removeBond(device);
//            Log.d(TAG, "Removing bond state for device " + device.getName() + " successful: " + removeBondSuccess);
//        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        //mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        mBluetoothGatt = mBluetoothManager.connectGatt(device, this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDevice = device;
        setConnectionState(STATE_CONNECTING);
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        setConnectionState(STATE_DISCONNECTING);
        setListening(false);
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        byte[] notificationValue;
        if (enabled) {
            notificationValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        } else {
            notificationValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            setListening(false);
        }

        boolean success = mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        Log.d(TAG, String.format("Characteristics notification %s %s", enabled ? "enable" : "disable", success ? "successful" : "failed"));

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        if (descriptor == null) {
            descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG_2));
        }

        if (descriptor != null) {
            descriptor.setValue(notificationValue);
            mBluetoothGatt.writeDescriptor(descriptor);
//            String characteristicName = GattAttributes.lookup(characteristic.getUuid().toString(), "Unknown");
//            Logger.d(TAG, "Client Characteristic Config Updated: %s ==> %s", characteristicName, BluetoothDeviceHelper.toHexString(notificationValue));

        }
    }

    /**
     *
     */
    public void writeCharacteristicValue(BluetoothGattCharacteristic characteristic, String value) {
        String characteristicName = GattAttributes.lookup(characteristic.getUuid().toString(), "Unknown");
        Logger.d(TAG, "Characteristic: %s --> %s", characteristicName, value);

        characteristic.setValue(value);
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * @param characteristic
     */
    public static void printAllDescriptors(BluetoothGattCharacteristic characteristic) {
        List<BluetoothGattDescriptor> gattDescriptors = characteristic.getDescriptors();
        StringBuilder sb = new StringBuilder();
        String characteristicName = GattAttributes.lookup(characteristic.getUuid().toString(), "Unknown");
        if (gattDescriptors == null || gattDescriptors.isEmpty()) {
            sb.append("No GattDescriptors found for ").append(characteristicName);
        } else {
            sb.append(String.format("%s (Properties: %s)", characteristicName, Integer.toBinaryString(characteristic.getProperties())));

            for (int index = 0; index < gattDescriptors.size(); index++) {
                BluetoothGattDescriptor descriptor = gattDescriptors.get(index);

                sb.append(String.format("\n%02d) %s", (index + 1), descriptor.getUuid().toString()));
            }
        }
        Logger.d(TAG, sb.toString());
    }

    /**
     * Write the provided descriptor to the bluetoothGattConnection
     *
     * @param descriptor
     */
    public boolean writerDescriptor(BluetoothGattDescriptor descriptor, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        if (enabled == false) {
            setListening(false);
        }

        return mBluetoothGatt.writeDescriptor(descriptor);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public BluetoothGattCharacteristic getCharacteristic(UUID serviceID, UUID characteristicID) {
        List<BluetoothGattService> services = getSupportedGattServices();
        if (services != null && services.size() > 0) {
            for (BluetoothGattService service : services) {
                if (serviceID == null || serviceID.equals(service.getUuid())) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicID);
                    if (characteristic != null) {
                        return characteristic;
                    }
                }
            }
        }

        return null;
    }

    private void setConnectionState(int connectionState) {
        if (this.mConnectionState != connectionState) {
            Intent connectionStateIntent = new Intent(ACTION_GATT_CLIENT_CONNECTION_STATE_CHANGED);
            connectionStateIntent.setPackage(getPackageName());
            connectionStateIntent.putExtra(EXTRA_PREVIOUS_CONNECTION_STATE, this.mConnectionState);
            connectionStateIntent.putExtra(EXTRA_CONNECTION_STATE, connectionState);
            connectionStateIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, this.mBluetoothDevice);

            this.mConnectionState = connectionState;

            sendBroadcast(connectionStateIntent);
        }
    }

    public int getConnectionState() {
        return mConnectionState;
    }

    public boolean isDocoboDevice() {
        return this.docoboDevice;
    }

    public synchronized boolean isListening() {
        return listening;
    }

    public synchronized void setListening(boolean listening) {
        if (this.mBluetoothGatt != null) {
            if (this.listening != listening) {
                if (this.mBluetoothManager.setListening(this.mBluetoothGatt, listening)) {
                    this.listening = listening;
                }
            }
        }
    }

    private void handleDescriptorWriteSuccess(BluetoothGattDescriptor descriptor) {
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        String characteristicID = characteristic.getUuid().toString();
        if (characteristicID.equals(GattAttributes.MARSDEN_WEIGHT_CHARACTERISTIC)) {
            writeCharacteristicValue(characteristic, "P");
        }
    }

    public static boolean isANDBT40Device(BluetoothDevice device) {
        if (device != null) {
//            if (ANDUC352BLEDevice.isANDUC352BLEDevice(device, true, null)) {
//                return true;
//            } else if (ANDUA651BLEDevice.isANDUA651BLEDevice(device, true, null)) {
//                return true;
//            }
        }

        return false;
    }

    public void setDateTimeForANDDevice() {
        BluetoothDevice device = mBluetoothGatt.getDevice();
        if (isANDBT40Device(device)) {
            Logger.d(TAG, "AND BLE device detected in pairing mode, setting Date/Time: %s", device.getName());
            boolean setDateSuccessful = false;
//            BluetoothGattCharacteristic characteristic = getCharacteristic(null, UUID.fromString(GattAttributes.GATT_CHARACTERISTIC_DATE_TIME));
//            if (characteristic != null) {
//                characteristic.setValue(GattCharacteristicDateTime.getDateTimeByteArray(Calendar.getInstance()));
//                setDateSuccessful = mBluetoothGatt.writeCharacteristic(characteristic);
//            }

            Logger.d(TAG, "AND BLE-Pairing setDateTimeSetting() success: %s", setDateSuccessful);
        }
    }

    public boolean processPairingRequest(BluetoothDevice device, int pairingVariant) {
        boolean result = false;

        if (mBluetoothDevice != null && mBluetoothDevice.equals(device)) {
            String value = null;
            switch (pairingVariant) {
                case BluetoothDevice.PAIRING_VARIANT_PIN: {
//                    if (value != null) {
//                        result = mBluetoothDeviceHelper.setPin(device, value);
//                    }
                    Logger.df(TAG, 15, "Setting PIN %s to pair with %s (Success: %s)", value, this.toString(), result);
                    break;
                }
                case LocalBluetoothManager.PAIRING_VARIANT_PASSKEY: {
//                    if (value != null) {
//                        result = mBluetoothDeviceHelper.setPasskey(device, value);
//                    }
                    Logger.df(TAG, 15, "Setting passkey %s to pair with %s (Success: %s)", value, this.toString(), result);
                    break;
                }
                case LocalBluetoothManager.PAIRING_VARIANT_PASSKEY_CONFIRMATION: {
//                    result = mBluetoothDeviceHelper.setPairingConfirmation(device, true);
                    Logger.df(TAG, 15, "Confirming passkey to pair with %s (Success: %s)", this.toString(), result);
                    break;
                }
                case LocalBluetoothManager.PAIRING_VARIANT_CONSENT: {
//                    result = mBluetoothDeviceHelper.setPairingConfirmation(device, true);
                    Logger.df(TAG, 15, "Consenting to pair with %s (Success: %s)", this.toString(), result);
                    break;
                }
                case LocalBluetoothManager.PAIRING_VARIANT_DISPLAY_PASSKEY: {
                    // Always true as there is nothing to do.
                    result = true;
                    Logger.df(TAG, 15, "Confirming pairing request without displaying passkey for %s (Success: %s)", this.toString(), result);
                    break;
                }
                case LocalBluetoothManager.PAIRING_VARIANT_OOB_CONSENT: {
//                    result = mBluetoothDeviceHelper.setRemoteOutOfBandData(device);
                    Logger.df(TAG, 15, "Setting Remote Out Of Band data to pair with %s (Success: %s)", this.toString(), result);
                    break;
                }
                default: {
                    Logger.w(TAG, "Unknown pairing type received (%d)", pairingVariant);
                    break;
                }
            }
        }

        return result;
    }
}