package nl.dobots.rssifingerprinting;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

import nl.dobots.bluenet.ble.extended.structs.BleDevice;
import nl.dobots.bluenet.ble.extended.structs.BleDeviceList;
import nl.dobots.bluenet.service.BleScanService;
import nl.dobots.bluenet.service.callbacks.EventListener;
import nl.dobots.bluenet.service.callbacks.IntervalScanListener;
import nl.dobots.bluenet.utils.BleLog;
import nl.dobots.rssifingerprinting.db.Fingerprint;
import nl.dobots.rssifingerprinting.db.FingerprintDbAdapter;
import nl.dobots.rssifingerprinting.db.LocationWithFingerprints;

public class LocateActivity extends AppCompatActivity implements EventListener, IntervalScanListener {

	private static final String TAG = LocateActivity.class.getCanonicalName();

	private BleScanService _service;
	private boolean _bound;

	private boolean _running = false;

	private Settings _settings;
	private FingerprintDbAdapter _fingerprintDb;

	private Button _btnLocate;
	private ArrayList<LocationWithFingerprints> _locations;
	private BleDeviceList _bleDeviceList;

	private TextView _txtLastUpdate;
	private TextView _txtLocation;
	private TextView _txtBest;
	private TextView _txtLastBest;
	private TextView _txtOldBest;

	private Fingerprint _bestGuess;
	private Fingerprint _lastBestGuess;
	private Fingerprint _oldBestGuess;
	private Fingerprint _location;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initUI();

		// create and bind to the BleScanService
		Intent intent = new Intent(this, BleScanService.class);
		bindService(intent, _connection, Context.BIND_AUTO_CREATE);

		_settings = Settings.getInstance(getApplicationContext());
		_fingerprintDb = _settings.getDbAdapter(getApplicationContext());
		_locations = _fingerprintDb.getAllLocations();

		BleDevice.setExpirationTime(Config.SCAN_INTERVAL * 2);

		BleLog.setLogLevel(Log.INFO);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (_bound) {
			unbindService(_connection);
		}
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
			_service.registerEventListener(LocateActivity.this);

			// register as an interval scan listener. If you only need to know the list of scanned
			// devices at every end of an interval, then this is better. additionally it also informs
			// about the start of an interval.
			_service.registerIntervalScanListener(LocateActivity.this);

			// set the scan interval (for how many ms should the service scan for devices)
			_service.setScanInterval(_settings.getScanInterval());
			// set the scan pause (how many ms should the service wait before starting the next scan)
			_service.setScanPause(_settings.getPauseInterval());
//			_service.setScanPause(2000);

			_bound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG, "disconnected from service");
			_bound = false;
		}
	};

	private void initUI() {
		setContentView(R.layout.activity_locate);
		_btnLocate = (Button) findViewById(R.id.btnLocate);
		_btnLocate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!_running) {
					_btnLocate.setText("Stop");
					_service.startIntervalScan(Config.BLE_DEVICE_FILTER);
					_running = true;
				} else {
					_btnLocate.setText("Locate");
					_service.stopIntervalScan();
					_running = false;
				}
			}
		});

		_txtLastUpdate = (TextView) findViewById(R.id.txtLastUpdate);
		_txtLocation = (TextView) findViewById(R.id.txtLocation);
		_txtBest = (TextView) findViewById(R.id.txtBest);
		_txtLastBest = (TextView) findViewById(R.id.txtLastBest);
		_txtOldBest = (TextView) findViewById(R.id.txtOldBest);

	}

	@Override
	public void onEvent(Event event) {

	}

	@Override
	public void onScanStart() {
	}

	Handler mHandler = new Handler();

	@Override
	public void onScanEnd() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				_txtLastUpdate.setText(Config.SDF_DISPLAY_TIMESTAMP.format(new Date()));
			}
		});

		_bleDeviceList = _service.getDeviceMap().getRssiSortedList();

		for (BleDevice dev : _bleDeviceList) {
			Log.d(TAG, String.format("device: %s [%d] (%d)", dev.getAddress(), dev.getRssi(), dev.getOccurrences()));
		}

