package nl.dobots.rssifingerprinting;

import android.os.Environment;

import java.text.SimpleDateFormat;

import nl.dobots.bluenet.ble.extended.BleDeviceFilter;

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
 * Created on 2-2-16
 *
 * @author Dominik Egger
 */
public class Config {

	// version number
	public static final int DATABASE_VERSION = 1;
	// filename of the database
	public static final String DATABASE_NAME = "fingerprints.db";

	public static final int SCAN_INTERVAL = 2000; //  second scan intervals
	public static final int SCAN_PAUSE = 0; // no pause

	public static final long MIN_FREE_SPACE = 10 * 1024 * 1024; // 10 MB
	public static final long MAX_BACKUP_FILE_SIZE = 500 * 1024 * 1024; // 500 MB

	public static final boolean LOG_TO_FILE = true;
	public static final boolean WRITE_TO_DB = true;

	public static final String LOG_FILES_DIRECTORY = Environment.getExternalStorageDirectory().getPath() + "/dobots/fingerprinting";
//	public static final BleDeviceFilter BLE_DEVICE_FILTER = BleDeviceFilter.doBeacon;
	public static final BleDeviceFilter BLE_DEVICE_FILTER = BleDeviceFilter.all;

	public static final SimpleDateFormat SDF_TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	public static final SimpleDateFormat SDF_DISPLAY_TIMESTAMP = new SimpleDateFormat("MM-dd HH:mm:ss");
	public static final SimpleDateFormat SDF_FILENAME = new SimpleDateFormat("yyMMdd_HHmmss");

}
