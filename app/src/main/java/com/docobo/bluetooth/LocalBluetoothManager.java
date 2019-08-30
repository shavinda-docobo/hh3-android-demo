/*
 *  Copyright (C) 2019 Docobo Ltd - All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 */

package com.docobo.bluetooth;

import static android.bluetooth.BluetoothAdapter.STATE_OFF;
import static android.bluetooth.BluetoothAdapter.STATE_ON;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.*;

import com.docobo.device.PlatformInfo;
import com.docobo.logger.Logger;

public class LocalBluetoothManager
{
	/**
	 *	Bluetooth device types supported by the service
	 */
	public enum BluetoothDeviceTypes
	{
		undefined,
		/** Bluetooth 2.0 devices **/
		Standard,
		/** Bluetooth 2.0 devices, on master mode **/
		Standard_MasterMode,
		/** Bluetooth 2.0 devices, using BluetoothProfiles (Health Device Profile). **/
		Standard_HDP,
		/** Bluetooth 4.0 (Bluetooth Low Energy) devices **/
		BluetoothLE;
	}

	private static final String TAG = "LocalBluetoothManager";
	public static final int DEBUG_LEVEL = 10;

	// private static final float SUPPORTED_FIRMWARE_MIN = 7.47f; TODO Add earliest support version
	public static final String FEATURE_BLUETOOTH_LE = "android.hardware.bluetooth_le";
	public static final String ACTION_BLUETOOTH_DEVICE_DISAPPEARED = "android.bluetooth.device.action.DISAPPEARED";
	public static final String EXTRA_BLUETOOTH_DEVICE_REASON = "android.bluetooth.device.extra.REASON";

	/**
	 * Intent used to broadcast the change in connection state of the local Bluetooth adapter to a profile of the remote device. When the adapter is
	 * not connected to any profiles of any remote devices and it attempts a connection to a profile this intent will sent. Once connected, this
	 * intent will not be sent for any more connection attempts to any profiles of any remote device. When the adapter disconnects from the last
	 * profile its connected to of any remote device, this intent will be sent.<p>
	 * This intent is useful for applications that are only concerned about whether the local adapter is connected to any profile of any device and
	 * are not really concerned about which profile. For example, an application which displays an icon to display whether Bluetooth is connected or
	 * not can use this intent.<p>
	 * This intent will have 3 extras: 
	 * <ul>
	 * 	<li>{@link #EXTRA_CONNECTION_STATE} - The current connection state</li>
	 *  <li>{@link #EXTRA_PREVIOUS_CONNECTION_STATE} - The previous connection state</li>
	 *  <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote device.</li>
	 * </ul>
	 * <p>
	 * EXTRA_CONNECTION_STATE or EXTRA_PREVIOUS_CONNECTION_STATE can be any of
	 * {@link #STATE_DISCONNECTED}, {@link #STATE_CONNECTING}, {@link #STATE_CONNECTED}, {@link #STATE_DISCONNECTING}. <p>
	 * 
	 * 
	 * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
	 */
	public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED";
    /**
     * Extra used by {@link #ACTION_CONNECTION_STATE_CHANGED}
     *
     * This extra represents the current connection state.
     */
    public static final String EXTRA_CONNECTION_STATE = "android.bluetooth.adapter.extra.CONNECTION_STATE";

    /**
     * Extra used by {@link #ACTION_CONNECTION_STATE_CHANGED}
     *
     * This extra represents the previous connection state.
     */
    public static final String EXTRA_PREVIOUS_CONNECTION_STATE = "android.bluetooth.adapter.extra.PREVIOUS_CONNECTION_STATE";

    /**
     * Broadcast fired when a bluetooth device is attempting to pair. Listen to this to set the pin<p>
     * 
     * Will contain the extras {@link BluetoothDevice#EXTRA_DEVICE} and {@link BluetoothDevice#EXTRA_PAIRING_VARIANT}
     */
    public static final String ACTION_BLUETOOTH_DEVICE_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
    /** The pairing variant indicate the type of pairing required for this device **/
    public static final String EXTRA_BLUETOOTH_DEVICE_PAIRING_VARIANT = "android.bluetooth.device.extra.PAIRING_VARIANT";
    /** Broadcast that can be sent to cancel the Android OS pairing UI **/
    public static final String ACTION_BLUETOOTH_DEVICE_PAIRING_CANCEL = "android.bluetooth.device.action.PAIRING_CANCEL";
    
