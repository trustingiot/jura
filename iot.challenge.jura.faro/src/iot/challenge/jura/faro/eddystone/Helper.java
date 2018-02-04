package iot.challenge.jura.faro.eddystone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.ble.eddystone.BluetoothLeEddystone;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.message.KuraPayload;

import iot.challenge.jura.faro.BeaconEvent;
import iot.challenge.jura.faro.BeaconHelper;
import iot.challenge.jura.faro.Protocol;
import iot.challenge.jura.faro.advertiser.AdvertiserOptions;
import iot.challenge.jura.faro.eddystone.advertiser.Options;

/**
 * Eddystone beacon helper
 */
public interface Helper extends BeaconHelper {

	public static final String UID = "UID";
	public static final String URL = "URL";
	public static final String TYPE = "type";
	public static final String NAMESPACE = "namespace";
	public static final String INSTANCE = "instance";

	@Override
	default Protocol getProtocol() {
		return Protocol.Eddystone;
	}

	@Override
	default AdvertiserOptions createAdvertiserOptions(Map<String, Object> properties) {
		return new Options(properties);
	}

	@Override
	default BluetoothLeBeacon createBeacon(AdvertiserOptions options) {
		BluetoothLeEddystone eddystone = new BluetoothLeEddystone();
		Options eddystoneOptions = (Options) options;
		switch (eddystoneOptions.getEddystoneFrametype()) {
		case UID:
			eddystone.configureEddystoneUIDFrame(
					hexToByteArray(eddystoneOptions.getUidNamespace()),
					hexToByteArray(eddystoneOptions.getUidInstance()),
					options.getTxPower().shortValue());
			break;

		case URL:
			eddystone.configureEddystoneURLFrame(
					eddystoneOptions.getUrl(),
					options.getTxPower().shortValue());
			break;

		default:
			error("Unsuported eddystone frame type {}", eddystoneOptions.getEddystoneFrametype());
		}
		return eddystone;
	}

	@Override
	default void showBeaconsInfo(List<BeaconEvent> beacons) {
		int size = beacons.size();
		BluetoothLeEddystone template = (BluetoothLeEddystone) beacons.get(0).getBeacon();
		info("{} Eddystone{} {} received from {}",
				size,
				(size > 1) ? "s" : "",
				template.getFrameType(),
				template.getAddress());
	}

	@Override
	default void showBeaconInfo(BluetoothLeBeacon beacon) {
		BluetoothLeEddystone eddystone = (BluetoothLeEddystone) beacon;
		debug("Eddystone {} received from {}", eddystone.getFrameType(), eddystone.getAddress());
		if (isUID(eddystone)) {
			debug("{}: {}", NAMESPACE, bytesArrayToHexString(eddystone.getNamespace()));
			debug("{}: {}", INSTANCE, bytesArrayToHexString(eddystone.getInstance()));
		} else if (isURL(eddystone)) {
			debug("{}: {}{}", URL, eddystone.getUrlScheme(), eddystone.getUrl());
		}
		debug("{}: {}", TXPOWER, eddystone.getTxPower());
		debug("{}: {}", RSSI, beacon.getRssi());
	}

	@Override
	default void addProtocolSpecificMetrics(KuraPayload payload, BluetoothLeBeacon beacon) {
		BluetoothLeEddystone eddystone = (BluetoothLeEddystone) beacon;
		payload.addMetric(TYPE, eddystone.getFrameType());
		if (isUID(eddystone)) {
			payload.addMetric(NAMESPACE, bytesArrayToHexString(eddystone.getNamespace()));
			payload.addMetric(INSTANCE, bytesArrayToHexString(eddystone.getInstance()));
		} else if (isURL(eddystone)) {
			payload.addMetric(URL, eddystone.getUrl());
		}
	}

	@Override
	default int getTxPower(BluetoothLeBeacon beacon) {
		return ((BluetoothLeEddystone) beacon).getTxPower();
	}

	@Override
	default BluetoothLeBeacon readProtocolSpecificMetrics(KuraPayload payload) {
		BluetoothLeEddystone eddystone = new BluetoothLeEddystone();
		short txPower = (short) (int) payload.getMetric(TXPOWER);
		eddystone.setFrameType((String) payload.getMetric(TYPE));
		if (isUID(eddystone)) {
			byte[] namespace = hexToByteArray((String) payload.getMetric(NAMESPACE));
			byte[] instance = hexToByteArray((String) payload.getMetric(INSTANCE));
			eddystone.configureEddystoneUIDFrame(namespace, instance, txPower);
		} else if (isURL(eddystone)) {
			String url = (String) payload.getMetric(URL);
			eddystone.configureEddystoneURLFrame(url, txPower);
		}
		return eddystone;
	}

	@Override
	default BluetoothLeBeacon copyBeacon(BluetoothLeBeacon beacon) {
		BluetoothLeEddystone eddystone = (BluetoothLeEddystone) beacon;
		BluetoothLeEddystone result = new BluetoothLeEddystone();
		if (isUID(eddystone)) {
			result.configureEddystoneUIDFrame(eddystone.getNamespace().clone(), eddystone.getInstance().clone(),
					eddystone.getTxPower());
		} else if (isURL(eddystone)) {
			result.configureEddystoneURLFrame(eddystone.getUrl(), eddystone.getTxPower());
		}
		result.setAddress(eddystone.getAddress());
		result.setRssi(eddystone.getRssi());
		return result;
	}

	@Override
	default List<String> toStringProtocolSpecificFields(BluetoothLeBeacon beacon) {
		List<String> fields = new ArrayList<>();
		BluetoothLeEddystone eddystone = (BluetoothLeEddystone) beacon;
		fields.add(TYPE + VALUE_SEPARATOR + eddystone.getFrameType());
		if (isUID(eddystone)) {
			fields.add(NAMESPACE + VALUE_SEPARATOR + bytesArrayToHexString(eddystone.getNamespace()));
			fields.add(INSTANCE + VALUE_SEPARATOR + bytesArrayToHexString(eddystone.getInstance()));
		} else if (isURL(eddystone)) {
			fields.add(URL + VALUE_SEPARATOR + eddystone.getUrl());
		}
		return fields;
	}

	@Override
	default BluetoothLeBeacon fromStringProtocolSpecificFields(Map<String, String> fields) {
		BluetoothLeEddystone eddystone = new BluetoothLeEddystone();
		short txPower = Short.parseShort(fields.get(TXPOWER));
		eddystone.setFrameType(fields.get(TYPE));
		if (isUID(eddystone)) {
			byte[] namespace = hexToByteArray(fields.get(NAMESPACE));
			byte[] instance = hexToByteArray(fields.get(INSTANCE));
			eddystone.configureEddystoneUIDFrame(namespace, instance, txPower);
		} else if (isURL(eddystone)) {
			eddystone.configureEddystoneURLFrame(fields.get(URL), txPower);
		}
		return eddystone;
	}

	default boolean isUID(BluetoothLeEddystone beacon) {
		return isFrameType(beacon, UID);
	}

	default boolean isURL(BluetoothLeEddystone beacon) {
		return isFrameType(beacon, URL);
	}

	default boolean isFrameType(BluetoothLeEddystone beacon, String type) {
		return type.equals(beacon.getFrameType());
	}

}
