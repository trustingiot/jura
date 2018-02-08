package iot.challenge.jura.graba;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import iot.challenge.jura.faro.BeaconEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

import org.eclipse.kura.message.KuraPayload;

/**
 * Represents a recording of beacon events
 */
public class Recording {

	public static final String RECORDING = "recording";

	public static final String TOPIC = "topic";
	public static final String START_TIME = "startTime";
	public static final String DURATION = "duration";
	public static final String EVENTS = "events";
	public static final String SCANNER = "scanner";
	public static final String SCANNER_EVENTS = "scannerEvents";

	private final String topic;

	private final Long startTime;

	private final Long duration;

	private Map<String, Queue<BeaconEvent>> events;

	public Recording(String topic, long startTime, long duration) {
		this.topic = topic;
		this.startTime = startTime;
		this.duration = duration;
		this.events = new HashMap<>();
	}

	public String getTopic() {
		return topic;
	}

	public Long getStartTime() {
		return startTime;
	}

	public Long getDuration() {
		return duration;
	}

	public Map<String, Queue<BeaconEvent>> getEvents() {
		return events;
	}

	/**
	 * Save all beacon events in 'payload' body
	 *
	 * @param scanner
	 *            Scanner
	 * @param payload
	 *            Payload
	 */
	public void save(String scanner, KuraPayload payload) {
		Queue<BeaconEvent> events = getScannerEvents(scanner);
		BeaconEvent.readBeacons(payload).stream()
				.filter(event -> event.getTime() >= startTime)
				.map(event -> {
					event.setTime(event.getTime() - startTime);
					return event;
				})
				.forEach(events::add);
	}

	/**
	 * Returns all beacon events detected by 'scanner'
	 *
	 * @param scanner
	 *            Scanner
	 * @return A priority queue of events.
	 *         {@link BeaconEvent#compareTo(BeaconEvent)}
	 */
	public Queue<BeaconEvent> getScannerEvents(String scanner) {
		Queue<BeaconEvent> scannerEvents = events.get(scanner);
		if (scannerEvents == null) {
			scannerEvents = new PriorityQueue<>();
			events.put(scanner, scannerEvents);
		}
		return scannerEvents;
	}

	@Override
	public String toString() {
		return toJson().toString();
	}

	public JsonObject toJson() {
		return new JsonObject()
				.add(TOPIC, topic)
				.add(START_TIME, startTime)
				.add(DURATION, duration)
				.add(EVENTS, toJson(events));
	}

	private static JsonArray toJson(Map<String, Queue<BeaconEvent>> events) {
		JsonArray json = new JsonArray();
		events.entrySet()
				.stream()
				.map(Recording::toJson)
				.forEach(json::add);
		return json;
	}

	private static JsonObject toJson(Map.Entry<String, Queue<BeaconEvent>> entry) {
		return new JsonObject()
				.add(SCANNER, entry.getKey())
				.add(SCANNER_EVENTS, toJson(entry.getValue()));
	}

	private static JsonArray toJson(Queue<BeaconEvent> scannerEvents) {
		JsonArray json = new JsonArray();
		scannerEvents.stream()
				.map(BeaconEvent::toJson)
				.forEach(json::add);
		return json;
	}

	public static Recording fromString(String message) {
		JsonObject json = Json.parse(message).asObject();
		Recording recording = new Recording(
				json.get(TOPIC).asString(),
				json.get(START_TIME).asLong(),
				json.get(DURATION).asLong());
		recording.events = readEvents(json.get(EVENTS).asArray());
		return recording;
	}

	private static Map<String, Queue<BeaconEvent>> readEvents(JsonArray events) {
		return events.values().stream()
				.map(JsonValue::asObject)
				.collect(Collectors.toMap(
						json -> json.get(SCANNER).asString(),
						json -> readScannerEvents(json.get(SCANNER_EVENTS).asArray())));
	}

	private static Queue<BeaconEvent> readScannerEvents(JsonArray scannerEvents) {
		return scannerEvents.values().stream()
				.map(BeaconEvent::fromJson)
				.collect(Collectors.toCollection(PriorityQueue::new));
	}

	/**
	 * Copy a recording
	 *
	 * @return Copied recording
	 */
	public Recording copy() {
		Recording recording = new Recording(topic, startTime, duration);
		events.keySet().forEach(scanner -> {
			Queue<BeaconEvent> queue = new PriorityQueue<>();
			recording.events.put(scanner, queue);
			events.get(scanner).stream()
					.map(BeaconEvent::copy)
					.forEach(queue::add);
		});
		return recording;
	}

	/**
	 * Aggregate a list of recordings into a single recording
	 *
	 * @param recordings
	 *            List of recordings
	 * @param topic
	 *            Recording topic
	 * @return Aggregated recording
	 */
	public static Recording aggregate(List<Recording> recordings, String topic) {
		return recordings.stream().reduce((a, b) -> aggregate(a, b, topic)).orElse(null);
	}

	/**
	 * Aggregate two recordings
	 *
	 * @param a
	 *            Recording a
	 * @param b
	 *            Recording a
	 * @param topic
	 *            Recording topic
	 * @return Aggregated recording
	 */
	public static Recording aggregate(Recording a, Recording b, String topic) {
		long startTime = a.startTime <= b.startTime ? a.startTime : b.startTime;
		long delayA = (a.startTime > b.startTime) ? a.startTime - b.startTime : 0;
		long delayB = (b.startTime > a.startTime) ? b.startTime - a.startTime : 0;
		long endTimeA = a.startTime + (a.duration * 1000L);
		long endTimeB = b.startTime + (b.duration * 1000L);
		long endTime = endTimeA >= endTimeB ? endTimeA : endTimeB;
		long duration = (endTime - startTime) / 1000L;
		Recording recording = new Recording(topic, startTime, duration);

		copyRecordingEvents(a, recording, delayA);
		copyRecordingEvents(b, recording, delayB);
		// Note: events is a PriorityQueue so it's not necessary to sort it
		return recording;
	}

	/**
	 * Copy recordings events from 'source' to 'target' with the specified 'delay'
	 * 
	 * @param source
	 *            Source recording
	 * @param target
	 *            Target recording
	 * @param delay
	 *            Events delay
	 */
	protected static void copyRecordingEvents(Recording source, Recording target, long delay) {
		source.events.keySet().forEach(scanner -> {
			Queue<BeaconEvent> queue = target.events.get(scanner);
			if (queue == null) {
				target.events.put(scanner, queue = new PriorityQueue<>());
			}

			source.events.get(scanner).stream()
					.map(be -> copyWithDelay(be, delay))
					.forEach(queue::add);
		});
	}

	/**
	 * Copy 'beaconEvent' with the specified 'delay'
	 * 
	 * @param beaconEvent
	 *            Beacon event
	 * @param delay
	 *            Event delay
	 * @return Copied event
	 */
	protected static BeaconEvent copyWithDelay(BeaconEvent beaconEvent, long delay) {
		BeaconEvent result = beaconEvent.copy();
		if (delay > 0) {
			result.setTime(beaconEvent.getTime() + delay);
		}
		return result;
	}
}
