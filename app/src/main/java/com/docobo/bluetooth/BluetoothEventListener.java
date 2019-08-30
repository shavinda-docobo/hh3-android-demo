/*
 *  Copyright (C) 2019 Docobo Ltd - All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 */

package com.docobo.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public abstract class BluetoothEventListener
{
	/**
	 * Callback triggered when the bluetooth adapter state changes to enabled or disabled.
	 * <p>
	 * Note: This callback will not be invoked for intermediate states.
	 * @param enable - true when the state is {@link BluetoothAdapter#STATE_ON}, false when the state changes to {@link BluetoothAdapter#STATE_OFF}
	 */
	public void onBluetoothStateChanged(boolean enable)
	{	
	}
	
	/**
	 * Callback triggered when bluetooth discovery state changes.
	 * @param searching - true when discovery is started, false when discovery finishes.
	 */
	public void onDiscoveryStateChanged(boolean searching)
	{	
	}
	
	/**
	 * Callback triggered when the bluetooth scan mode changes.
	 * 
	 * @param scanMode - current scan mode :<br>
	 * 		{@link BluetoothAdapter#SCAN_MODE_NONE} - Bluetooth adapter is off (No Power consumption)
	 * 		{@link BluetoothAdapter#SCAN_MODE_CONNECTABLE} - Remote devices can connect (Low power state)
	 * 		{@link BluetoothAdapter#SCAN_MODE_CONNECTABLE_DISCOVERABLE} - Remote devices can discover and connect (High power state)
	 */
	public void onScanModeChanged(int scanMode)
	{	
	}
	
	/**
	 * Callback triggered when a device is found
	 * 
	 * @param device
	 * @param signalStrength
	 * @param bluetoothLEDevice
	 */
	public void onDeviceFound(BluetoothDevice device, int signalStrength, boolean bluetoothLEDevice)
	{	
	}

	/**
	 * Callback trigger when device information is updated.
	 * 
	 * @param device - Device information was updated for.
	 * @param updateType - information type that was updated.
	 */
	public void onDeviceInfoChanged(BluetoothDevice device, int updateType)
	{	
	}

	/**
	 * Callback when a device has disappeared.
	 * <br>
	 * <i>Note: Not quite sure when this would get called!</i>
	 * @param device
	 */
	public void onDeviceDisappeared(BluetoothDevice device)
	{
	}
	
	/**
	 * Callback invoked when the device connection state has changed. 
	 * @param device
	 * @param connectionState - {@link LocalBluetoothManager#STATE_CONNECTED}, 
	 * 					{@link LocalBluetoothManager#STATE_DISCONNECTING or {@link LocalBluetoothManager#STATE_DISCONNECTED}
	 */
	public void onDeviceConnectionStateChanged(BluetoothDevice device, int connectionState)
	{
	}
	
	/**
	 * Callback invoked when the bond state has changed.
	 * 
	 * @param device
	 * @param prevBondingState
	 * @param newBondingState
	 */
	public void onBondingStateChanged(BluetoothDevice device, int prevBondingState, int newBondingState)
	{
	}

	/**
	 * Callback invoked when the pairing is requested.
	 * 
	 * @param device
	 * @param requestedOrCancelled
	 * @param pairingVariant
	 * @return true to indicate the receiver is processing this pairing request, false otherwise.
	 */
	public boolean onPairingEventOccurred(BluetoothDevice device, boolean requestedOrCancelled, int pairingVariant)
	{
		return false;
	}
	
	/**
	 * Callback invoked when the Bluetooth GATT service state changes.
	 * 
	 * @param bluetoothGattReady
	 */
	public void onGattServiceStateChanged(boolean bluetoothGattReady)
	{
	}
}
