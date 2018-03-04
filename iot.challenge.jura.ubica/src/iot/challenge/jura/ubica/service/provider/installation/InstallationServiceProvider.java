package iot.challenge.jura.ubica.service.provider.installation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.eclipsesource.json.WriterConfig;

import iot.challenge.jura.faro.BeaconEvent;
import iot.challenge.jura.ubica.installation.Installation;
import iot.challenge.jura.ubica.service.InstallationService;
import iot.challenge.jura.util.MqttProcessor;
import iot.challenge.jura.util.trait.DataServiceAdapter;

/**
 * InstallationService provider
 */
public class InstallationServiceProvider implements InstallationService, DataServiceAdapter, ConfigurableComponent {

	////
	//
	// Action recorder
	//
	//
	public static final String ID = "iot.challenge.jura.ubica.installation";

	@Override
	public String getID() {
		return ID;
	}

	////
	//
	// Parameters
	//
	//
	private Map<String, Installation> installations;
	private CloudClient cloudClient;
	private Options options;
	private GarbageCollector gc;

	private Map<String, NavigableMap<Long, Map<String, BeaconEvent>>> events;

	public InstallationServiceProvider() {
		super();
		installations = new HashMap<>();
	}

	////
	//
	// Installation service
	//
	//
	@Override
	public Map<String, Installation> getInstallations() {
		return installations;
	}

	@Override
	public Map<String, NavigableMap<Long, Map<String, BeaconEvent>>> getEvents() {
		return events;
	}

	@Override
	public Map<String, Map<String, Map<String, List<BeaconEvent>>>> getEventWindow(long start, long end) {

		synchronized (this) {

			// Beacon -> Scanner -> List<BeaconEvent>
			Map<String, Map<String, List<BeaconEvent>>> events = getAllEventsInWindow(start, end);
			return mapEventsByInstallations(events);
		}
	}

	// Beacon -> Scanner -> List<BeaconEvent>
	protected Map<String, Map<String, List<BeaconEvent>>> getAllEventsInWindow(long start, long end) {

		// Beacon -> Scanner -> List<BeaconEvent>
		Map<String, Map<String, List<BeaconEvent>>> result = new HashMap<>();

		// Iterate over events
		events.forEach((beacon, beaconEvents) -> {

			// Events in window
			Collection<Map<String, BeaconEvent>> window = beaconEvents.subMap(start, true, end, true).values();

			if (!window.isEmpty()) {

				// Beacon detection in window
				// Scanner -> Queue<BeaconEvent>
				Map<String, List<BeaconEvent>> byBeacon = new HashMap<>();
				result.put(beacon, byBeacon);

				// Window is a collection of maps (each map contains all detections of a beacon
				// in an instant)
				window.stream().forEach(map -> {
					map.forEach((scanner, beaconEvent) -> {
						List<BeaconEvent> byScanner = byBeacon.get(scanner);
						if (byScanner == null) {
							byScanner = new ArrayList<>();
							byBeacon.put(scanner, byScanner);
						}
						byScanner.add(beaconEvent);
					});
				});
			}
		});

		return result;
	}

	// Installation -> Beacon -> Scanner -> List<BeaconEvent>
	protected Map<String, Map<String, Map<String, List<BeaconEvent>>>> mapEventsByInstallations(
			Map<String, Map<String, List<BeaconEvent>>> events) {

		Map<String, Map<String, Map<String, List<BeaconEvent>>>> result = new HashMap<>();

		installations.keySet().forEach(installation -> {
			Map<String, Map<String, List<BeaconEvent>>> eventsByInstallation = new HashMap<>();

			events.forEach((beacon, detections) -> {
				Map<String, List<BeaconEvent>> byInstallation = detections.entrySet().stream()
						.filter(entry -> hasScanner(installation, entry.getKey()))
						.collect(Collectors.toMap(Entry::getKey, it -> new ArrayList<>(it.getValue())));

				if (!byInstallation.isEmpty())
					eventsByInstallation.put(beacon, byInstallation);

			});

			if (!eventsByInstallation.isEmpty())
				result.put(installation, eventsByInstallation);

		});

		return result;
	}

	@Override
	public Installation getInstallation(String id) {
		return installations.get(id);
	}

	@Override
	public void addInstallation(Installation installation) {
		if (installation != null)
			installations.put(installation.getId(), installation);
	}

