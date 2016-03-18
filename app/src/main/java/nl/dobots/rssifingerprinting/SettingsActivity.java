package nl.dobots.rssifingerprinting;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import java.util.Date;

public class SettingsActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		final Settings settings = Settings.getInstance(getApplicationContext());

		final NumberPicker npScanIntervalH = (NumberPicker) findViewById(R.id.npScanIntervalH);
		final NumberPicker npScanIntervalM = (NumberPicker) findViewById(R.id.npScanIntervalM);
		final NumberPicker npScanIntervalS = (NumberPicker) findViewById(R.id.npScanIntervalS);

		final NumberPicker npPauseIntervalH = (NumberPicker) findViewById(R.id.npPauseIntervalH);
		final NumberPicker npPauseIntervalM = (NumberPicker) findViewById(R.id.npPauseIntervalM);
		final NumberPicker npPauseIntervalS = (NumberPicker) findViewById(R.id.npPauseIntervalS);

//		Date scanInterval = new Date(Config.SCAN_INTERVAL);
		int scanInterval = settings.getScanInterval();
		npScanIntervalH.setMaxValue(24);
		npScanIntervalH.setValue(scanInterval / 1000 / 60 / 60);
		npScanIntervalM.setMaxValue(60);
		npScanIntervalM.setValue(scanInterval / 1000 / 60 % 60);
		npScanIntervalS.setMaxValue(60);
		npScanIntervalS.setValue(scanInterval / 1000 % 60);

		int pauseInterval = settings.getPauseInterval();
		npPauseIntervalH.setMaxValue(24);
		npPauseIntervalH.setValue(pauseInterval / 1000 / 60 / 60);
		npPauseIntervalM.setMaxValue(60);
		npPauseIntervalM.setValue(pauseInterval / 1000 / 60 % 60);
		npPauseIntervalS.setMaxValue(60);
		npPauseIntervalS.setValue(pauseInterval / 1000 % 60);

		Button btnSave = (Button) findViewById(R.id.btnSave);
		btnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int scanInterval = npScanIntervalH.getValue() * 60 * 60 * 1000 +
						npScanIntervalM.getValue() * 60 * 1000 +
						npScanIntervalS.getValue() * 1000;
				settings.setScanInterval(scanInterval);

				int pauseInterval = npPauseIntervalH.getValue() * 60 * 60 * 1000 +
						npPauseIntervalM.getValue() * 60 * 1000 +
						npPauseIntervalS.getValue() * 1000;
				settings.setPauseInterval(pauseInterval);

				SettingsActivity.this.finish();
			}
		});

		Button btnCancel = (Button) findViewById(R.id.btnCancel);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SettingsActivity.this.finish();
			}
		});
	}


}
