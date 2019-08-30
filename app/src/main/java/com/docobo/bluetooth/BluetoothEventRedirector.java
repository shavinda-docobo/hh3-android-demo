/*
 *  Copyright (C) 2019 Docobo Ltd - All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 */

package com.docobo.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.docobo.device.PlatformInfo;
import com.docobo.logger.Logger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

public class BluetoothEventRedirector
{
	private static String TAG = "BluetoothEventDetector";
	private final int DEBUG_LEVEL = 10; 
	
	private class BluetoothBroadcastReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			
			if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
			{
				Logger.df(TAG, 15,"Bluetooth2.0 discorvery started");
				localBluetoothManager.dispatchBluetoothScanningStateChanged(true);
			}
			else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
			{
				Logger.df(TAG, 15,"Bluetooth2.0 discorvery finished");
				localBluetoothManager.dispatchBluetoothScanningStateChanged(false);
			}
			else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
			{
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.ERROR);
				Logger.df(TAG, 15,"Bluetooth State Changed [%s --> %s]", 
						getBluetothAdapterStateString(previousState), 
						getBluetothAdapterStateString(state)
						);
				
				localBluetoothManager.setBluetoothStateInt(state);
				
				if (PlatformInfo.isDocoboDevice() == false)
				{
					// The GATT ready broadcast is not usually sent on non-health hub devices.
					// But in order to make the application layer simpler a GATT ready and not ready events are simulated if the device supported BLE.
					if (localBluetoothManager.isBluetooth40Supported())
					{
						if (state == BluetoothAdapter.STATE_ON)
						{
							dispatchGATTServiceStateChangedEvent(true);
						}
						else if (state == BluetoothAdapter.STATE_TURNING_OFF) // Report GATT not ready on when turning off rather than turned Off.
						{
							dispatchGATTServiceStateChangedEvent(true);
						}
					}
				}
			}
			else if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED))
			{
				int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
				int previousMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1);
	
				Logger.df(TAG, DEBUG_LEVEL,"Bluetooth Scan Mode Changed [%s --> %s]", 
						getBluetothAdapterStateString(previousMode), 
						getBluetothAdapterStateString(mode)
						);
				
				dispatchScanModeChangedEvent(mode);
			}
			else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED))
			{
				Logger.df(TAG, DEBUG_LEVEL, "Connected: %s [%s]", device.getName(), device.getAddress());
	
				dispatchDeviceConnectionStateChanged(device, LocalBluetoothManager.STATE_CONNECTED);
			}
			else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED))
			{
				Logger.df(TAG, DEBUG_LEVEL, "Disconnect Request: %s [%s]", device.getName(), device.getAddress());
	
				dispatchDeviceConnectionStateChanged(device, LocalBluetoothManager.STATE_DISCONNECTING);
			}
			else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))
			{
				Logger.df(TAG, DEBUG_LEVEL, "Disconnected: %s [%s]", device.getName(), device.getAddress());
	
				dispatchDeviceConnectionStateChanged(device, LocalBluetoothManager.STATE_DISCONNECTED);
			}
			else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
			{
				int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
				int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
				int reason = intent.getIntExtra(LocalBluetoothManager.EXTRA_BLUETOOTH_DEVICE_REASON, BluetoothDevice.ERROR);
				
				Logger.df(TAG, DEBUG_LEVEL, "Bond State Changed for %1$s (%2$s) [Reason: %5$d]: %3$s --> %4$s", 
						device.getName(), 
						device.getAddress(), 
						getBluetothDeviceStateString(previousBondState), 
						getBluetothDeviceStateString(bondState), 
						reason);
				
				dispatchBondingStateChanged(device, previousBondState, bondState);
			}
			else if (action.equals(BluetoothDevice.ACTION_FOUND))
			{
				String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
				if (name == null)
				{
					name = device.getName();
				}
				short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
				BluetoothClass btClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
				
				Logger.df(TAG, DEBUG_LEVEL, "Device Found: %s [%s] (Name: %s, RSSI: %d, Class: %s)", 
						device.getName(), device.getAddress(), name, rssi, btClass);
				
				dispatchDeviceFoundEvent(device, rssi, false);
			}
			else if (action.equals(BluetoothDevice.ACTION_NAME_CHANGED))
			{
				String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
				if (name == null)
				{
					name = device.getName();
				}
	
				Logger.df(TAG, DEBUG_LEVEL, "Device Name Update: %s [%s] (Name: %s)", device.getName(), device.getAddress(), name);
				
				dispatchDeviceInfoUpdatedEvent(device, UPDATE_TYPE_NAME);
			}
			else if (action.equals(BluetoothDevice.ACTION_CLASS_CHANGED))
			{
				BluetoothClass bluetoothClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
				Logger.df(TAG, DEBUG_LEVEL, "Device Class Update: %s[%s] (Class: %s)", device.getName(), device.getAddress(), bluetoothClass);
				
				dispatchDeviceInfoUpdatedEvent(device, UPDATE_TYPE_BLUETOOTH_DEVICE_CLASS);
			}
			else if (action.equals(LocalBluetoothManager.ACTION_BLUETOOTH_DEVICE_DISAPPEARED))
			{
				Logger.df(TAG, DEBUG_LEVEL, "Device Disappeared: %s[%s]", device.getName(), device.getAddress());
				
				dispatchDeviceDisappeared(device);
			}
			else if (action.equals(LocalBluetoothManager.ACTION_BLUETOOTH_DEVICE_PAIRING_CANCEL))
			{
				if (device != null)
				{
					Logger.df(TAG, DEBUG_LEVEL, "Device Pairing Cancel: %s[%s]", device.getName(), device.getAddress());
				}
				else
				{
					Logger.df(TAG, DEBUG_LEVEL, "Device Pairing Cancel: NULL");
				}
				
				dispatchPairingEvent(device, false, -1);
			}
			else if (action.equals(LocalBluetoothManager.ACTION_CONNECTION_STATE_CHANGED))
			{
				int connState = intent.getIntExtra(LocalBluetoothManager.EXTRA_CONNECTION_STATE, BluetoothDevice.ERROR);
				int previousConnState = intent.getIntExtra(LocalBluetoothManager.EXTRA_PREVIOUS_CONNECTION_STATE, BluetoothDevice.ERROR);
				Logger.df(TAG, DEBUG_LEVEL, "BluetoothProfile Connection State: %s[%s] [%s --> %s]", 
						device.getName(), 
						device.getAddress(), 
						getBluetothAdapterStateString(connState), 
						getBluetothAdapterStateString(previousConnState)
						);
			}
			else if (action.equals(LocalBluetoothManager.ACTION_BLUETOOTH_DEVICE_PAIRING_REQUEST))
			{
				int pairingVariant = intent.getIntExtra(LocalBluetoothManager.EXTRA_BLUETOOTH_DEVICE_PAIRING_VARIANT, BluetoothDevice.ERROR);
				Logger.df(TAG, DEBUG_LEVEL, "Device Pairing Requested: %s[%s] (Variant: %d)", device.getName(), device.getAddress(), pairingVariant);
				
				if (dispatchPairingEvent(device, true, pairingVariant))
				{
					Logger.df(TAG, DEBUG_LEVEL, "Pairing request has been processed by an event listener");
					if (isOrderedBroadcast())
					{
						Logger.df(TAG, DEBUG_LEVEL, "Aborting pairing request broadcast");
						try
						{
							abortBroadcast();
						}
						catch (Exception e)
						{
							Logger.ex(TAG, "Exception aborting pairing request broadcast", e);
						}
					}
				}
			}
			else if (action.equals(LocalBluetoothManager.ACTION_GATT_CONNECTED))
			{
				Logger.df(TAG, DEBUG_LEVEL, "GATT Service connected");
				dispatchGATTServiceStateChangedEvent(true);
			}
			else if (action.equals(LocalBluetoothManager.ACTION_GATT_DISCONNECTED))
			{
				Logger.df(TAG, DEBUG_LEVEL, "GATT Service disconnected");
				dispatchGATTServiceStateChangedEvent(false);
			}
			else if (action.equals(LocalBluetoothManager.ACTION_GATT_NOT_SUPPORTED))
			{
				Logger.df(TAG, DEBUG_LEVEL, "GATT Service not supported");
				dispatchGATTServiceStateChangedEvent(false);
			}
		}
	}
	
	private class BluetoothLeScanCallback extends LeScanCallbackInterface
	{
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
		{
			String scanRecordString = "";
			if (scanRecord != null && scanRecord.length > 0)
			{
				final StringBuilder stringBuilder = new StringBuilder(scanRecord.length);
				for (byte byteChar : scanRecord)
					stringBuilder.append(String.format("%02X ", byteChar));
				
				scanRecordString = stringBuilder.toString();
			}
			
			Logger.df(TAG, DEBUG_LEVEL, "BluetoothLE Device Found: %s [%s] (RSSI: %d, Scan Record: %s)", 
					device.getName(), device.getAddress(), rssi, scanRecordString);

			//
			// Temp - check type
			//
			Logger.d(TAG, "---- getType returned " + device.getType());
			
			dispatchDeviceFoundEvent(device, rssi, true);
		}
	}
	
	public static final int UPDATE_TYPE_NONE                   = 0;
	public static final int UPDATE_TYPE_NAME                   = 1;
	public static final int UPDATE_TYPE_BLUETOOTH_DEVICE_CLASS = 2;
	
	private final LocalBluetoothManager localBluetoothManager;
	private BluetoothBroadcastReceiver broadcastReceiver = null;
	private BluetoothLeScanCallback bluetoothScanCallBack = null;
	private final ArrayList<WeakReference<BluetoothEventListener>> bluetoothEventListeners;
	
	public BluetoothEventRedirector(LocalBluetoothManager localBluetoothManager)
	{
		if (localBluetoothManager == null)
		{
			throw new IllegalArgumentException("LocalBluetoothManager cannot be null");
		}
		
		this.localBluetoothManager = localBluetoothManager;
		this.bluetoothEventListeners = localBluetoothManager.getBluetoothEventListeners();
	}
	
	synchronized void startRedirector()
	{
		if (this.broadcastReceiver == null)
		{
			IntentFilter filter = new IntentFilter();
			
			// Bluetooth hardware state broadcast (on/off and discovery state)
			filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
			
			// Device discovery broadcast
			filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
			filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			filter.addAction(BluetoothDevice.ACTION_FOUND);
			filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
			filter.addAction(LocalBluetoothManager.ACTION_BLUETOOTH_DEVICE_DISAPPEARED);
			
			// Device connection state changed broadcasts
			filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
			filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
			filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
			
			// Pairing state changed broadcast
			filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
			filter.addAction(LocalBluetoothManager.ACTION_BLUETOOTH_DEVICE_PAIRING_REQUEST);
			filter.addAction(LocalBluetoothManager.ACTION_BLUETOOTH_DEVICE_PAIRING_CANCEL);
			
			filter.addAction(BluetoothDevice.ACTION_CLASS_CHANGED);
			filter.addAction(LocalBluetoothManager.ACTION_CONNECTION_STATE_CHANGED);
			
			// Bluetooth GATT State changes
			filter.addAction(LocalBluetoothManager.ACTION_GATT_CONNECTED);
			filter.addAction(LocalBluetoothManager.ACTION_GATT_DISCONNECTED);
			filter.addAction(LocalBluetoothManager.ACTION_GATT_NOT_SUPPORTED);
			
			this.broadcastReceiver = new BluetoothBroadcastReceiver();
			this.localBluetoothManager.getContext().registerReceiver(this.broadcastReceiver, filter);
		}
		
		if (this.bluetoothScanCallBack == null && this.localBluetoothManager.isBluetooth40Supported())
		{
			this.bluetoothScanCallBack = new BluetoothLeScanCallback();
		}
	}
	
	synchronized void stopRedirector()
	{
		if (this.broadcastReceiver != null)
		{
			this.localBluetoothManager.getContext().unregisterReceiver(this.broadcastReceiver);
			this.broadcastReceiver = null;
		}
		
		this.bluetoothScanCallBack = null;
	}
	
	public BluetoothLeScanCallback getBluetoothScanCallBack()
	{
		return bluetoothScanCallBack;
	}
	
	/*
	 * Event dispatch methods
	 */
	private void dispatchScanModeChangedEvent(int scanMode)
	{
		synchronized (this.bluetoothEventListeners)
		{
			for (WeakReference<BluetoothEventListener> callback : this.bluetoothEventListeners)
			{
				if (callback.get() != null)
					callback.get().onScanModeChanged(scanMode);
			}
		}
	}
	
	private void dispatchDeviceFoundEvent(BluetoothDevice device, int signalStrength, boolean bluetoothLEDevice)
	{
		synchronized (this.bluetoothEventListeners)
		{
			for (WeakReference<BluetoothEventListener> callback : this.bluetoothEventListeners)
			{
				if (callback.get() != null)
					callback.get().onDeviceFound(device, signalStrength, bluetoothLEDevice);
			}
		}
	}
	
	private void dispatchDeviceInfoUpdatedEvent(BluetoothDevice device, int updateType)
	{
		synchronized (this.bluetoothEventListeners)
		{
			for (WeakReference<BluetoothEventListener> callback : this.bluetoothEventListeners)
			{
				if (callback.get() != null)
					callback.get().onDeviceInfoChanged(device, updateType);
			}
		}
	}
	
	private void dispatchDeviceDisappeared(BluetoothDevice device)
	{
		synchronized (this.bluetoothEventListeners)
		{
			for (WeakReference<BluetoothEventListener> callback : this.bluetoothEventListeners)
			{
				if (callback.get() != null)
					callback.get().onDeviceDisappeared(device);
			}
		}
	}
	
	private void dispatchDeviceConnectionStateChanged(BluetoothDevice device, int connectionState)
	{
		synchronized (this.bluetoothEventListeners)
		{
			for (WeakReference<BluetoothEventListener> callback : this.bluetoothEventListeners)
			{
				if (callback.get() != null)
					callback.get().onDeviceConnectionStateChanged(device, connectionState);
			}
		}
	}
	
	private void dispatchBondingStateChanged(BluetoothDevice device, int previousBondState, int newBondingState)
	{
		synchronized (this.bluetoothEventListeners)
		{
			for (WeakReference<BluetoothEventListener> callback : this.bluetoothEventListeners)
			{
				if (callback.get() != null)
					callback.get().onBondingStateChanged(device, previousBondState, newBondingState);
			}
		}
	}
	
	private boolean dispatchPairingEvent(BluetoothDevice device, boolean requestedOrCancelled, int pairingVariant)
	{
		boolean processed = false;
		synchronized (this.bluetoothEventListeners)
		{
			for (WeakReference<BluetoothEventListener> callback : this.bluetoothEventListeners)
			{
				if (callback.get() != null && callback.get().onPairingEventOccurred(device, requestedOrCancelled, pairingVariant))
				{
					// If the pairing request has been processed, stop firing events 
					processed = true;
					break;
				}
			}
		}
		return processed;
	}
	
	private void dispatchGATTServiceStateChangedEvent(boolean bluetoothGattReady)
	{
		synchronized (this.bluetoothEventListeners)
		{
			for (WeakReference<BluetoothEventListener> callback : this.bluetoothEventListeners)
			{
				if (callback.get() != null)
					callback.get().onGattServiceStateChanged(bluetoothGattReady);
			}
		}
	}
	
	private String getBluetothAdapterStateString(int bluetoothState)
	{
		String result = "";
		
		switch (bluetoothState)
		{
			case LocalBluetoothManager.STATE_DISCONNECTED:
				result = "Disconnected";
				break;
			case LocalBluetoothManager.STATE_CONNECTING:
				result = "Connecting";
				break;
			case LocalBluetoothManager.STATE_CONNECTED:
				result = "Connected";
				break;
			case LocalBluetoothManager.STATE_DISCONNECTING:
				result = "Disconnecting";
				break;
			case BluetoothAdapter.STATE_OFF:
				result = "OFF";
				break;
			case BluetoothAdapter.STATE_ON:
				result = "ON";
				break;
			case BluetoothAdapter.STATE_TURNING_OFF:
				result = "Turning Off";
				break;
			case BluetoothAdapter.STATE_TURNING_ON:
				result = "Turning On";
				break;
			case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
				result = "Connectable";
				break;
			case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
				result = "Connectable/Discoverable";
				break;
			case BluetoothAdapter.SCAN_MODE_NONE:
				result = "NONE";
				break;
			default:
				result = "Unknown";
				break;
		}
		return String.format(Locale.US, "%s [%d]", result, bluetoothState);
	}
	
	private String getBluetothDeviceStateString(int bluetoothDeviceState)
	{
		String result = "";
		
		switch (bluetoothDeviceState)
		{
			case BluetoothDevice.BOND_BONDED:
				result = "Bonded";
				break;
			case BluetoothDevice.BOND_BONDING:
				result = "Bonding";
				break;
			case BluetoothDevice.BOND_NONE:
				result = "NONE";
				break;
			default:
				result = "Unknown";
				break;
		}
		
		return String.format(Locale.US, "%s [%d]", result, bluetoothDeviceState);
	}
}