	@Override
	public void removeInstallation(Installation installation) {
		if (installation != null)
			installations.remove(installation.getId());
	}

	@Override
	public void modifyInstallation(Installation installation) {
		addInstallation(installation);
	}

	////
	//
	// Registered services
	//
	//
	protected CloudService cloudService;
	private DataService dataService;
	private CloudPayloadProtoBufDecoder decoder;
	protected ConfigurationService configurationService;

	protected void setCloudService(CloudService service) {
		cloudService = service;
	}

	protected void unsetCloudService(CloudService service) {
		cloudService = null;
	}

	protected void setDataService(DataService service) {
		dataService = service;
	}

	protected void unsetDataService(DataService service) {
		dataService = null;
	}

	protected void setCloudPayloadProtoBufDecoder(CloudPayloadProtoBufDecoder decoder) {
		this.decoder = decoder;
	}

	protected void unsetCloudPayloadProtoBufDecoder(CloudPayloadProtoBufDecoder decoder) {
		this.decoder = null;
	}

	protected void setConfigurationService(ConfigurationService service) {
		configurationService = service;
	}

	protected void unsetConfigurationService(ConfigurationService service) {
		configurationService = null;
	}

	////
	//
	// Service methods
	//
	//
	protected void activate(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Activating", this::activate, properties);
	}

	protected void updated(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Updating", this::update, properties);
	}

	protected void deactivate(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Deactivating", this::deactivate);
	}

	////
	//
	// Functionality
	//
	//
	private void activate(Map<String, Object> properties) {
		events = new HashMap<>();
		options = new Options(properties);
		gc = new GarbageCollector(this, options.getRetentionTime());
		connectToCloud();
	}

	private void connectToCloud() {
		if (cloudService != null) {
			try {
				cloudClient = cloudService.newCloudClient(options.getApplication());
				dataService.addDataServiceListener(this);
			} catch (KuraException e) {
				error("Unable to get CloudClient", e);
				cloudClient = null;
			}
			subscribeToScanners();
		}
	}

	private void subscribeToScanners() {
		if (dataService != null) {
			try {
				dataService.subscribe(getScannersTopic(), 2);
			} catch (KuraException e) {
				error("Unable to subscribe to data service", e);
				dataService.removeDataServiceListener(this);
			}
		}
	}

	private String getScannersTopic() {
		return MessageFormat.format("+/+/+/{0}/+/+", options.getScannerTopicPrefix());
	}

	private void update(Map<String, Object> properties) {
		synchronized (this) {
			boolean modified = setReadableInstallations(properties);

			// FIXME update application topic
			if (!modified) {
				Options previous = options;
				options = new Options(properties);
				updateInstallations();
				updateSubscription(previous, options);
			}
		}
	}

	// modified -> true, !modified -> false
	private boolean setReadableInstallations(Map<String, Object> properties) {
		Object value = properties.get(Options.PROPERTY_INSTALLATIONS);

		boolean doit = false;

		if (value != null) {
			String installations = (String) value;

			String test = installations.trim();
			if (!test.equals(installations)) {
				doit = true;
				installations = test;
			}

			if (!installations.isEmpty()) {
				try {
					test = readValidInstallations(installations).toString(WriterConfig.PRETTY_PRINT);
					if (!installations.equals(test)) {
						doit = true;
						installations = test;
					}
				} catch (ParseException e) {
					doit = true;
					installations = "";
				}
			}

			if (doit) {
				Map<String, Object> map = new HashMap<>(properties);
				map.put(Options.PROPERTY_INSTALLATIONS, installations);
				try {
					configurationService.updateConfiguration(ID, map);
				} catch (KuraException e) {
					error("Unable to update configuartion", e);
				}
			}
		}

		return doit;
	}

	private JsonArray readValidInstallations(String installations) throws ParseException {
		JsonValue json = Json.parse(installations);

		JsonArray array = null;
		if (json.isArray()) {
			array = json.asArray();
		} else {
			array = new JsonArray();
			array.add(json);
		}

		JsonArray result = new JsonArray();
		array.forEach(v -> {
			try {
				Installation installation = Installation.fromJson(v);
				result.add(installation.toJson());
			} catch (Exception e) {
			}
		});

		return result;
	}

