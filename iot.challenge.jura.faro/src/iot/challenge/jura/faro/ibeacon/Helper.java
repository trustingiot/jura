package iot.challenge.jura.faro.ibeacon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeacon;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.message.KuraPayload;

import iot.challenge.jura.faro.BeaconEvent;
import iot.challenge.jura.faro.BeaconHelper;
import iot.challenge.jura.faro.Protocol;
import iot.challenge.jura.faro.advertiser.AdvertiserOptions;
import iot.challenge.jura.faro.ibeacon.advertiser.Options;

/**
 * iBeacon beacon helper
 */
public interface Helper extends BeaconHelper {

	public static final String UUID = "uuid";
	public static final String MAJOR = "major";
	public static final String MINOR = "minor";

	@Override
	default Protocol getProtocol() {
		return Protocol.iBeacon;
	}

	@Override
	default AdvertiserOptions createAdvertiserOptions(Map<String, Object> properties) {
		return new Options(properties);
	}

	@Override
	default BluetoothLeBeacon createBeacon(AdvertiserOptions options) {
		Options iBeacon = (Options) options;
		return new BluetoothLeIBeacon(
				iBeacon.getUuid(),
				(short) iBeacon.getMajor(),
				(short) iBeacon.getMinor(),
				options.getTxPower().shortValue());
	}

	@Override
	default void showBeaconsInfo(List<BeaconEvent> beacons) {
		int size = beacons.size();
		BluetoothLeBeacon template = (BluetoothLeIBeacon) beacons.get(0).getBeacon();
		info("{} iBeacon{} received from {}",
				size,
				(size > 1) ? "s" : "",
				template.getAddress());
	}

	@Override
	default void showBeaconInfo(BluetoothLeBeacon beacon) {
		BluetoothLeIBeacon iBeacon = (BluetoothLeIBeacon) beacon;
		debug("iBeacon received from {}", iBeacon.getAddress());
		debug("{}: {}", UUID, iBeacon.getUuid());
		debug("{}: {}", MAJOR, iBeacon.getMajor());
		debug("{}: {}", MINOR, iBeacon.getMinor());
		debug("{}: {}", TXPOWER, iBeacon.getTxPower());
		debug("{}: {}", RSSI, iBeacon.getRssi());
	}

	@Override
	default void addProtocolSpecificMetrics(KuraPayload payload, BluetoothLeBeacon beacon) {
		BluetoothLeIBeacon iBeacon = (BluetoothLeIBeacon) beacon;
		payload.addMetric(UUID, iBeacon.getUuid().toString());
		payload.addMetric(MAJOR, (int) iBeacon.getMajor());
		payload.addMetric(MINOR, (int) iBeacon.getMinor());
	}

	@Override
	default int getTxPower(BluetoothLeBeacon beacon) {
		return ((BluetoothLeIBeacon) beacon).getTxPower();
	}

	@Override
	default BluetoothLeBeacon readProtocolSpecificMetrics(KuraPayload payload) {
		BluetoothLeIBeacon beacon = new BluetoothLeIBeacon();
		beacon.setUuid(java.util.UUID.fromString((String) payload.getMetric(UUID)));
		beacon.setTxPower((short) (int) payload.getMetric(TXPOWER));
		beacon.setMajor((short) (int) payload.getMetric(MAJOR));
		beacon.setMinor((short) (int) payload.getMetric(MINOR));
		return beacon;
	}

	@Override
	default BluetoothLeBeacon copyBeacon(BluetoothLeBeacon beacon) {
		BluetoothLeIBeacon ibeacon = (BluetoothLeIBeacon) beacon;
		BluetoothLeIBeacon result = new BluetoothLeIBeacon(ibeacon.getUuid(), ibeacon.getMajor(), ibeacon.getMinor(),
				ibeacon.getTxPower());
		result.setRssi(beacon.getRssi());
		result.setAddress(beacon.getAddress());
		return result;
	}

	@Override
	default List<String> toStringProtocolSpecificFields(BluetoothLeBeacon beacon) {
		List<String> fields = new ArrayList<>();
		BluetoothLeIBeacon iBeacon = (BluetoothLeIBeacon) beacon;
		fields.add(UUID + VALUE_SEPARATOR + iBeacon.getUuid().toString());
		fields.add(MAJOR + VALUE_SEPARATOR + iBeacon.getMajor());
		fields.add(MINOR + VALUE_SEPARATOR + iBeacon.getMinor());
		return fields;
	}

	@Override
	default BluetoothLeBeacon fromStringProtocolSpecificFields(Map<String, String> fields) {
		BluetoothLeIBeacon beacon = new BluetoothLeIBeacon();
		beacon.setUuid(java.util.UUID.fromString(fields.get(UUID)));
		beacon.setTxPower(Short.parseShort(fields.get(TXPOWER)));
		beacon.setMajor(Short.parseShort(fields.get(MAJOR)));
		beacon.setMinor(Short.parseShort(fields.get(MINOR)));
		return beacon;
	}
}