    /** The profile is in disconnected state */
    public static final int STATE_DISCONNECTED  = 0;
    /** The profile is in connecting state */
    public static final int STATE_CONNECTING    = 1;
    /** The profile is in connected state */
    public static final int STATE_CONNECTED     = 2;
    /** The profile is in disconnecting state */
    public static final int STATE_DISCONNECTING = 3;
	
    /**
     * The user will be prompted to enter a pin
     * @hide
     */
    public static final int PAIRING_VARIANT_PIN = 0;

    /**
     * The user will be prompted to enter a passkey
     * @hide
     */
    public static final int PAIRING_VARIANT_PASSKEY = 1;

    /**
     * The user will be prompted to confirm the passkey displayed on the screen
     */
    public static final int PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2;

    /**
     * The user will be prompted to accept or deny the incoming pairing request
     */
    public static final int PAIRING_VARIANT_CONSENT = 3;

    /**
     * The user will be prompted to enter the passkey displayed on remote device
     * This is used for Bluetooth 2.1 pairing.
     */
    public static final int PAIRING_VARIANT_DISPLAY_PASSKEY = 4;

    /**
     * The user will be prompted to enter the PIN displayed on remote device.
     * This is used for Bluetooth 2.0 pairing.
     */
    public static final int PAIRING_VARIANT_DISPLAY_PIN = 5;

    /**
     * The user will be prompted to accept or deny the OOB pairing request
     */
    public static final int PAIRING_VARIANT_OOB_CONSENT = 6;
    
	public final static String ACTION_GATT_CLIENT_CONNECTED = "com.docobo.bluetooth.ACTION_GATT_CLIENT_CONNECTED";
	public final static String ACTION_GATT_CLIENT_DISCONNECTED = "com.docobo.bluetooth.ACTION_GATT_CLIENT_DISCONNECTED";
	public final static String ACTION_GATT_CLIENT_CONNECTION_STATE_CHANGED = "com.docobo.bluetooth.ACTION_GATT_CLIENT_CONNECTION_STATE_CHANGED";
	
    protected final static String ACTION_GATT_CONNECTED = "com.android.server.bluetooth.BluetoothGattManagerService.ACTION_GATT_CONNECTED";
    protected final static String ACTION_GATT_DISCONNECTED = "com.android.server.bluetooth.BluetoothGattManagerService.ACTION_GATT_DISCONNECTED";
    protected final static String ACTION_GATT_NOT_SUPPORTED = "com.android.server.bluetooth.BluetoothGattManagerService.ACTION_GATT_NOT_SUPPORTED";
	// private final int MINIMUM_BLUETOOTH_SCAN_PERIOD = 30000;

	private Method setScanModeAndDurationMethod = null;
	private Method setScanModeMethod = null;
	private Method getDiscoverableTimeoutMethod = null;
	private Method setDiscoverableTimeoutMethod = null;
	private Method hh3GATTRestartMethod = null; // TODO - figure out how to avoid the restart and report a meaningful failure for non HH3 devices
	private Method hh3IsGATTReadyMethod = null; // TODO - Report a GATT ready success result if bluetooth is On as this is some weird thing in the HH3 OS ONLY
	private Method hh3Listen = null; // BluetoothGatt listen method which should only be called on HH3 devices.
	
	private Method startLeScanMethod = null;
	private Method stopLeScanMethod = null;
	// private Method startLeScanForProfilesMethod = null; // Not supported on HH3 devices as of FW 7.47
	private Method connectBluetoothGattMethod = null;
	
	private final boolean isDocoboDevice;
	private boolean bluetooth40Supported = false;
	private BluetoothAdapter bluetoothAdapter;
	private BluetoothManager bluetoothManagerObject;
	private Context context;

	private BluetoothEventRedirector bluetoothEventRedirector;
	private Handler handler = null;

	/*
	 * Single instance of LocalBluetoothManager
	 */
	private static LocalBluetoothManager instance;

	/**
	 * Get LocalBluetoothManager instance
	 */
	public static LocalBluetoothManager getInstance(Context context)
	{
		if (instance == null)
		{
			LocalBluetoothManager blManager = new LocalBluetoothManager(context);
			if (blManager.isBluetoothSupported())
			{
				instance = blManager;
			}
		}

		return instance;
	}

