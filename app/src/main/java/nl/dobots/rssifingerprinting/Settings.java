package nl.dobots.rssifingerprinting;

import android.content.Context;
import android.content.SharedPreferences;

import nl.dobots.rssifingerprinting.db.FingerprintDbAdapter;

/**
 * Copyright (c) 2015 Dominik Egger <dominik@dobots.nl>. All rights reserved.
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
 * Created on 7-12-15
 *
 * @author Dominik Egger
 */
public class Settings {

	private static final String SHARED_PREFS = "settings";

	private static final String SCAN_INTERVAL = "prefs_scaninterval";
	private static final String PAUSE_INTERVAL = "prefs_pauseinterval";

	private static Settings ourInstance;

	private final Context _context;
	private final SharedPreferences _sharedPreferences;

	private int _scanInterval;
	private int _pauseInterval;

	private FingerprintDbAdapter _dbAdapter = null;

	public static Settings getInstance(Context context) {

		if (ourInstance == null) {
			ourInstance = new Settings(context);
		}

		return ourInstance;
	}

	private Settings(Context context) {
		this._context = context;

		_sharedPreferences = _context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
		readSettings();
	}

	public void destroy() {
		if (_dbAdapter != null) {
			_dbAdapter.close();
			_dbAdapter = null;
		}
	}

	public void readSettings() {
		_scanInterval = _sharedPreferences.getInt(SCAN_INTERVAL, Config.SCAN_INTERVAL);
		_pauseInterval = _sharedPreferences.getInt(PAUSE_INTERVAL, Config.SCAN_PAUSE);
	}

	public void setScanInterval(int scanInterval) {
		SharedPreferences.Editor editor = _sharedPreferences.edit();
		_scanInterval = scanInterval;
		editor.putInt(SCAN_INTERVAL, _scanInterval);
		editor.commit();
	}

	public int getScanInterval() {
		return _scanInterval;
	}

	public void setPauseInterval(int pauseInterval) {
		SharedPreferences.Editor editor = _sharedPreferences.edit();
		_pauseInterval = pauseInterval;
		editor.putInt(PAUSE_INTERVAL, _pauseInterval);
		editor.commit();
	}

	public int getPauseInterval() {
		return _pauseInterval;
	}

	public void clearSettings() {
		final SharedPreferences.Editor editor = _sharedPreferences.edit();
		editor.clear().commit();
		_scanInterval = Config.SCAN_INTERVAL;
		_pauseInterval = Config.SCAN_PAUSE;
	}

	public FingerprintDbAdapter getDbAdapter(Context context) {
		if (_dbAdapter == null) {
			_dbAdapter = new FingerprintDbAdapter(context).open(Config.DATABASE_NAME, Config.DATABASE_VERSION);
		}
		return _dbAdapter;
	}

	public void deleteFingerprintDb(Context context) {
		context.deleteDatabase(Config.DATABASE_NAME);
		_dbAdapter = null;
	}

}
