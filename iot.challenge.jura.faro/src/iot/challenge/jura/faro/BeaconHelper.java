package iot.challenge.jura.faro;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.message.KuraPayload;

import iot.challenge.jura.faro.advertiser.AdvertiserOptions;
import iot.challenge.jura.util.trait.Loggable;

/**
 * Functions to perform specific actions with beacons
 */
public interface BeaconHelper extends Loggable {

	public static final String PROTOCOL = "protocol";
	public static final String ADDRESS = "address";
	public static final String TXPOWER = "txpower";
	public static final String RSSI = "rssi";

	public static final String VALUE_SEPARATOR = ": ";
	public static final String METRIC_SEPARATOR = "; ";

	/**
	 * Helper protocol
	 * 
	 * @return Protocol
	 */
	Protocol getProtocol();

	/**
	 * Creates the options for an advertiser
	 * 
	 * @param properties
	 *            Options values
	 * @return Advertiser options
	 */
	AdvertiserOptions createAdvertiserOptions(Map<String, Object> properties);

	/**
	 * Shows the information of a list of beacon events
	 * 
	 * @param beacons
	 *            List of beacon events
	 */
	void showBeaconsInfo(List<BeaconEvent> beacons);

	/**
	 * Shows the information of a beacon event
	 * 
	 * @param beacon
	 *            Beacon event
	 */
	void showBeaconInfo(BluetoothLeBeacon beacon);

	/**
	 * Creates a beacon using the advertiser options
	 * 
	 * @param options
	 *            Advertiser options
	 * @return Beacon
	 */
	BluetoothLeBeacon createBeacon(AdvertiserOptions options);

	/**
	 * Copy a beacon
	 * 
	 * @param beacon
	 *            Beacon
	 * @return Copied beacon
	 */
	BluetoothLeBeacon copyBeacon(BluetoothLeBeacon beacon);

	/**
	 * Add 'beacon' information to 'payload'. It calls addProtocolSpecificMetrics to
	 * add specific information
	 * 
	 * @param payload
	 *            Payload
	 * @param beacon
	 *            Beacon
	 */
	default void addMetrics(KuraPayload payload, BluetoothLeBeacon beacon) {
		addProtocolSpecificMetrics(payload, beacon);
		payload.addMetric(PROTOCOL, getProtocol().name());
		payload.addMetric(ADDRESS, beacon.getAddress());
		payload.addMetric(TXPOWER, getTxPower(beacon));
		payload.addMetric(RSSI, beacon.getRssi());
	}

	/**
	 * Return beacon TxPower
	 * 
	 * @param beacon
	 *            Beacon
	 * @return TxPower
	 */
	int getTxPower(BluetoothLeBeacon beacon);

	/**
	 * Add 'beacon' specific information to 'payload'
	 * 
	 * @param payload
	 *            Payload
	 * @param beacon
	 *            Beacon
	 */
	void addProtocolSpecificMetrics(KuraPayload payload, BluetoothLeBeacon beacon);

	/**
	 * Creates a beacon using 'payload'. It calls readProtocolSpecificMetrics to
	 * create the beacon
	 * 
	 * @param payload
	 *            Payload
	 * @return Created beacon
	 */
	default BluetoothLeBeacon readMetrics(KuraPayload payload) {
		BluetoothLeBeacon beacon = readProtocolSpecificMetrics(payload);
		beacon.setAddress((String) payload.getMetric(ADDRESS));
		beacon.setRssi((int) payload.getMetric(RSSI));
		return beacon;
	}

	/**
	 * Creates a beacon using 'payload'
	 * 
	 * @param payload
	 * @return
	 */
	BluetoothLeBeacon readProtocolSpecificMetrics(KuraPayload payload);

	/**
	 * Encodes a 'beacon' to KuraPayload
	 * 
	 * @param beacon
	 *            Beacon to encode
	 * @return Encoded beacon
	 */
	default KuraPayload encode(BluetoothLeBeacon beacon) {
		return BeaconEvent.encode(beacon);
	}

	/**
	 * Decodes a beacon event from 'payload'
	 * 
	 * @param payload
	 *            Payload
	 * @return Beacon event
	 */
	default BeaconEvent decode(KuraPayload payload) {
		return BeaconEvent.decode(getProtocol(), payload);
	}

	/**
	 * Converts a beacon to String. It calls toStringProtocolSpecificFields
	 * 
	 * @param beacon
	 *            Beacon
	 * @return Converted beacon
	 */
	default String beaconToString(BluetoothLeBeacon beacon) {
		List<String> fields = toStringProtocolSpecificFields(beacon);
		fields.add(PROTOCOL + VALUE_SEPARATOR + getProtocol().toString());
		fields.add(ADDRESS + VALUE_SEPARATOR + beacon.getAddress());
		fields.add(TXPOWER + VALUE_SEPARATOR + getTxPower(beacon));
		fields.add(RSSI + VALUE_SEPARATOR + beacon.getRssi());
		return fields.stream().collect(Collectors.joining(METRIC_SEPARATOR));
	}

	/**
	 * Reads specific beacon fields
	 * 
	 * @param beacon
	 *            Beacon
	 * @return The list of fields in String format
	 */
	List<String> toStringProtocolSpecificFields(BluetoothLeBeacon beacon);

	/**
	 * Converts a String to a Beacon. It calls fromStringProtocolSpecificFields
	 * 
	 * @param string
	 *            String
	 * @return Beacon
	 */
	default BluetoothLeBeacon beaconFromString(String string) {
		Map<String, String> fields = Arrays.asList(string.split(METRIC_SEPARATOR))
				.stream()
				.map(element -> element.split(VALUE_SEPARATOR))
				.collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
		BluetoothLeBeacon beacon = fromStringProtocolSpecificFields(fields);
		beacon.setAddress(fields.get(ADDRESS));
		beacon.setRssi(Integer.parseInt(fields.get(RSSI)));
		return beacon;
	}

	/**
	 * Converts 'fields' to a Beacon
	 * 
	 * @param fields
	 *            Beacon's fields
	 * @return Beacon
	 */
	BluetoothLeBeacon fromStringProtocolSpecificFields(Map<String, String> fields);

	default String bytesArrayToHexString(byte[] bytes) {
		return IntStream
				.range(0, bytes.length)
				.mapToObj(i -> String.format("%02X", bytes[i]))
				.collect(Collectors.joining());
	}

	default byte[] hexToByteArray(String hex) {
		char[] chars = hex.toCharArray();
		byte[] result = new byte[chars.length / 2];
		for (int i = 0; i < chars.length; i += 2) {
			result[i / 2] = hexToByte(chars[i], chars[i + 1]);
		}
		return result;
	}

	default byte hexToByte(char l, char r) {
		return (byte) ((byte) (digit(l) << 4) + digit(r));
	}

	default byte digit(char v) {
		return (byte) Character.digit(v, 16);
	}

}