	private void updateInstallations() {
		Map<String, Installation> map = readInstallations();

		// Removed
		diff(installations, map, Installation::getId).forEach(this::removeInstallation);

		// Modified
		find(installations, it -> !it.equals(map.get(it.getId())))
				.stream()
				.map(it -> map.get(it.getId()))
				.forEach(this::modifyInstallation);

		// Added
		diff(map, installations, Installation::getId).forEach(this::addInstallation);
	}

	private Map<String, Installation> readInstallations() {
		Map<String, Installation> result = new HashMap<>();
		String installations = options.getInstallations();
		if (!installations.isEmpty()) {
			readValidInstallations(options.getInstallations()).forEach(value -> {
				Installation installation = Installation.fromJson(value);
				result.put(installation.getId(), installation);
			});
		}
		return result;
	}

	private static <T, U> List<T> diff(Map<U, T> A, Map<U, T> B, Function<T, U> f) {
		return find(A, it -> !B.containsKey(f.apply(it)));
	}

	private static <T, U> List<T> find(Map<U, T> source, Predicate<T> predicate) {
		return source.values().stream()
				.filter(predicate::test)
				.collect(Collectors.toList());
	}

	private void updateSubscription(Options previous, Options current) {
		if (previous != null) {
			String pT = previous.getScannerTopicPrefix();
			String cT = current.getScannerTopicPrefix();

			// FIXME Unsubscribe from previous topic without affecting the system
			if (!pT.equals(cT))
				subscribeToScanners();
		}
	}

	private void deactivate() {
		releaseCloudConnection();
		gc.cancel();
		clearEvents();
	}

	private void releaseCloudConnection() {
		if (cloudClient != null) {
			cloudClient.release();
			cloudClient = null;
		}

		if (dataService != null) {
			dataService.removeDataServiceListener(this);
		}
	}

	protected void clearEvents() {
		synchronized (this) {
			events.values().forEach(map -> {
				map.values().forEach(Map::clear);
				map.clear();
			});
			events.clear();
		}
	}

	@Override
	public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
		synchronized (this) {
			info(topic);
			if (MqttProcessor.matches(getScannersTopic(), topic)) {
				try {
					String[] tokens = topic.split("/");
					String scanner = tokens[4];
					String beacon = tokens[5];
					KuraPayload message = decoder.buildFromByteArray(payload);
					notifyBeaconEvent(scanner, beacon, message, qos);
				} catch (Exception e) {
					error(e.getMessage());
				}
			}
		}
	}

	private void notifyBeaconEvent(String scanner, String beacon, KuraPayload message, int qos) {
		saveEvents(scanner, beacon, BeaconEvent.readBeacons(message));
		notifyEvents(scanner, beacon, message, qos);
	}

	protected void saveEvents(String scanner, String beacon, List<BeaconEvent> events) {
		synchronized (this) {
			NavigableMap<Long, Map<String, BeaconEvent>> beaconEvents = this.events.get(beacon);
			if (beaconEvents == null) {
				beaconEvents = new TreeMap<Long, Map<String, BeaconEvent>>();
				this.events.put(beacon, beaconEvents);
			}

			for (BeaconEvent event : events) {
				Long time = event.getTime();
				Map<String, BeaconEvent> instantEvents = beaconEvents.get(time);
				if (instantEvents == null) {
					instantEvents = new HashMap<String, BeaconEvent>();
					beaconEvents.put(time, instantEvents);
				}

				instantEvents.put(scanner, event);
			}
		}
	}

	protected void notifyEvents(String scanner, String beacon, KuraPayload message, int qos) {
		if (options.getMqttPublish()) {
			installations.keySet().forEach(installation -> {
				if (hasScanner(installation, scanner)) {
					publishBeaconEvent(installation, scanner, beacon, message, qos);
				}
			});
		}
	}

	protected boolean hasScanner(String installation, String scanner) {
		return installations.get(installation)
				.getScanners()
				.stream()
				.filter(it -> it.getAddr().equals(scanner))
				.findFirst()
				.isPresent();
	}

	protected void publishBeaconEvent(String installation, String scanner, String beacon, KuraPayload message,
			int qos) {
		try {
			cloudClient.publish(
					getCloudTopic(installation, scanner, beacon),
					message,
					qos,
					false);
		} catch (KuraException e) {
			error("Unable to publish", e);
		}
	}

	protected String getCloudTopic(String installation, String scanner, String beacon) {
		return MessageFormat.format(
				"{0}/{1}/{2}/{3}",
				options.getInstallationTopicPrefix(),
				installation,
				scanner,
				beacon);
	}
}
