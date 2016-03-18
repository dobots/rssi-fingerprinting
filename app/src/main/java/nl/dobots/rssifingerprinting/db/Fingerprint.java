package nl.dobots.rssifingerprinting.db;

import java.util.HashMap;

import nl.dobots.bluenet.ble.extended.structs.BleDevice;
import nl.dobots.bluenet.ble.extended.structs.BleDeviceList;

/**
 * Copyright (c) 2016 Dominik Egger <dominik@dobots.nl>. All rights reserved.
 * <p/>
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3, as
 * published by the Free Software Foundation.
 * <p/>
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * <p/>
 * Created on 19-2-16
 *
 * @author Dominik Egger
 */
public class Fingerprint {

	private static int FINGERPRINT_COUNTER = 0;

	private String _location;
//	private BleDeviceList _devices = new BleDeviceList();
	private HashMap<String, BleDevice> _devices = new HashMap<>();
	private int _fingerprintId;
	private static int counter;

	public Fingerprint() {
		_fingerprintId = FINGERPRINT_COUNTER++;
	}

	public Fingerprint(String location, int fingerprintId) {
		_location = location;
		_fingerprintId = fingerprintId;
	}

	public static void resetCounter() {
		FINGERPRINT_COUNTER = 0;
	}

	public static void setCounter(int counter) {
		FINGERPRINT_COUNTER = counter;
	}

	public BleDeviceList getDevices() {
		return new BleDeviceList(_devices.values());
	}

	public String getLocation() {
		return _location;
	}

	public void setLocation(String location) {
		_location = location;
	}

	public void setDeviceList(BleDeviceList deviceList) {
		_devices.clear();
		for (BleDevice dev : deviceList) {
			_devices.put(dev.getAddress(), dev);
		}
	}

	public int getFingerprintId() {
		return _fingerprintId;
	}

	public void addDevice(String deviceAddress, int deviceRssi) {
		_devices.put(deviceAddress, new BleDevice(deviceAddress, "", deviceRssi));
	}

	public BleDevice getDevice(String address) {
		return _devices.get(address);
	}
}