//		// for now only "single shot"
//		_service.stopIntervalScan();

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				// for now only "single shot"
				compareFingerprint(_locations, _bleDeviceList);
			}
		});

		_service.clearDeviceMap();
	}

	private void compareFingerprint(ArrayList<LocationWithFingerprints> locations, BleDeviceList bleDeviceList) {

		double maxProb = Double.MAX_VALUE;
		double maxLikely = 0.0;
		Fingerprint bestProbFingerprint = null;
		Fingerprint maxLikelyFingerprint = null;

		for (LocationWithFingerprints dbLoc : locations) {

			for (Fingerprint dbFingerprint : dbLoc.getFingerprints()) {

				int sum = 0;
				int count = 0;

//				for (BleDevice device : bleDeviceList) {
//
//					BleDevice dbDevice = dbFingerprint.getDevice(device.getAddress());
//					if (dbDevice != null) {
//						sum += Math.pow(device.getAverageRssi() - dbDevice.getRssi(), 2);
//						count++;
//					} else {
//						// todo: what now???
//					}
//					// todo: and do we need to account for the devices which are in the fingerprint but were
//					// not seen now ???!
//
//				}

				for (BleDevice dbDevice : dbFingerprint.getDevices()) {

//					if (!Filter.contains(dbDevice.getAddress())) {
////						Log.w(TAG, "filtered: " + dbDevice.getAddress());
//						continue;
//					}

					BleDevice device = bleDeviceList.getDevice(dbDevice.getAddress());
					if (device != null) {
						sum += Math.pow(device.getAverageRssi() - dbDevice.getRssi(), 2);
						count++;
					} else {
						sum += Math.pow(dbDevice.getRssi(), 2);
					}

				}

				double fpDistance = 0.0;
				if ((sum & count) != 0) {
					fpDistance = Math.sqrt(sum / count);
				}

				double devNoise = 1.0; // todo: how much???
				double likelihood = Math.exp(-fpDistance / (2 * Math.pow(devNoise, 2)));

//				Log.i(TAG, String.format("[%s][%d] F: %f, L: %f", dbLoc.getLocationName(), dbFingerprint.getFingerprintId(), fpDistance, likelihood));

				if (fpDistance != 0 && fpDistance < maxProb) {
					maxProb = fpDistance;
					bestProbFingerprint = dbFingerprint;
				}

				if (likelihood != 1 && likelihood > maxLikely) {
					maxLikely = likelihood;
					maxLikelyFingerprint = dbFingerprint;
				}

			}
		}

		if (bestProbFingerprint != null) {
			Log.i(TAG, String.format("best Prob FP: %s, %d", bestProbFingerprint.getLocation(), bestProbFingerprint.getFingerprintId()));
		}
		if (maxLikelyFingerprint != null) {
			Log.i(TAG, String.format("max Likely FP: %s, %d", maxLikelyFingerprint.getLocation(), maxLikelyFingerprint.getFingerprintId()));
		}

		_oldBestGuess = _lastBestGuess;
		_lastBestGuess = _bestGuess;

		_bestGuess = bestProbFingerprint;

		if (_lastBestGuess != null && _bestGuess != null &&
				_lastBestGuess.getLocation().equals(_bestGuess.getLocation())) {
			_location = _bestGuess;
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (_bestGuess != null) {
					_txtBest.setText(_bestGuess.getLocation());
				}
				if (_lastBestGuess != null) {
					_txtLastBest.setText(_lastBestGuess.getLocation());
				}
				if (_oldBestGuess != null) {
					_txtOldBest.setText(_oldBestGuess.getLocation());
				}
				if (_location != null) {
					_txtLocation.setText(_location.getLocation());
				}
			}
		});

	}
}
