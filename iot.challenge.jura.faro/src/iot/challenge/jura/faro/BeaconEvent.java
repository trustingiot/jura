package iot.challenge.jura.faro;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.message.KuraPayload;

/**
 * Represents a BeaconEvent (Dated BluetoothLeBeacon)
 */
public class BeaconEvent implements Comparable<BeaconEvent> {

	public static final String TIME = "time";
	public static final String BEACON = "beacon";

	private Long time;

	private BluetoothLeBeacon beacon;

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public BluetoothLeBeacon getBeacon() {
		return beacon;
	}

	private BeaconEvent() {
		super();
	}

	public BeaconEvent(BluetoothLeBeacon beacon) {
		this();
		this.beacon = beacon;
		this.time = System.currentTimeMillis();
	}

	public BeaconEvent(BluetoothLeBeacon beacon, long time) {
		this();
		this.beacon = beacon;
		this.time = time;
	}

	/**
	 * Encodes to KuraPayload
	 * 
	 * @return Encoded BeaconEvent
	 */
	public KuraPayload encode() {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());
		Protocol.readProtocol(beacon).addMetrics(payload, beacon);
		return payload;
	}

	/**
	 * Creates a BeaconEvent using 'beacon' and encodes it to KuraPayload
	 * 
	 * @param beacon
	 *            Beacon to encode
	 * @return Encoded BeaconEvent
	 */
	public static KuraPayload encode(BluetoothLeBeacon beacon) {
		BeaconEvent event = new BeaconEvent(beacon);
		return event.encode();
	}

	/**
	 * Decodes from KuraPayload
	 * 
	 * @param protocol
	 *            Beacon protocol
	 * @param payload
	 *            Encoded beacon
	 * @return Decoded BeaconEvent
	 */
	public static BeaconEvent decode(Protocol protocol, KuraPayload payload) {
		BluetoothLeBeacon beacon = protocol.readMetrics(payload);
		return new BeaconEvent(beacon, payload.getTimestamp().getTime());
	}

	/**
	 * Decodes from KuraPayload
	 * 
	 * @param payload
	 *            Encoded beacon
	 * @return Decoded BeaconEvent
	 */
	public static BeaconEvent decode(KuraPayload payload) {
		return decode(Protocol.readProtocol(payload), payload);
	}

	/**
	 * Copy the current instance
	 * 
	 * @return Copied instance
	 */
	public BeaconEvent copy() {
		BeaconEvent event = new BeaconEvent();
		event.time = new Long(time);
		event.beacon = Protocol.copyBeacon(beacon);
		return event;
	}

	@Override
	public String toString() {
		return toJson().toString();
	}

	public JsonObject toJson() {
		return new JsonObject()
				.add(TIME, time)
				.add(BEACON, Protocol.beaconToString(beacon));
	}

	public static BeaconEvent fromString(String event) {
		return fromJson(Json.parse(event));
	}

	public static BeaconEvent fromJson(JsonValue value) {
		JsonObject json = value.asObject();
		BeaconEvent result = new BeaconEvent();
		result.time = json.get(TIME).asLong();
		result.beacon = Protocol.beaconFromString(json.get(BEACON).asString());
		return result;
	}

	/**
	 * Converts a list of events to an array of bytes. It's symmetric to readBeacons
	 * 
	 * @param beacons
	 *            List of events
	 * @return Array of bytes
	 */
	public static byte[] toByteArray(List<BeaconEvent> beacons) {
		JsonArray array = new JsonArray();
		beacons.stream()
				.map(BeaconEvent::toJson)
				.forEach(array::add);
		return array.toString().getBytes();
	}

	/**
	 * Reads the beacons of payload's body
	 * 
	 * @param payload
	 *            Payload in whose body events have been sent.
	 * @return List of events
	 */
	public static List<BeaconEvent> readBeacons(KuraPayload payload) {
		return BeaconEvent.readBeacons(payload.getBody());
	}

	/**
	 * Converts an array of bytes to a list of events. It's symmetric to toByteArray
	 * 
	 * @param bytes
	 *            Array of bytes
	 * @return List of events
	 */
	public static List<BeaconEvent> readBeacons(byte[] bytes) {
		if (bytes != null && bytes.length > 0) {
			String message = new String(bytes);
			JsonArray array = Json.parse(message).asArray();

			return array.values().stream()
					.map(BeaconEvent::fromJson)
					.collect(Collectors.toList());

		} else {
			return new ArrayList<>();
		}
	}

	@Override
	public int compareTo(BeaconEvent other) {
		if (other == null)
			return 1;
		int result = time.compareTo(other.time);
		return (result == 0) ? beacon.getAddress().compareTo(other.getBeacon().getAddress()) : result;
	}
}
