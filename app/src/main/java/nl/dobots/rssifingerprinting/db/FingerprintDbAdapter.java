package nl.dobots.rssifingerprinting.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import nl.dobots.bluenet.ble.extended.structs.BleDevice;

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
public class FingerprintDbAdapter {

	///////////////////////////////////////////////////////////////////////////////////////////
	/// Variables
	///////////////////////////////////////////////////////////////////////////////////////////

	private static final String TAG = FingerprintDbAdapter.class.getCanonicalName();

	// key names of the database fields
	public static final String KEY_LOCATION = "location";
	public static final String KEY_FINGERPRINT_ID = "fingerprintId";
	public static final String KEY_DEVICE_ADDRESS = "address";
	public static final String KEY_DEVICE_RSSI = "rssi";
	public static final String KEY_ROWID = "_id";

	// table name
	public static final String TABLE_NAME = "fingerprints";

	// database helper to manage database creation and version management.
	private DatabaseHelper mDbHelper;

	// database object to read and write database
	private SQLiteDatabase mDb;

	// define query used to create the database
	public static final String DATABASE_CREATE =
			"create table " + TABLE_NAME + " (" +
					KEY_ROWID + " integer primary key autoincrement, " +
					KEY_LOCATION + " text not null," +
					KEY_FINGERPRINT_ID + " integer not null," +
					KEY_DEVICE_ADDRESS + " text not null," +
					KEY_DEVICE_RSSI + " integer not null" +
					" )";

	// application context
	private final Context mContext;

	///////////////////////////////////////////////////////////////////////////////////////////
	/// Code
	///////////////////////////////////////////////////////////////////////////////////////////

	// helper class to manage database creation and version management, see SQLiteOpenHelper
	private static class DatabaseHelper extends SQLiteOpenHelper {

		// default constructor
		DatabaseHelper(Context context, String dbName, int dbVersion) {
			super(context, dbName, null, dbVersion);
		}

