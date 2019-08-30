/*
 *  Copyright (C) 2019 Docobo Ltd - All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 */

package com.docobo.bluetooth;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes
{
	private static HashMap<String, String> attributes = new HashMap<String, String>();
	
	public static final String GENERIC_INFORMATION_SERVICE            = "00001800-0000-1000-8000-00805f9b34fb";
	public static final String DEVICE_NAME                            = "00002a00-0000-1000-8000-00805f9b34fb";
	public static final String DEVICE_APPEARANCE                      = "00002a01-0000-1000-8000-00805f9b34fb";
	public static final String DEVICE_PREFERRED_CONNECTION_PARAMETERS = "00002a04-0000-1000-8000-00805f9b34fb";
	
	public static final String DEVICE_INFORMATION_SERVICE    = "0000180a-0000-1000-8000-00805f9b34fb";
	public static final String MANUFACTURE_NAME              = "00002a29-0000-1000-8000-00805f9b34fb";
	public static final String MANUFACTURE_MODEL             = "00002a24-0000-1000-8000-00805f9b34fb";
	public static final String MANUFACTURE_SERIAL_NUMBER     = "00002a25-0000-1000-8000-00805f9b34fb";
	public static final String MANUFACTURE_FIRMWARE_REVISION = "00002a26-0000-1000-8000-00805f9b34fb";
	public static final String MANUFACTURE_HARDWARE_REVISION = "00002a27-0000-1000-8000-00805f9b34fb";
	public static final String MANUFACTURE_SOFTWARE_REVISION = "00002a28-0000-1000-8000-00805f9b34fb";
	public static final String MANUFACTURE_SYSTEM_ID         = "00002a23-0000-1000-8000-00805f9b34fb";
	public static final String MANUFACTURE_DEVICE_ID         = "00002a50-0000-1000-8000-00805f9b34fb";
	
	public static final String HEART_RATE_SERVICE     = "0000180d-0000-1000-8000-00805f9b34fb";
	public static final String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
	
	public static final String NONIN_OXIMETRY_SERVICE     = "46a970e0-0d5f-11e2-8b5e-0002a5d5c51b";
	public static final String NONIN_CONTROL_POINT        = "1447af80-0d60-11e2-88b6-0002a5d5c51b";
	public static final String NONIN_OXIMETRY_MEASUREMENT = "0aad7ea0-0d60-11e2-8e3c-0002a5d5c51b";
	
	public static final String USER_CHARACTERISTIC_CONFIG     = "00002901-0000-1000-8000-00805f9b34fb";
	public static final String CLIENT_CHARACTERISTIC_CONFIG   = "00002902-0000-1000-8000-00805f9b34fb";
	public static final String CLIENT_CHARACTERISTIC_CONFIG_2 = "00000000-0000-0000-0000-000000002902";
	
	public static final String BATTERY_INFORMATION_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";
	public static final String BATTERY_LEVEL_PERCENTAGE    = "00002a19-0000-1000-8000-00805f9b34fb";
	public static final String BATTERY_POWER_STATE         = "00002a1a-0000-1000-8000-00805f9b34fb";
	public static final String BATTERY_LEVEL_STATE         = "00002a1b-0000-1000-8000-00805f9b34fb";
	
	public static final String MARSDEN_SERVICE               = "3a1bc6e0-fb06-11e1-b9c2-0002a5d5c51b";
	public static final String MARSDEN_WEIGHT_CHARACTERISTIC = "cc330a40-fb09-11e1-a84d-0002a5d5c51b";
	
	public static final String TAIDOC_COMMUNICATION_SERVICE        = "00001523-1212-efde-1523-785feabcd123";
	public static final String TAIDOC_COMMUNICATION_CHARACTERISTIC = "00001524-1212-efde-1523-785feabcd123";
	
	public static final String HEALTH_THERMOMETER_SERVICE = "00001809-0000-1000-8000-00805f9b34fb";
	public static final String TEMPERATURE_MEASUREMENT    = "00002a1c-0000-1000-8000-00805f9b34fb";
	
	public static final String GENERIC_ATTRIBUTE_SERVICE     = "00001801-0000-1000-8000-00805f9b34fb";
	public static final String GATT_CHARACTERISTIC_DATE_TIME = "00002a08-0000-1000-8000-00805f9b34fb";
	
	public static final String GATT_SERVICE_BLOOD_PRESSURE                    = "00001810-0000-1000-8000-00805f9b34fb";
	public static final String GATT_CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT = "00002a35-0000-1000-8000-00805f9b34fb";
	
	public static final String GATT_SERVICE_BODY_WEIGHT                    = "0000181d-0000-1000-8000-00805f9b34fb";
	public static final String GATT_CHARACTERISTIC_BODY_WEIGHT_MEASUREMENT = "00002a9d-0000-1000-8000-00805f9b34fb";

	public static final String PULSE_OXIMETER_SERVICE                = "00001822-0000-1000-8000-00805f9b34fb";
	public static final String PULSE_OXIMETRY_SPOT_CHECK_MEASUREMENT = "00002a5e-0000-1000-8000-00805f9b34fb";
	public static final String PULSE_OXIMETRY_CONTINUOUS_MEASUREMENT = "00002a5f-0000-1000-8000-00805f9b34fb";
	public static final String PULSE_OXIMETER_FEATURES               = "00002a60-0000-1000-8000-00805f9b34fb";
	
	public static final String CURRENT_TIME_SERVICE          = "00001805-0000-1000-8000-00805f9b34fb";
	public static final String CURRENT_TIME_CHARACTERISTIC   = "00002a2b-0000-1000-8000-00805f9b34fb";
	public static final String LOCAL_TIME_CHARACTERISTIC     = "00002a0f-0000-1000-8000-00805f9b34fb";
	public static final String REFERENCE_TIME_CHARACTERISTIC = "00002a14-0000-1000-8000-00805f9b34fb";
	
	public static final String RECORD_ACCESS_CONTROL_POINT = "00002a52-0000-1000-8000-00805f9b34fb";

	public static final String SINOCARE_COMMUNICATION_SERVICE = "0000ffb0-0000-1000-8000-00805f9b34fb";
	public static final String SINOCARE_COMMUNICATION_CHARACTERISTIC = "0000ffb2-0000-1000-8000-00805f9b34fb";

	static
	{
		// Generic Service and Characteristics
		attributes.put(GENERIC_INFORMATION_SERVICE, "Generic Access Info");
		attributes.put(DEVICE_NAME, "Device Name");
		attributes.put(DEVICE_APPEARANCE, "Device Appearance");
		attributes.put(DEVICE_PREFERRED_CONNECTION_PARAMETERS, "Device Preferred Connection Parameters");
		attributes.put(GATT_CHARACTERISTIC_DATE_TIME, "GATT Date Time");

		attributes.put(GENERIC_ATTRIBUTE_SERVICE, "Generic Attribute Service");
		
		// Other generic characteristics
		attributes.put(RECORD_ACCESS_CONTROL_POINT, "Record Access Control Point");
		
		// Heart Rate Characteristics.
		attributes.put(HEART_RATE_SERVICE, "Heart Rate Service");
		attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");

		// Device Information Service and supported characteristics
		attributes.put(DEVICE_INFORMATION_SERVICE, "Device Information Service");
		attributes.put(MANUFACTURE_NAME, "Manufacturer Name");
		attributes.put(MANUFACTURE_MODEL, "Manufacturer Model");
		attributes.put(MANUFACTURE_SERIAL_NUMBER, "Manufacturer Serial Number");
		attributes.put(MANUFACTURE_FIRMWARE_REVISION, "Manufacturer Firmware Rev.");
		attributes.put(MANUFACTURE_HARDWARE_REVISION, "Manufacturer Hardware Rev.");
		attributes.put(MANUFACTURE_SOFTWARE_REVISION, "Manufacturer Software Rev.");
		attributes.put(MANUFACTURE_SYSTEM_ID, "Manufacturer System ID");
		attributes.put(MANUFACTURE_DEVICE_ID, "Manufacturer PnP ID");

		// Nonin oximetry service and supported characteristics
		attributes.put(NONIN_OXIMETRY_SERVICE, "Nonin Oximetry Service");
		attributes.put(NONIN_CONTROL_POINT, "Nonin Control Point");
		attributes.put(NONIN_OXIMETRY_MEASUREMENT, "Nonin Oximetry Measurement");

		// Battery information service
		attributes.put(BATTERY_INFORMATION_SERVICE, "Battery Information Service");
		attributes.put(BATTERY_LEVEL_PERCENTAGE, "Battery Level Percentage");
		attributes.put(BATTERY_POWER_STATE, "Battery Power State");
		attributes.put(BATTERY_LEVEL_STATE, "Battery Level & State"); // Provides both level and state

		// Marsden service
		attributes.put(MARSDEN_SERVICE, "Marsden Scale Service");
		attributes.put(MARSDEN_WEIGHT_CHARACTERISTIC, "Marsden Weight Measurement");

		// TaiDoc service
		attributes.put(TAIDOC_COMMUNICATION_SERVICE, "TaiDoc Service");
		attributes.put(TAIDOC_COMMUNICATION_CHARACTERISTIC, "TaiDoc Communication Protocol");

		// Health Thermometer service
		attributes.put(HEALTH_THERMOMETER_SERVICE, "Health Thermometer");
		attributes.put(TEMPERATURE_MEASUREMENT, "Temperature Measurement");

		//
		// Blood pressure
		//
		attributes.put(GATT_SERVICE_BLOOD_PRESSURE, "GATT Blood Pressure Service");
		attributes.put(GATT_CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT, "GATT Blood Pressure Measurement");

		//
		// Body weight
		//
		attributes.put(GATT_SERVICE_BODY_WEIGHT, "GATT Body Weight Service");
		attributes.put(GATT_CHARACTERISTIC_BODY_WEIGHT_MEASUREMENT, "GATT Body Weight Measurement");

		/*
		 * Current time service
		 */
		attributes.put(CURRENT_TIME_SERVICE, "Current Time Service");
		attributes.put(CURRENT_TIME_CHARACTERISTIC, "Current Time");
		attributes.put(LOCAL_TIME_CHARACTERISTIC, "Local Time");
		attributes.put(REFERENCE_TIME_CHARACTERISTIC, "Reference Time");
		
		/*
		 * Pulse Oximetry service
		 */
		attributes.put(PULSE_OXIMETER_SERVICE, "Pulse Oximetry Service");
		attributes.put(PULSE_OXIMETRY_SPOT_CHECK_MEASUREMENT, "Pulse Oximetry Spot-Check");
		attributes.put(PULSE_OXIMETRY_CONTINUOUS_MEASUREMENT, "Pulse Oximetry Continuous");
		attributes.put(PULSE_OXIMETER_FEATURES, "PLX Features");

		/*
		 * Sinocare Service
		 */
		attributes.put(SINOCARE_COMMUNICATION_SERVICE, "Sinocare Service");
		attributes.put(SINOCARE_COMMUNICATION_CHARACTERISTIC, "Sinocare Communication Protocol");

	}

	public static String lookup(String uuid, String defaultName)
	{
		String name = attributes.get(uuid);
		return name == null ? defaultName : name;
	}
}