	/**
	 * Cleanup the local bluetooth manager instance
	 */
	public static void cleanupInstance()
	{
		if (instance != null)
		{
			instance.cleanup();
			instance = null;
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private LocalBluetoothManager(Context context)
	{
		this.isDocoboDevice = PlatformInfo.isDocoboDevice();

		this.bluetoothAdapter = getBluetoothAdapter(context);
		this.bluetoothManagerObject = getBluetoothManagerObject(context);
		this.context = context.getApplicationContext();

		if (this.bluetoothAdapter == null)
		{
			Logger.warning(TAG, "Bluetooth not supported, adapter was null");
			return;
		}

		// Initialise the handler
		HandlerThread handlerThread = new HandlerThread("LocalBluetoothManager", Thread.MIN_PRIORITY);
		handlerThread.start();
		this.handler = new Handler(handlerThread.getLooper());

		// Initialise all reflection method objects
		this.setScanModeMethod = getMethod(TAG, BluetoothAdapter.class, "setScanMode", int.class);
		this.setScanModeAndDurationMethod = getMethod(TAG, BluetoothAdapter.class, "setScanMode", int.class, int.class);

		this.setDiscoverableTimeoutMethod = getMethod(TAG, BluetoothAdapter.class, "setDiscoverableTimeout", int.class);
		this.getDiscoverableTimeoutMethod = getMethod(TAG, BluetoothAdapter.class, "getDiscoverableTimeout");

		this.setDiscoverableTimeoutMethod = getMethod(TAG, BluetoothAdapter.class, "setDiscoverableTimeout", int.class);
		this.getDiscoverableTimeoutMethod = getMethod(TAG, BluetoothAdapter.class, "getDiscoverableTimeout");

		if (PlatformInfo.isDocoboDevice() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
		{
			this.connectBluetoothGattMethod = getMethod(TAG, BluetoothDevice.class, "connectGatt", Context.class, boolean.class, BluetoothGattCallback.class);
	
			this.startLeScanMethod = getMethod(TAG, BluetoothAdapter.class, "startLeScan", LeScanCallbackInterface.getInterface());
			this.stopLeScanMethod = getMethod(TAG, BluetoothAdapter.class, "stopLeScan", LeScanCallbackInterface.getInterface());
		}
		
		if (PlatformInfo.isDocoboDevice())
		{
			this.hh3GATTRestartMethod = getMethod(TAG, BluetoothAdapter.class, "GATTRestart");
			this.hh3IsGATTReadyMethod = getMethod(TAG, BluetoothAdapter.class, "isGATTReady");
			this.hh3Listen = getMethod(TAG, BluetoothGatt.class, "listen", boolean.class, int.class);
		}

		validateBluetooth40Supported();

		// Start the event detector and redirector
		this.bluetoothEventRedirector = new BluetoothEventRedirector(this);
		this.bluetoothEventRedirector.startRedirector();

		// Sync the current bluetooth state
		syncBluetoothState();
	}

	/**
	 * Cleanup the Local Bluetooth manager object
	 */
	private void cleanup()
	{
		this.bluetoothAdapter = null;
		this.bluetoothManagerObject = null;

		this.bluetoothEventRedirector.stopRedirector();
		this.bluetoothEventRedirector = null;

		this.bluetoothEventListeners.clear();
		
		this.handler.getLooper().quit();
		this.handler = null;
	}

	/**
	 * Validate if all the methods required to perform BluetoothLE operations have been initialised
	 */
	private void validateBluetooth40Supported()
	{
		boolean supported = true;

		if (this.isDocoboDevice)
		{
			if (this.hh3GATTRestartMethod == null)
			{
				supported = false;
			}

			if (this.hh3IsGATTReadyMethod == null)
			{
				supported = false;
			}
			
			if (this.hh3Listen == null)
			{
				supported = false;
			}
		}
		else if (isBluetoothLESupported(this.context))
		{
			if (this.bluetoothManagerObject == null)
			{
				supported = false;
			}
		}

		if (this.startLeScanMethod == null)
		{
			supported = false;
		}
		if (this.stopLeScanMethod == null)
		{
			supported = false;
		}

		this.bluetooth40Supported = supported;
	}

	/**
	 * Check if bluetooth is supported on the current platform
	 * 
	 * @return
	 */
	private boolean isBluetoothSupported()
	{
		if (this.bluetoothAdapter == null)
		{
			return false;
		}
		if (android.os.Build.VERSION.SDK_INT >= 18)
		{
			if (this.bluetoothManagerObject == null)
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Check if Bluetooth LE operations can be performed
	 *
	 * @return true if BluetoothLE is supported, false otherwise
	 */
	public boolean isBluetooth40Supported()
	{
		return this.bluetooth40Supported;
	}

	/**
	 * Get an instance of the Bluetooth Adapter
	 *
	 * @return
	 */
	public BluetoothAdapter getAdapter()
	{
		return this.bluetoothAdapter;
	}

	/**
	 * Get the Application context
	 *
	 * @return
	 */
	public Context getContext()
	{
		return context;
	}

	/*
	 ****************************************************************************************
	 * 																						*
	 * 		Method to access bluetooth 4.0 API and other hidden methods via reflection 		*
	 * 																						*
	 ****************************************************************************************
	 */
	/**
	 * Set the Bluetooth scan mode of the local Bluetooth adapter with the currently defined discoverable timeout period.
	 * <p>
	 *
	 * @see LocalBluetoothManager#setScanMode(int, int)
	 * @param scanMode
	 *            - valid scan mode
	 * @return true if the scan mode was set, false otherwise
	 */
	public boolean setScanMode(int scanMode)
	{
		boolean result = false;

		if (this.setScanModeMethod != null)
		{
			try
			{
				result = (Boolean) this.setScanModeMethod.invoke(this.bluetoothAdapter, scanMode);
			} catch (Exception e)
			{
				Logger.ex(TAG, "Error invoking setScanMode(int)", e);
			}
		}

		return result;
	}

	/**
	 * Set the Bluetooth scan mode of the local Bluetooth adapter.
	 * <p>
	 *
	 * The Bluetooth scan mode determines if the local adapter is connectable and/or discoverable from remote Bluetooth devices. For privacy reasons,
	 * discoverable mode is automatically turned off after duration seconds. For example, 120 seconds should be enough for a remote device to initiate
	 * and complete its discovery process.
	 * <p>
	 *
	 * Valid scan mode values are: {@link BluetoothAdapter#SCAN_MODE_NONE}, {@link BluetoothAdapter#SCAN_MODE_CONNECTABLE},
	 * {@link BluetoothAdapter#SCAN_MODE_CONNECTABLE_DISCOVERABLE}.
	 * <p>
	 *
	 * If Bluetooth state is not STATE_ON, this API will return false. After turning on Bluetooth, wait for
	 * {@link BluetoothAdapter#ACTION_STATE_CHANGED} with {@link BluetoothAdapter#STATE_ON} to get the updated value.
	 * <p>
	 *
	 * <b>Note:</b> Requires secure permissions, so may not be able to set this value on non HH3 devices.
	 *
	 * @param scanMode
	 *            - valid scan mode
	 * @param duration
	 *            - time in seconds to apply scan mode, only used for {@link BluetoothAdapter#SCAN_MODE_CONNECTABLE_DISCOVERABLE}
	 * @return true if the scan mode was set, false otherwise
	 */
	public boolean setScanMode(int scanMode, int duration)
	{
		boolean result = false;

		if (this.setScanModeAndDurationMethod != null)
		{
			try
			{
				result = (Boolean) this.setScanModeAndDurationMethod.invoke(this.bluetoothAdapter, scanMode, duration);
			} catch (Exception e)
			{
				Logger.ex(TAG, "Error invoking setScanMode(int, int)", e);
			}
		}

		return result;
	}

	/**
	 * Get the current discoverable timeout period when scan mode is set to {@link BluetoothAdapter#SCAN_MODE_CONNECTABLE_DISCOVERABLE}
	 * <p>
	 *
	 * If Bluetooth state is not STATE_ON, this API will return -1. After turning on Bluetooth, wait for {@link BluetoothAdapter#ACTION_STATE_CHANGED}
	 * with {@link BluetoothAdapter#STATE_ON} to get the updated value.
	 * <p>
	 *
	 * <b>Note:</b> Requires secure permissions, so may not be able to set this value on non HH3 devices.
	 *
	 * @return the discoverable timeout period in seconds or -1 if this call failed or was unsuccesful
	 */
	public int getDiscoverableTimeout()
	{
		int result = -1;

		if (this.getDiscoverableTimeoutMethod != null)
		{
			try
			{
				result = (Integer) this.getDiscoverableTimeoutMethod.invoke(this.bluetoothAdapter);
			} catch (Exception e)
			{
				Logger.ex(TAG, "Error invoking getDiscoverableTimeout()", e);
			}
		}

		return result;
	}

	/**
	 * Set the current discoverable timeout period when scan mode is set to {@link BluetoothAdapter#SCAN_MODE_CONNECTABLE_DISCOVERABLE}
	 * <p>
	 *
	 * If Bluetooth state is not STATE_ON, this API will return false. After turning on Bluetooth, wait for
	 * {@link BluetoothAdapter#ACTION_STATE_CHANGED} with {@link BluetoothAdapter#STATE_ON} to change this value.
	 * <p>
	 *
	 * @param timeout
	 *            - the new timeout period in seconds
	 * @return true if this value was set successfully, false otherwise
	 * @see #getDiscoverableTimeout()
	 */
	public boolean setDiscoverableTimeout(int timeout)
	{
		boolean result = false;

		if (this.setDiscoverableTimeoutMethod != null)
		{
			if (this.bluetoothAdapter.getState() == STATE_ON)
			{
				try
				{
					this.setDiscoverableTimeoutMethod.invoke(this.bluetoothAdapter, timeout);
					result = true;
				} catch (Exception e)
				{
					Logger.ex(TAG, "Error invoking setDiscoverableTimeoutMethod(int)", e);
				}
			}
		}

		return result;
	}

	/**
	 * Starts a scan for Bluetooth LE devices.
	 * Results of the scan are reported using the {@link LeScanCallbackInterface#onLeScan(BluetoothDevice, int, byte[])} callback.
	 *
	 * <p>
	 * Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
	 * <p>
	 *
	 * @param mLeScanCallback
	 * @return
	 */
	public boolean startLeScan(LeScanCallbackInterface mLeScanCallback)
	{
		boolean result = false;

		if (isBluetooth40Supported())
		{
			try
			{
				if (!isBluetoothLEScanStarted())
				{
					setBluetoothLEScanStarted(true);
					this.startLeScanMethod.invoke(this.bluetoothAdapter, mLeScanCallback.getLeScanCallback());

					Logger.df(TAG, 15,"BluetoothLE discorvery started");
					dispatchBluetoothScanningStateChanged(true);
				}
				result = true;
			} catch (Exception e)
			{
				Logger.ex(TAG, "Error invoking startLeScan", e);
			}
		}

		return result;
	}

	/**
	 * Stops an ongoing Bluetooth LE device scan.
	 *
	 * <p>
	 * Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
	 * <p>
	 *
	 * @param mLeScanCallback
	 *            callback used to identify which scan to stop must be the same handle used to start the scan
	 * @return
	 */
	public boolean stopLeScan(LeScanCallbackInterface mLeScanCallback)
	{
		boolean result = false;

		if (isBluetooth40Supported())
		{
			try
			{
				if (isBluetoothLEScanStarted())
				{
					setBluetoothLEScanStarted(false);
					this.stopLeScanMethod.invoke(this.bluetoothAdapter, mLeScanCallback.getLeScanCallback());

					Logger.df(TAG, 15,"BluetoothLE discorvery stopped");
					dispatchBluetoothScanningStateChanged(false);
				}
				result = true;
			} catch (Exception e)
			{
				Logger.ex(TAG, "Error invoking stopLeScan", e);
			}
		}

		return result;
	}

	/**
	 * Check if Gatt services are ready.
	 * <p>
	 * This check is only valid on HH3 devices, on any other device this method will always return true if bluetooth us enabled.
	 * </p>
	 *
	 * @return true if the Gatt service is ready, false otherwise
	 */
	public boolean isGattReady()
	{
		boolean result = false;

		if (isBluetooth40Supported())
		{
			if (this.isDocoboDevice)
			{
				try
				{
					result = (Boolean) this.hh3IsGATTReadyMethod.invoke(this.bluetoothAdapter);
				} catch (Exception e)
				{
					Logger.ex(TAG, "Error invoking isGattReady", e);
				}
			}
			else
			{
				result = this.bluetoothAdapter.isEnabled();
			}
		}

		return result;
	}

	/**
	 * Restart the Gatt service
	 *
	 * <p>
	 * This call is only valid on HH3 devices, on any other device the result will always be false and no action will be taken
	 * </p>
	 *
	 * @return true if the Gatt service restart was initiated, false otherwise
	 */
	public boolean restartGatt()
	{
		boolean result = false;

		if (isBluetooth40Supported() && this.isDocoboDevice)
		{
			try
			{
				this.hh3GATTRestartMethod.invoke(this.bluetoothAdapter);
				result = true;
			} catch (Exception e)
			{
				Logger.ex(TAG, "Error invoking restartGatt", e);
			}
		}

		return result;
	}

	/**
	 * TODO - This can be moved to BluetoothDeviceHelper in future
	 * Helper method to perform {@link BluetoothDevice#connectGatt} by reflection.
	 *
	 * <p>
	 * Connect to GATT Server hosted by this device. Caller acts as GATT client. The callback is used to deliver results to Caller, such as connection
	 * status as well as any further GATT client operations. The method returns a BluetoothGatt instance. You can use BluetoothGatt to conduct GATT
	 * client operations
	 * </p>
	 *
	 * @param device
	 *            - the bluetooth device to initialise the Gatt connection
	 * @param context
	 * @param autoConnect
	 *            - Whether to directly connect to the remote device (false) or to automatically connect as soon as the remote device becomes
	 *            available (true).
	 * @param callback
	 *            - GATT callback handler that will receive asynchronous callbacks.
	 * @return
	 * @throws IllegalArgumentException
	 *             if callback is null
	 */
	public BluetoothGatt connectGatt(BluetoothDevice device, Context context, boolean autoConnect, BluetoothGattCallback callback) throws IllegalArgumentException
	{
		BluetoothGatt bluetoothGatt = null;

		if (this.connectBluetoothGattMethod != null)
		{
			try
			{
				bluetoothGatt = (BluetoothGatt) this.connectBluetoothGattMethod.invoke(device, context, autoConnect, callback);
			} catch (Exception e)
			{
				Logger.ex(TAG, "Error invoking connectGatt", e);
			}
		}
		return bluetoothGatt;
	}

	/*
	 ****************************************************************************************
	 *																				 		*
	 * 			Convenience methods that can be used to support Bluetooth 2.0 & 4.0 		*
	 * 																						*
	 ****************************************************************************************
	 */
	private ArrayList<WeakReference<BluetoothEventListener>> bluetoothEventListeners = new ArrayList<>();

	ArrayList<WeakReference<BluetoothEventListener>> getBluetoothEventListeners()
	{
		return bluetoothEventListeners;
	}

	/**
	 * Register a listener for bluetooth callback events
	 *
	 * @param listener
	 */
	public void registerBluetoothEventListener(@NonNull BluetoothEventListener listener)
	{
		synchronized (this.bluetoothEventListeners)
		{
			boolean addListener = true;
			for (Iterator<WeakReference<BluetoothEventListener>> iterator = bluetoothEventListeners.iterator(); iterator.hasNext(); )
			{
				WeakReference<BluetoothEventListener> listenerRef =  iterator.next();
				if (listener.equals(listenerRef.get()))
				{
					addListener = false;
				}
				else if (listenerRef.get() == null)
				{
					iterator.remove();
				}
			}

			if (addListener)
			{
				this.bluetoothEventListeners.add(new WeakReference<>(listener));
			}
		}
	}

	/**
	 * Remove bluetooth callback event listener
	 *
	 * @param listener
	 */
	public void unregisterBluetoothEventListener(BluetoothEventListener listener)
	{
		synchronized (this.bluetoothEventListeners)
		{
			for (Iterator<WeakReference<BluetoothEventListener>> iterator = bluetoothEventListeners.iterator(); iterator.hasNext(); )
			{
				WeakReference<BluetoothEventListener> listenerRef =  iterator.next();
				if (listenerRef.get() == null || listener.equals(listenerRef.get()))
				{
					iterator.remove();
				}
			}
		}
	}

	private int bluetoothState = BluetoothAdapter.ERROR;

	public int getBluetoothState()
	{
		if (this.bluetoothState == BluetoothAdapter.ERROR)
		{
			syncBluetoothState();
		}

		return this.bluetoothState;
	}

	void setBluetoothStateInt(int state)
	{
		if (this.bluetoothState != state)
		{
			this.bluetoothState = state;

			dispatchBluetoothStateChangeEvent();
		}
	}

	private void syncBluetoothState()
	{
		int bluetoothState;

		if (this.bluetoothAdapter != null)
		{
			bluetoothState = this.bluetoothAdapter.isEnabled() ? STATE_ON : STATE_OFF;
		}
		else
		{
			bluetoothState = BluetoothAdapter.ERROR;
		}

		setBluetoothStateInt(bluetoothState);
	}

	private boolean bluetoothLEScanStarted = false;
	public void setBluetoothLEScanStarted(boolean bluetoothLEScanStarted)
	{
		this.bluetoothLEScanStarted = bluetoothLEScanStarted;
	}
	public boolean isBluetoothLEScanStarted()
	{
		return bluetoothLEScanStarted;
	}

	private void dispatchBluetoothStateChangeEvent()
	{
		if (this.bluetoothState == STATE_ON || this.bluetoothState == STATE_OFF)
		{
			synchronized (this.bluetoothEventListeners)
			{
				boolean enable = this.bluetoothState == STATE_ON;
				for (WeakReference<BluetoothEventListener> callback : this.bluetoothEventListeners)
				{
                    if (callback.get() != null)
                        callback.get().onBluetoothStateChanged(enable);
				}
			}
		}
	}

	void dispatchBluetoothScanningStateChanged(boolean started)
	{
		synchronized (this.bluetoothEventListeners)
		{
            for (WeakReference<BluetoothEventListener> callback : this.bluetoothEventListeners)
			{
                if (callback.get() != null)
				    callback.get().onDiscoveryStateChanged(started);
			}
		}
	}

	/**
	 * Start/Stop bluetooth discover.
	 *
	 * @param enable
	 *            - true to start, false to stop discovery
	 * @param bluetoothLE
	 *            - true perform the operation for Bluetooth LE interface, false for standard Bluetooth interface
	 * @return
	 */
	public boolean setBluetoothDiscoveryEnabled(boolean enable, boolean bluetoothLE)
	{
		boolean result = false;

		if (bluetoothLE)
		{
			if (enable)
			{
				result = startLeScan(this.bluetoothEventRedirector.getBluetoothScanCallBack());
			}
			else
			{
				result = stopLeScan(this.bluetoothEventRedirector.getBluetoothScanCallBack());
			}
		}
		else
		{
			if (enable)
			{
				if (this.bluetoothAdapter.isDiscovering())
				{
					result = true;
				}
				else
				{
					result = this.bluetoothAdapter.startDiscovery();
				}
			}
			else
			{
				if (this.bluetoothAdapter.isDiscovering())
				{
					result = this.bluetoothAdapter.cancelDiscovery();
				}
				else
				{
					result = true;
				}
			}
		}

		return result;
	}

	public boolean setListening(BluetoothGatt bluetoothGatt, boolean listening)
	{
		boolean result = false;

		if (isBluetooth40Supported())
		{
			if (this.isDocoboDevice)
			{
				try
				{
					this.hh3Listen.invoke(bluetoothGatt, listening, 0);
					result = true;
				} catch (Exception e)
				{
					Logger.ex(TAG, "Error invoking listen(booleam, int)", e);
				}
			}
			else
			{
				// This step is not required for Non-HH3 devices.
				// So any calls to this method will always reuturn true.
				result = true;
			}
		}

		return result;
	}

	/**
	 * Attempts to enable/disable the bluetooth adapter.
	 *
	 * @param enable - true to enable, false to disable bluetooth
	 */
	public boolean setBluetoothEnabled(boolean enable)
	{
		// If BT is already in the requested state, return true.
		if (this.bluetoothAdapter.isEnabled() == enable)
		{
			return true;
		}

		return enable ? this.bluetoothAdapter.enable() : this.bluetoothAdapter.disable();
	}
	/**
	 * Returns whether the bluetooth hardware is currently enabled or not.
	 * @return
	 */
	public boolean isBluetoothEnabled()
	{
		return this.bluetoothAdapter.isEnabled();
	}

	/*
	 ****************************************************************************************
	 *																						*
	 *								 Static convenience methods 							*
	 *																						*
	 ****************************************************************************************
	 */
	/**
	 * Get the Bluetooth Manager object
	 *
	 * @param context
	 * @return the bluetooth manager object or null
	 */
	public static BluetoothManager getBluetoothManagerObject(Context context)
	{
		BluetoothManager bluetoothManager = null;

		if (android.os.Build.VERSION.SDK_INT >= 18)
		{
			bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		}

		return bluetoothManager;
	}

	/**
	 * Initialise a BluetoothAdapter or null if Bluetooth is not supported
	 * <p>
	 * For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
	 * </p>
	 *
	 * @param context
	 * @return {@link BluetoothAdapter} instance
	 */
	public static BluetoothAdapter getBluetoothAdapter(final Context context)
	{
		if (Looper.myLooper() != null)
		{
			return getBluetoothAdapterInLooper(context);
		}
		else
		{
			// Call not made in a looper thread. So this could cause an exception.
			final AtomicReference<BluetoothAdapter> adapterReference = new AtomicReference<>();
			final AtomicBoolean processComplete = new AtomicBoolean(false);
			new Handler(Looper.getMainLooper())
			.post(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						adapterReference.set(getBluetoothAdapter(context));
					}
					catch (Exception e)
					{
						Logger.ex(TAG, "Error attempting getBluetoothAdapter()", e);
					}
					finally
					{
						processComplete.set(true);
					}
				}
			});

			while (!processComplete.get())
			{
				try
				{
					Thread.sleep(100);
				} catch (InterruptedException e)
				{
				}
			}

			return adapterReference.get();
		}
	}

	/**
	 * Returns the {@link BluetoothAdapter}. This method should be called in a Thread that has a Looper.
	 *
	 * @param context
	 * @return
	 */
	private static BluetoothAdapter getBluetoothAdapterInLooper(Context context)
	{
		BluetoothAdapter bluetoothAdapter = null;
		try
		{
			Context appContext = context.getApplicationContext();
			if (android.os.Build.VERSION.SDK_INT >= 18)
			{
				Object bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE);
				if (bluetoothManager != null && bluetoothManager instanceof BluetoothManager)
				{
					bluetoothAdapter = ((BluetoothManager) bluetoothManager).getAdapter();
				}
			}
			else
			{
				bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			}
		}
		catch (Exception e)
		{
			Logger.df(TAG, 12, "Exception occurred getting BluetoothAdapter: %s", Logger.getStackTraceString(e));
		}

		return bluetoothAdapter;
	}

	/**
	 * Verify whether Bluetooth LE is supported on this device
	 * 
	 * @param context
	 * @return
	 */
	@SuppressLint("NewApi")
	public static boolean isBluetoothLESupported(Context context)
	{
		boolean result = false;

		try
		{
			if (PlatformInfo.isDocoboDevice())
			{
				Method connectBluetoothGattMethod = getMethod(TAG, BluetoothDevice.class, "connectGatt", Context.class, boolean.class, BluetoothGattCallback.class);
				if (connectBluetoothGattMethod != null)
				{
					result = true;
				}
			}
			else if (context.getPackageManager().hasSystemFeature(LocalBluetoothManager.FEATURE_BLUETOOTH_LE))
			{
				result = true;
			}
		}
		catch (NoClassDefFoundError e)
		{
			// This means the bluetoothGattCallback classes are not available on this device.
			// Hence Bluetooth LE is not supported.
			result = false;
		}

		return result;
	}

	/**
	 * Convenience function to access a method.
	 * <p>
	 * Useful when using reflection to access private methods or methods not available in the public API.
	 *
	 * @param logTag - log tag used in the event of an error (Using the SimpleName of the calling class is generally a good practice)
	 * @param classObject - The class to the method is expected to be in.
	 * @param methodName - the method name
	 * @param parameterTypes - Parameter types array.
	 * @return the method defined by the methodName and parameter types or null in case of any failure.
	 *
	 * @see Class#getDeclaredMethod(String, Class...)
	 */
	public static Method getMethod(String logTag, Class<?> classObject, String methodName, Class<?>... parameterTypes)
	{
		Method method = null;
		try
		{
			method = classObject.getDeclaredMethod(methodName, parameterTypes);
		} catch (Exception e)
		{
			Logger.df(logTag, DEBUG_LEVEL, "Exception occurred getting method %s: %s", methodName, Logger.getStackTraceString(e));
		}

		return method;
	}
}
