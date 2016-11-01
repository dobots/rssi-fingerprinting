package nl.dobots.rssifingerprinting;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import nl.dobots.bluenet.ble.base.callbacks.IStatusCallback;
import nl.dobots.bluenet.ble.extended.structs.BleDevice;
import nl.dobots.bluenet.ble.extended.structs.BleDeviceList;
import nl.dobots.bluenet.service.BleScanService;
import nl.dobots.bluenet.service.callbacks.EventListener;
import nl.dobots.bluenet.service.callbacks.IntervalScanListener;
import nl.dobots.loopback.util.BleScanServiceUploadHelper;
import nl.dobots.rssifingerprinting.db.Fingerprint;
import nl.dobots.rssifingerprinting.db.FingerprintDbAdapter;
import nl.dobots.rssifingerprinting.db.LocationWithFingerprints;

public class MainActivity extends AppCompatActivity implements EventListener, IntervalScanListener {

	private static final String TAG = MainActivity.class.getCanonicalName();

	private TextView _txtStatus;
	private TextView _txtLastScan;

	private BleScanService _service;
	private boolean _bound;

	private boolean _started = false;

	private BleScanServiceUploadHelper _uploadHelper;

	private BleDeviceList _bleDeviceList;
	private ListView _lvScan;

	private File _fingerprintFile;
	private DataOutputStream _fingerprintDos;
	private boolean _fileOpen;

	private Settings _settings;
	private FingerprintDbAdapter _fingerprintDb;

	private String _roomName;
	private EditText _edtRoomName;

	private ArrayList<String> _roomNames = new ArrayList<>();
//	private String[] _roomNames = {
//			"Test",
//			"DoBots Software",
//			"Peet",
//			"Hallway 1",
//			"Hallway 2",
//			"Hallway 3",
//			"DoBots Hardware",
//			"Proto Room",
//			"SMP Room",
//			"Almende",
//			"Allert"
//	};
	private Spinner _spRoomName;
	private ArrayAdapter<String> _arrayAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		_watchdog.postDelayed(_watchdogRunner, GUI_UPDATE_INTERVAL);

		BleDevice.setExpirationTime(Config.SCAN_INTERVAL);

		_settings = Settings.getInstance(getApplicationContext());
		_fingerprintDb = _settings.getDbAdapter(getApplicationContext());

		_roomNames.add("");
		for (LocationWithFingerprints loc : _fingerprintDb.getAllLocations()) {
			_roomNames.add(loc.getLocationName());
		}

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		initUI();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
//		if (_bound) {
//			unbindService(_connection);
//		}
		_settings.destroy();
	}

	// if the service was connected successfully, the service connection gives us access to the service
	private ServiceConnection _connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "connected to ble scan service ...");
			// get the service from the binder
			BleScanService.BleScanBinder binder = (BleScanService.BleScanBinder) service;
			_service = binder.getService();

			// register as event listener. Events, like bluetooth initialized, and bluetooth turned
			// off events will be triggered by the service, so we know if the user turned bluetooth
			// on or off
			_service.registerEventListener(MainActivity.this);

			// register as an interval scan listener. If you only need to know the list of scanned
			// devices at every end of an interval, then this is better. additionally it also informs
			// about the start of an interval.
			_service.registerIntervalScanListener(MainActivity.this);

			// set the scan interval (for how many ms should the service scan for devices)
			_service.setScanInterval(_settings.getScanInterval());
			// set the scan pause (how many ms should the service wait before starting the next scan)
			_service.setScanPause(_settings.getPauseInterval());

			_uploadHelper = new BleScanServiceUploadHelper(_service, MainActivity.this);
			_uploadHelper.enableScanUpload("lib-user@dobots.nl", "dodedodo");

			_bound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG, "disconnected from service");
			_bound = false;
			// set to null to make garbage collector destroy the helper class. will be recreated
			// when service connects
			_uploadHelper = null;
		}
	};

	@Override
	protected void onPause() {
		super.onPause();
		stopFingerprinting();
		if (_bound) {
			unbindService(_connection);
			_bound = false;
			// set to null to make garbage collector destroy the helper class. will be recreated
			// when service connects
			_uploadHelper = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!_bound) {
			// create and bind to the BleScanService
			Intent intent = new Intent(this, BleScanService.class);
			bindService(intent, _connection, Context.BIND_AUTO_CREATE);
		}

//		startActivity(new Intent(this, LocateActivity.class));
	}

	// is scanning returns true if the service is "running", not if it is currently in a
	// scan interval or a scan pause
	private boolean isScanning() {
		if (_bound) {
			return _service.isRunning();
		}
		return false;
	}

	private void initUI() {
		setContentView(R.layout.activity_main);

		_edtRoomName = (EditText) findViewById(R.id.edtRoomName);
//		_edtRoomName.setText("DoBots Software");

		_spRoomName = (Spinner) findViewById(R.id.spRoomName);

		_arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, _roomNames);
		_spRoomName.setAdapter(_arrayAdapter);
		_spRoomName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				_edtRoomName.setText(_roomNames.get(position));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
