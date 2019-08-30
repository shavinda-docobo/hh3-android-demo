/*
 *  Copyright (C) 2019 Docobo Ltd - All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 */

package com.docobo.bluetooth;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.docobo.device.PlatformInfo;

public abstract class LeScanCallbackInterface implements InvocationHandler
{
	private Object leScanCallback;
	private static Class<?> interfaceLeScanCallback;
	
	public LeScanCallbackInterface()
	{
		if (PlatformInfo.isDocoboDevice() || android.os.Build.VERSION.SDK_INT >= 18)
		{
			try
			{
				// initialise the interfaceLeScanCallback static variable
				if (interfaceLeScanCallback == null)
				{
					getInterface();
				}
				
				if (interfaceLeScanCallback != null)
				{
					this.leScanCallback = Proxy.newProxyInstance(
							interfaceLeScanCallback.getClassLoader(), 
							new Class<?>[] { interfaceLeScanCallback },
							this
							);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public abstract void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		try
		{
			// System.out.println("before method " + method.getName());
			if (method.getName().equals("onLeScan"))
			{
				BluetoothDevice founddev = (BluetoothDevice) args[0];
				int rssi = ((Integer) args[1]).intValue();
				byte[] scanRecord = (byte[]) args[2];
				onLeScan(founddev, rssi, scanRecord);
				return null;
			}
			else if (method.getName().equals("hashCode"))
			{
				return Integer.valueOf(System.identityHashCode(proxy));
			}
			else if (method.getName().equals("toString"))
			{
				return proxy.getClass().getName() + '@' + Integer.toHexString(proxy.hashCode());
			}
			else if (method.getName().equals("equals"))
			{
				return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
			}
			else
			{
				Log.v("LeScanCallbackInterface", "undefined proxy methods " + method.getName());
			}

		} catch (Exception e)
		{
			throw new RuntimeException("unexpected invocation exception: " + e.getMessage());
		} finally
		{
			// System.out.println("after method " + method.getName());
		}
		
		return null;
	}
	
	public Object getLeScanCallback()
	{
		return leScanCallback;
	}
	
	public static Class<?> getInterface()
	{
		if (interfaceLeScanCallback == null)
		{
			try
			{
				interfaceLeScanCallback = Class.forName("android.bluetooth.BluetoothAdapter$LeScanCallback");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		return interfaceLeScanCallback;
	}
}
