package iot.challenge.jura.faro;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.ble.eddystone.BluetoothLeEddystone;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeacon;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.message.KuraPayload;

import iot.challenge.jura.faro.advertiser.Advertiser;
import iot.challenge.jura.faro.scanner.Scanner;

/**
 * Beacon protocol
 */
public enum Protocol {
	iBeacon("iBeacon") {
		@Override
		protected BeaconHelper createGenericHelper() {
			class Helper implements iot.challenge.jura.faro.ibeacon.Helper {
			}
			return new Helper();
		}

		@Override
		protected Advertiser createAdvertiser() {
			class IBeaconAdvertiser extends Advertiser implements iot.challenge.jura.faro.ibeacon.Helper {
			}
			return new IBeaconAdvertiser();
		}

		@Override
		protected Scanner createScanner() {
			class IBeaconScanner extends Scanner implements iot.challenge.jura.faro.ibeacon.Helper {
			}
			return new IBeaconScanner();
		}
	},
	Eddystone("Eddystone") {
		@Override
		protected BeaconHelper createGenericHelper() {
			class Helper implements iot.challenge.jura.faro.eddystone.Helper {
			}
			return new Helper();
		}

		@Override
		protected Advertiser createAdvertiser() {
			class EddystoneAdvertiser extends Advertiser implements iot.challenge.jura.faro.eddystone.Helper {
			}
			return new EddystoneAdvertiser();
		}

		@Override
		protected Scanner createScanner() {
			class EddystoneScanner extends Scanner implements iot.challenge.jura.faro.eddystone.Helper {
			}
			return new EddystoneScanner();
		}
	};

	private static Map<Protocol, BeaconHelper> helpers;

	private String name;

	private Protocol(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static final String PROPERTY_BEACON_PROTOCOL = "beacon.protocol";
	public static final Protocol PROPERTY_BEACON_PROTOCOL_DEFAULT = iBeacon;

	/**
	 * Discover protocol from properties
	 * 
	 * @param properties
	 *            Map of properties
	 * @return Protocol
	 */
	public static Protocol readProtocol(Map<String, Object> properties) {
		return valueOf(
				(String) properties.getOrDefault(PROPERTY_BEACON_PROTOCOL, PROPERTY_BEACON_PROTOCOL_DEFAULT.name()));
	}

	/**
	 * Discover protocol from payload
	 * 
	 * @param payload
	 *            Payload
	 * @return Protocol
	 */
	public static Protocol readProtocol(KuraPayload payload) {
		return valueOf((String) payload.getMetric(BeaconHelper.PROTOCOL));
	}

	/**
	 * Discover protocol from beacon
	 * 
	 * @param beacon
	 *            Beacon
	 * @return Protocol
	 */
	public static Protocol readProtocol(BluetoothLeBeacon beacon) {
		if (beacon instanceof BluetoothLeIBeacon) {
			return iBeacon;
		} else if (beacon instanceof BluetoothLeEddystone) {
			return Eddystone;
		} else {
			return null;
		}
	}

	/**
	 * Discover protocol from string
	 * 
	 * @param string
	 *            String
	 * @return Protocol
	 */
	public static Protocol readProtocolFromStringBeacon(String string) {
		String prefix = BeaconHelper.PROTOCOL + BeaconHelper.VALUE_SEPARATOR;
		if (string.contains(prefix + iBeacon.name)) {
			return iBeacon;
		} else if (string.contains(prefix + Eddystone.name)) {
			return Eddystone;
		} else {
			return null;
		}
	}

	/**
	 * Creates beacon manager for a given working mode
	 * 
	 * @param workingMode
	 *            Working mode
	 * @return Beacon Manager
	 */
	public BeaconManager createBeaconManager(WorkingMode workingMode) {
		switch (workingMode) {
		case scanner:
			return createScanner();

		case advertiser:
			return createAdvertiser();

		default:
			return null;
		}
	}

	/**
	 * Creates a beacon's helper that does not extends a beacon manager
	 * 
	 * @return Beacon's generic helper
	 */
	public BeaconHelper getGenericHelper() {
		if (helpers == null)
			helpers = new HashMap<>();

		BeaconHelper helper = helpers.get(this);
		if (helper == null) {
			helper = createGenericHelper();
			helpers.put(this, helper);
		}
		return helper;
	}

	/**
	 * Add 'beacon' information to 'payload'
	 * 
	 * @param payload
	 *            Payload
	 * @param beacon
	 *            Beacon
	 */
	public void addMetrics(KuraPayload payload, BluetoothLeBeacon beacon) {
		getGenericHelper().addMetrics(payload, beacon);
	}

	/**
	 * Creates a beacon using 'payload'
	 * 
	 * @param payload
	 *            Payload
	 * @return Created beacon
	 */
	public BluetoothLeBeacon readMetrics(KuraPayload payload) {
		return getGenericHelper().readMetrics(payload);
	}

	/**
	 * Copy a beacon
	 * 
	 * @param beacon
	 *            Beacon to be copied
	 * @return Copied beacon
	 */
	public static BluetoothLeBeacon copyBeacon(BluetoothLeBeacon beacon) {
		Protocol protocol = readProtocol(beacon);
		if (protocol != null) {
			return protocol.copy(beacon);
		}
		return null;
	}

	/**
	 * Copy a beacon
	 * 
	 * @param beacon
	 *            Beacon to be copied
	 * @return Copied beacon
	 */
	private BluetoothLeBeacon copy(BluetoothLeBeacon beacon) {
		return getGenericHelper().copyBeacon(beacon);
	}

	/**
	 * Serialize a beacon
	 * 
	 * @param beacon
	 *            Beacon to serialize
	 * @return Serialized beacon
	 */
	public static String beaconToString(BluetoothLeBeacon beacon) {
		Protocol protocol = readProtocol(beacon);
		if (protocol != null) {
			return protocol.toString(beacon);
		}
		return null;
	}

	/**
	 * Serialize a beacon
	 * 
	 * @param beacon
	 *            Beacon to serialize
	 * @return Serialized beacon
	 */
	private String toString(BluetoothLeBeacon beacon) {
		return getGenericHelper().beaconToString(beacon);
	}

	/**
	 * Deserialize a beacon
	 * 
	 * @param string
	 *            String
	 * @return Deserialized beacon
	 */
	public static BluetoothLeBeacon beaconFromString(String string) {
		Protocol protocol = readProtocolFromStringBeacon(string);
		if (protocol != null) {
			return protocol.fromString(string);
		}
		return null;
	}

	/**
	 * Deserialize a beacon
	 * 
	 * @param string
	 *            String
	 * @return Deserialized beacon
	 */
	private BluetoothLeBeacon fromString(String string) {
		return getGenericHelper().beaconFromString(string);
	}

	abstract protected BeaconHelper createGenericHelper();

	abstract protected Advertiser createAdvertiser();

	abstract protected Scanner createScanner();

}