//		_spRoomName.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//			@Override
//			public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
//				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//				builder.setTitle("Confirm")
//						.setMessage("Delete Location " + _roomNames.get(position) + "?")
//						.setPositiveButton("Yex", new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								_fingerprintDb.deleteLocation(_roomName);
//								_roomNames.remove(position);
//							}
//						})
//						.setNegativeButton("No", new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								// nothing
//							}
//						});
//				builder.create().show();
//				return true;
//			}
//		});

		Button btnStart = (Button) findViewById(R.id.btnStart);
		btnStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startFingerprinting();
			}
		});

		Button btnStop = (Button) findViewById(R.id.btnStop);
		btnStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopFingerprinting();
			}
		});

		_txtStatus = (TextView) findViewById(R.id.txtStatus);
		_txtLastScan = (TextView) findViewById(R.id.txtLastScan);

		// create an empty list to assign to the list view. this will be updated whenever a
		// device is scanned
		_bleDeviceList = new BleDeviceList();
		DeviceListAdapter adapter = new DeviceListAdapter(this, _bleDeviceList);

		_lvScan = (ListView) findViewById(R.id.lvScan);
		_lvScan.setAdapter(adapter);
	}

	private void stopFingerprinting() {
		if (_bound) {
//			_btnScan.setText(getString(R.string.main_scan));
			// stop scanning for devices
			_service.stopIntervalScan();
			_started = false;
		}
	}

	private void startFingerprinting() {
		if (_bound && !_started) {

			_roomName = _edtRoomName.getText().toString();
//			_roomName = (String)_spRoomName.getSelectedItem();
			Fingerprint.resetCounter();

			_roomNames.add(_roomName);

			boolean hasLocation = _fingerprintDb.containsLocation(_roomName);
			if (hasLocation) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("Location exists already!")
						.setMessage("A location with this name was found already in the database. How do you want to proceed?")
						.setPositiveButton("Append", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								int lastFingerprintId = _fingerprintDb.getLastFingerprintId(_roomName);
								Fingerprint.setCounter(lastFingerprintId + 1);
								startScan();
							}
						})
						.setNeutralButton("Replace", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								_fingerprintDb.deleteLocation(_roomName);
								startScan();
							}
						})
						.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// nothing
							}
						});
				builder.create().show();
			} else {
				startScan();
			}

		}
	}

	private void startScan() {

		if (Config.LOG_TO_FILE) {

			if (Build.VERSION.SDK_INT >= 23) {
				int permissionCheck = ContextCompat.checkSelfPermission(this,
						Manifest.permission.WRITE_EXTERNAL_STORAGE);
				if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
					Log.w(TAG, "Ask for permission");
					this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
							123);
					return;
				}
			}

			_started = true;

			try {
				if (_fileOpen) {
					_fingerprintDos.close();
				}
				_fileOpen = openFingerprintFile(this, _roomName);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// make sure we start with an empty map
		_service.clearDeviceMap();
		// start scanning for devices, only return devices defined by the filter
		_service.startIntervalScan(_settings.getScanInterval(), _settings.getPauseInterval(), Config.BLE_DEVICE_FILTER);
	}

	@Override
	public void onEvent(Event event) {
		// by registering to the service as an EventListener, we will be informed whenever the
		// user turns bluetooth on or off, or even refuses to enable bluetooth
		switch (event) {
			case BLUETOOTH_INITIALIZED: {
//				onBleEnabled();
				break;
			}
			case BLUETOOTH_TURNED_OFF: {
//				onBleDisabled();
				break;
			}
			case BLE_PERMISSIONS_MISSING: {
				_started = false;
				_service.requestPermissions(this);
			}
		}
	}

	private static final int GUI_UPDATE_INTERVAL = 500;
	private long _lastUpdate;

