package nl.dobots.rssifingerprinting.db;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

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
 * Created on 22-2-16
 *
 * @author Dominik Egger
 */
public class LocationWithFingerprints {

	private String _locationName;
	private HashMap<Integer, Fingerprint> _fingerprints = new HashMap<>();

	public LocationWithFingerprints(String location) {
		_locationName = location;
	}

	public void addFingerprint(int fingerprintId, String deviceAddress, int deviceRssi) {
		Fingerprint fingerprint;
		if (_fingerprints.containsKey(fingerprintId)) {
			fingerprint = _fingerprints.get(fingerprintId);
		} else {
			fingerprint = new Fingerprint(_locationName, fingerprintId);
			_fingerprints.put(fingerprintId, fingerprint);
		}
		fingerprint.addDevice(deviceAddress, deviceRssi);
	}

	public String getLocationName() {
		return _locationName;
	}

	public ArrayList<Fingerprint> getFingerprints() {
		ArrayList<Fingerprint> result = new ArrayList<>();
		result.addAll(_fingerprints.values());
		Collections.sort(result, new Comparator<Fingerprint>() {
			@Override
			public int compare(Fingerprint lhs, Fingerprint rhs) {
				int l = lhs.getFingerprintId();
				int r = rhs.getFingerprintId();
				return l < r ? -1 : (l == r ? 0 : 1);
			}
		});
		return result;
	}

}