		// called when database should be created
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		// called if version changed and database needs to be upgraded
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " +
					newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS notes");
			onCreate(db);
		}

	}

	// default constructor, assigns context and initializes date formats
	public FingerprintDbAdapter(Context context) {
		mContext = context;
//		context.deleteDatabase(DATABASE_NAME);
	}

	/**
	 * Open the database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 *
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException if the database could be neither opened or created
	 */
	public FingerprintDbAdapter open(String dbName, int dbVersion) throws SQLException {
		mDbHelper = new DatabaseHelper(mContext, dbName, dbVersion);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	/**
	 * Close the database
	 */
	public void close() {
		mDbHelper.close();
	}

	public void clear() {
		mDb.delete(TABLE_NAME, null, null);
	}

//	public boolean saveAll(LocationsList list) {
//		clear();
//
//		boolean success = true;
//		for (Location location : list) {
//			success &= addLocation(location);
//		}
//		return success;
//	}

	//	public void loadAll() {
//	public void loadAll(LocationsList list) {
//
//		HashMap<String, Location> hashMap = new HashMap<>();
//
////		LocationsList result = new ArrayList<>();
//		Cursor cursor = fetchAllEntries();
//
//		Location location = null;
////		String lastLocationStr = "";
//
//		// as long as there are entries
//		while (!cursor.isAfterLast()) {
//
//			String locationStr = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOCATION_NAME));
//
////			if (!locationStr.matches(lastLocationStr)) {
////				location = new Location(locationStr);
////				list.add(location);
////			}
//			if (hashMap.containsKey(locationStr)) {
//				location = hashMap.get(locationStr);
//			} else {
//				location = new Location(locationStr);
//				hashMap.put(locationStr, location);
//			}
//
//			String address = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEVICE_ADDRESS));
//			String name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEVICE_NAME));
//
//			// dummy value -1 for rssi, because we don't need the rssi for the locations
//			location.addBeacon(new BleDevice(address, name, -1));
//
//			cursor.moveToNext();
//		}
//
//		list.addAll(hashMap.values());
////		return result;
//	}

	public boolean containsLocation(String location) {
		return DatabaseUtils.queryNumEntries(mDb, TABLE_NAME, KEY_LOCATION + "=\"" + location + "\"") > 0;
//		Cursor mCursor = mDb.query(TABLE_NAME, new String[] {KEY_LOCATION},
//				KEY_LOCATION + "=\"" + location + "\"", null, null, null, null);
//		return mCursor != null;
	}

	public boolean deleteLocation(String location) {
		return mDb.delete(TABLE_NAME, KEY_LOCATION + "=\"" + location + "\"", null) > 0;
	}

	public boolean addFingerprint(Fingerprint fingerprint) {
		ContentValues values = new ContentValues();

		for (BleDevice device : fingerprint.getDevices()) {
			values.put(KEY_LOCATION, fingerprint.getLocation());
			values.put(KEY_FINGERPRINT_ID, fingerprint.getFingerprintId());
			values.put(KEY_DEVICE_ADDRESS, device.getAddress());
			values.put(KEY_DEVICE_RSSI, device.getAverageRssi());

			if (replaceEntry(values) == -1) {
				Log.e(TAG, "failed to add db entry");
				return false;
			}
		}

		return true;
	}

	public ArrayList<Fingerprint> getFingerprintsForLocation(String location) {
		HashMap<Integer, Fingerprint> map = new HashMap<>();

		Cursor cursor = fetchAllEntries(location);

		while (!cursor.isAfterLast()) {
			int fingerprintId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FINGERPRINT_ID));

			Fingerprint fingerprint;
			if (map.containsKey(fingerprintId)) {
				fingerprint = map.get(fingerprintId);
			} else {
				fingerprint = new Fingerprint(location, fingerprintId);
				map.put(fingerprintId, fingerprint);
			}

			String deviceAddress = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEVICE_ADDRESS));
			int deviceRssi = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DEVICE_RSSI));

			fingerprint.addDevice(deviceAddress, deviceRssi);

		}

		return new ArrayList<>(map.values());
	}

	public int getLastFingerprintId(String location) {
		Cursor cursor = mDb.rawQuery("select * from " + TABLE_NAME + " where " + KEY_LOCATION + "=\"" + location + "\" order by " + KEY_FINGERPRINT_ID + " DESC limit 1", null);
		if (cursor != null) {
			cursor.moveToFirst();
			return cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FINGERPRINT_ID));
		}
		return 0;
	}


	public ArrayList<LocationWithFingerprints> getAllLocations() {

		HashMap<String, LocationWithFingerprints> map = new HashMap<>();

		Cursor cursor = fetchAllEntries();

		// as long as there are entries
		while (!cursor.isAfterLast()) {

			String locationName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOCATION));
			int fingerprintId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FINGERPRINT_ID));
			String deviceAddress = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEVICE_ADDRESS));

			int deviceRssi = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DEVICE_RSSI));

			LocationWithFingerprints location;
			if (map.containsKey(locationName)) {
				location = map.get(locationName);
			} else {
				location = new LocationWithFingerprints(locationName);
				map.put(locationName, location);
			}
			location.addFingerprint(fingerprintId, deviceAddress, deviceRssi);

			cursor.moveToNext();
		}

		return new ArrayList<>(map.values());
	}


	private long createEntry(String location, int fingerprintId, String deviceAddress, int deviceRssi) {
		ContentValues values = new ContentValues();

		values.put(KEY_LOCATION, location);
		values.put(KEY_FINGERPRINT_ID, fingerprintId);
		values.put(KEY_DEVICE_ADDRESS, deviceAddress);
		values.put(KEY_DEVICE_RSSI, deviceRssi);

		return replaceEntry(values);
	}

	public long createEntry(ContentValues values) {
		return mDb.insert(TABLE_NAME, null, values);
	}

	public long replaceEntry(ContentValues values) {
		return mDb.replace(TABLE_NAME, null, values);
	}

	/**
	 * Update existing entry. Return true if entry was updated
	 * successfully
	 *
	 * @param id the row id of the entry to be updated
	 * @return true if updated successfully, false otherwise
	 */
	public boolean updateEntry(long id, String location, int fingerprintId, String address, int rssi) {
		ContentValues values = new ContentValues();

		values.put(KEY_LOCATION, location);
		values.put(KEY_FINGERPRINT_ID, fingerprintId);
		values.put(KEY_DEVICE_ADDRESS, address);
		values.put(KEY_DEVICE_RSSI, rssi);

		int num = mDb.update(TABLE_NAME, values, "_id " + "=" + id, null);
		return num == 1;
	}

	/**
	 * Delete the entry with the given rowId
	 *
	 * @param rowId id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteEntry(long rowId) {
		return mDb.delete(TABLE_NAME, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Fetch all entries in the database
	 *
	 * @return cursor to access the entries
	 */
	public Cursor fetchAllEntries() {
		Cursor mCursor = mDb.query(TABLE_NAME, new String[] {KEY_ROWID, KEY_LOCATION, KEY_FINGERPRINT_ID, KEY_DEVICE_ADDRESS, KEY_DEVICE_RSSI},
				null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	/**
	 * Fetch all entries in the database
	 *
	 * @return cursor to access the entries
	 */
	public Cursor fetchAllEntries(String location) {
		Cursor mCursor = mDb.query(TABLE_NAME, new String[] {KEY_ROWID, KEY_LOCATION, KEY_FINGERPRINT_ID, KEY_DEVICE_ADDRESS, KEY_DEVICE_RSSI},
				KEY_LOCATION + "=\"" + location + "\"", null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	/**
	 * Fetch entry defined by row id
	 *
	 * @param rowId the id of the entry which should be returned
	 * @return cursor to access the entry
	 */
	public Cursor fetchEntry(long rowId) {
		Cursor mCursor = mDb.query(TABLE_NAME, new String[] {KEY_ROWID, KEY_LOCATION, KEY_FINGERPRINT_ID, KEY_DEVICE_ADDRESS, KEY_DEVICE_RSSI},
				KEY_ROWID + "=" + rowId, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public boolean exportDB(String fileName) {

		File exportFile = new File(fileName);
		File directory = exportFile.getParentFile();

		if (!directory.exists()) {
			Log.i(TAG, "creating export directory");
			directory.mkdirs();
		}

		DataOutputStream dos;
		try {
			dos = new DataOutputStream(new FileOutputStream(exportFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		try {
//			dos.writeChars(String.format("%s,%s,%s,%s\n", KEY_ROWID, KEY_LOCATION_NAME, KEY_DEVICE_NAME, KEY_DEVICE_ADDRESS));
			dos.write(String.format("%s,%s,%s,%s\n", KEY_LOCATION, KEY_FINGERPRINT_ID, KEY_DEVICE_ADDRESS, KEY_DEVICE_RSSI).getBytes());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		Cursor cursor = fetchAllEntries();

		// as long as there are entries
		while (!cursor.isAfterLast()) {

			String location = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOCATION));
			int fingerprintId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FINGERPRINT_ID));
			String deviceAddress = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEVICE_ADDRESS));
			int deviceRssi = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DEVICE_RSSI));
			int rowId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ROWID));

			try {
//				dos.writeChars(String.format("%d,%s,%s,%s\n", rowId, location, deviceRssi, deviceAddress));
				dos.write(String.format("%s,%d,%s,%d\n", location, fingerprintId, deviceAddress, deviceRssi).getBytes());
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}

			cursor.moveToNext();
		}

		try {
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean importDB(String fileName) {
		Log.i(TAG, "importing db from " + fileName);

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		try {
			String line = reader.readLine(); // skip first line (header information)
			while ((line = reader.readLine()) != null) {
				if (!line.equals("")) {
					String[] data = line.split(",");
					String roomName = data[0];
					int fingerprintId = Integer.valueOf(data[1]);
					String deviceAddress = data[2];
					int deviceRssi = Integer.valueOf(data[3]);
					createEntry(roomName, fingerprintId, deviceAddress, deviceRssi);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return  false;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	public void filterDB() {

		ArrayList<Integer> deleteRows = new ArrayList<>();

		Cursor cursor = fetchAllEntries();

		// as long as there are entries
		while (!cursor.isAfterLast()) {

//			String location = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOCATION));
//			int fingerprintId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FINGERPRINT_ID));
//			String deviceAddress = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEVICE_ADDRESS));

			int deviceRssi = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DEVICE_RSSI));
			int rowId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ROWID));

			if (deviceRssi == 0) {
				deleteRows.add(rowId);
			}

			cursor.moveToNext();
		}

		for (Integer rowId : deleteRows) {
			Log.i(TAG, "delete row: " + rowId);
			deleteEntry(rowId);
		}

	}

}
