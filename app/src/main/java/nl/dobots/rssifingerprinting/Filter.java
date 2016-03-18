package nl.dobots.rssifingerprinting;

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
 * Created on 23-2-16
 *
 * @author Dominik Egger
 */
public class Filter {

	public static final String[] BEACONS = {
			"DE:41:8E:2F:58:85",
			"DC:1A:5A:AF:1A:44",
			"C6:A0:0C:5A:C8:C6",
			"C0:82:3E:B9:F5:91",
			"E8:C5:AE:A7:6B:A9",
			"F1:C1:B8:AC:03:CD",
			"C5:71:64:3A:15:74",
			"D5:A7:34:EC:72:90",
			"C2:92:09:5F:04:78",
			"ED:AF:F3:7E:E1:47",
			"C6:27:A8:D7:D4:C7",
			"D5:6B:B8:B4:39:C0",
			"E0:31:D7:C5:CA:FF",
			"F8:27:73:28:DA:FE",
			"CC:02:60:7A:83:46",
			"FD:CB:99:58:0B:88",
			"D7:D5:51:82:49:43",
			"F0:20:A1:2C:57:D4",
			"DB:26:1F:D9:FA:5E",
			"ED:F5:F8:E3:6A:F6",
			"F4:A2:89:23:53:92",
			"E1:89:95:C1:06:04",
			"EB:82:34:DA:EE:0B",
			"EB:C9:C2:58:52:C4"
	};

	public static boolean contains(String address) {
		for (String beacon : BEACONS) {
			if (beacon.equals(address)) {
				return true;
			}
		}
		return false;
	}

}