//	private void updateDeviceList() {
//		if (System.currentTimeMillis() > _lastUpdate + GUI_UPDATE_INTERVAL) {
//			_bleDeviceList = _service.getDeviceMap().getRssiSortedList().clone();
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					// update the list view
//					DeviceListAdapter adapter = ((DeviceListAdapter) _lvScan.getAdapter());
//					adapter.updateList(_bleDeviceList);
//					adapter.notifyDataSetChanged();
//				}
//			});
//			_lastUpdate = System.currentTimeMillis();
//		}
//	}

	private enum Status {
		SCANNING,
		WAITING,
		STOPPED
	}

	private Status _currentStatus;

	private Handler _watchdog = new Handler();
	private Runnable _watchdogRunner = new Runnable() {

		@Override
		public void run() {
			if (_started) {
				if (_service.isScanActive() && _currentStatus != Status.SCANNING) {
					_txtStatus.setText("Scanning");
					_currentStatus = Status.SCANNING;
				} else if (!_service.isScanActive() && _currentStatus != Status.WAITING) {
					_txtStatus.setText("Waiting");
					_currentStatus = Status.WAITING;
				}
			} else if (_currentStatus != Status.STOPPED) {
				_txtStatus.setText("Stopped");
				_currentStatus = Status.STOPPED;
			}
			_watchdog.postDelayed(this, GUI_UPDATE_INTERVAL);
		}
	};

	@Override
	public void onScanStart() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				_txtLastScan.setText(Config.SDF_DISPLAY_TIMESTAMP.format(new Date()));

				// update the list view
				DeviceListAdapter adapter = ((DeviceListAdapter) _lvScan.getAdapter());
//				adapter.updateList(new BleDeviceList());
//				adapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onScanEnd() {
		_bleDeviceList = _service.getDeviceMap().getRssiSortedList();

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// update the list view
				DeviceListAdapter adapter = ((DeviceListAdapter) _lvScan.getAdapter());
				adapter.updateList(_bleDeviceList);
				adapter.notifyDataSetChanged();
			}
		});
		_lastUpdate = System.currentTimeMillis();

		Fingerprint fingerprint = createFingerprint(_roomName, _bleDeviceList);
		if (Config.WRITE_TO_DB) {
			if (!_fingerprintDb.addFingerprint(fingerprint)) {
				Log.e(TAG, "failed to write fingerprint do db!");
			}
		}
		if (Config.LOG_TO_FILE) {
			logFingerprint(fingerprint);
		}

		_service.clearDeviceMap();
	}

	private Fingerprint createFingerprint(String roomName, BleDeviceList bleDeviceList) {

		Fingerprint fingerprint = new Fingerprint();
		fingerprint.setLocation(roomName);
		fingerprint.setDeviceList((BleDeviceList) bleDeviceList.clone());

		return fingerprint;

	}

	private void logFingerprint(Fingerprint fingerprint) {

		if (_fileOpen) {
			Date timestamp = new Date();
			String roomName = fingerprint.getLocation();
			for (BleDevice device : fingerprint.getDevices()) {
				writeToFile(timestamp, roomName, device.getAddress(), device.getAverageRssi(), _fingerprintDos);
			}
			checkFileSizes();
		}
	}

	private boolean openFingerprintFile(Context context, String roomName) {

		String fileName = roomName.replace(" ", "-") + "_" + Config.SDF_FILENAME.format(new Date()) + ".txt";

//		File path = context.getExternalFilesDir(null);
		File path = new File(Config.LOG_FILES_DIRECTORY);
//		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		_fingerprintFile = new File(path, fileName);

//		Log.i(TAG, "external storage path: " + path.getAbsolutePath());
//		_scanBackupFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
		try {
			path.mkdirs();
			_fingerprintDos = new DataOutputStream(new FileOutputStream(_fingerprintFile));
		} catch (IOException e) {
			Log.e(TAG, "Error creating " + _fingerprintFile, e);

			return false;
		}

		return true;
	}

	private void writeToFile(Date timestamp, String roomName, String address, int rssi, DataOutputStream dos) {
//		if (_fileOpen) {
			try {
				String line = String.format("%s,%s,%s,%d\n", Config.SDF_TIMESTAMP.format(timestamp), roomName, address, rssi);
				dos.write(line.getBytes());
				dos.flush();

			} catch (IOException e) {
				e.printStackTrace();
			}
//		}
	}

	private void checkFileSizes() {

		try {

			if (_fileOpen) {
				if (_fingerprintFile.length() > Config.MAX_BACKUP_FILE_SIZE) {
					_fingerprintDos.close();
					_fileOpen = openFingerprintFile(MainActivity.this, _roomName);
				}

				if (_fingerprintFile.getFreeSpace() < Config.MIN_FREE_SPACE) {
					_fingerprintDos.close();
					_fileOpen = false;
				}
			}

			if (!_fileOpen) {
				// both files closed, we are out of memory, stop scanning
				stopFingerprinting();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		switch(id) {
			case R.id.action_settings: {
				startActivity(new Intent(this, SettingsActivity.class));
				break;
			}
			case R.id.action_exportDB: {
				String fileName = Config.LOG_FILES_DIRECTORY + "/db_" + Config.SDF_FILENAME.format(new Date()) + ".csv";
				if (!_fingerprintDb.exportDB(fileName)) {
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setTitle("Failure")
							.setMessage("Failed to export DB")
							.setNeutralButton("OK", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									MainActivity.this.finish();
								}
							});
					builder.create().show();
				}
				break;
			}
			case R.id.action_filter: {
				_fingerprintDb.filterDB();
				break;
			}
			case R.id.action_locate: {
				startActivity(new Intent(this, LocateActivity.class));
				break;
			}
			case R.id.action_deleteDB: {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("Attention")
						.setMessage("Are you sure you want to delete the DB?")
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								_settings.deleteFingerprintDb(MainActivity.this);
								_fingerprintDb = _settings.getDbAdapter(MainActivity.this);
								_roomNames.clear();
							}
						})
						.setNegativeButton("No", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {

							}
						});
				builder.create().show();
			}
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == 123) {
			if (grantResults.length > 0 &&	grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				startScan();
			} else {
				Log.e(TAG, "Can't write fingerprints without access to storage!");
			}
		} else if (!_service.getBleExt().handlePermissionResult(requestCode, permissions, grantResults,
				new IStatusCallback() {

					@Override
					public void onError(int error) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
								builder.setTitle("Fatal Error")
										.setMessage("Cannot scan for devices without permissions. Please " +
												"grant permissions or uninstall the app again!")
										.setNeutralButton("OK", new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												MainActivity.this.finish();
											}
										});
								builder.create().show();
							}
						});
					}

					@Override
					public void onSuccess() {}
				})) {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}
}
