package iot.challenge.jura.ubica.service.provider.location;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;

import iot.challenge.jura.faro.BeaconEvent;
import iot.challenge.jura.ubica.installation.Point;
import iot.challenge.jura.ubica.service.InstallationService;
import iot.challenge.jura.ubica.service.LocationService;
import iot.challenge.jura.ubica.service.PositioningService;
import iot.challenge.jura.util.trait.ActionRecorder;

/**
 * LocationService provider
 */
public class LocationServiceProvider implements LocationService, ActionRecorder, ConfigurableComponent {

	////
	//
	// Action recorder
	//
	//
	public static final String ID = "iot.challenge.jura.ubica.location";

	@Override
	public String getID() {
		return ID;
	}

	////
	//
	// Parameters
	//
	//
	protected ScheduledExecutorService worker;
	private CloudClient cloudClient;
	protected Future<?> handle;
	protected Options options;
	protected GarbageCollector gc;

	protected Map<String, NavigableMap<Long, Map<String, Point>>> locations;

	////
	//
	// Location service
	//
	//
	@Override
	public Map<String, NavigableMap<Long, Map<String, Point>>> getLocations() {
		return locations;
	}

	////
	//
	// Registered services
	//
	//
	protected InstallationService installationService;
	protected PositioningService positioningService;
	protected CloudService cloudService;

	protected void setInstallationService(InstallationService service) {
		installationService = service;
	}

	protected void unsetInstallationService(InstallationService service) {
		installationService = null;
	}

	protected void setPositioningService(PositioningService service) {
		positioningService = service;
	}

	protected void unsetPositioningService(PositioningService service) {
		positioningService = null;
	}

	protected void setCloudService(CloudService service) {
		cloudService = service;
	}

	protected void unsetCloudService(CloudService service) {
		cloudService = null;
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
	protected void activate(Map<String, Object> properties) {
		locations = new HashMap<>();
		options = new Options(properties);
		gc = new GarbageCollector(this, options.getRetentionTime());
		connectToCloud();
	}

	private void connectToCloud() {
		if (cloudService != null) {
			try {
				cloudClient = cloudService.newCloudClient(options.getApplication());
			} catch (KuraException e) {
				error("Unable to get CloudClient", e);
				cloudClient = null;
			}
		}
	}

	protected void update(Map<String, Object> properties) {
		stopLocalization();
		options = new Options(properties);
		startLocalization();
	}

	protected void stopLocalization() {
		shutdownWorker();
	}

	protected void shutdownWorker() {
		if (handle != null)
			handle.cancel(true);

		if (worker != null)
			worker.shutdown();
	}

	protected void startLocalization() {
		if (options.isEnable()) {
			worker = Executors.newScheduledThreadPool(1);
			scheduleNextExecution();
		}
	}

	protected void scheduleNextExecution() {
		handle = worker.schedule(this::localization, options.getPublicationRate(), TimeUnit.MILLISECONDS);
	}

	protected void localization() {
		synchronized (installationService) {
			long current = System.currentTimeMillis();
			long end = current - (options.getDelay() * 1000l);
			long start = end - options.getScanningWindow();

			locate(current, start, end);

			scheduleNextExecution();
		}
	}

	protected void locate(long current, long start, long end) {
		Map<String, Map<String, Point>> locations = computeLocations(start, end);
		if (!locations.isEmpty()) {
			saveLocations(current, locations);
			notifyLocations(current, locations);
		}
	}

	// Installation -> Beacon -> Position
	protected Map<String, Map<String, Point>> computeLocations(long start, long end) {

		Map<String, Map<String, Point>> result = new HashMap<>();

		// Installation -> Beacon -> Scanner -> Queue<BeaconEvent>
		Map<String, Map<String, Map<String, List<BeaconEvent>>>> events = installationService.getEventWindow(start,
				end);

		// Detections by installations
		events.forEach((installation, detections) -> {
			Map<String, Point> installationPositions = new HashMap<>();

			// Detections by beacons in installation
			detections.forEach((beacon, beaconEvents) -> {

				// Beacon position in installation
				Point point = position(installation, beacon, beaconEvents, start, end);

				if (point != null)
					installationPositions.put(beacon, point);

			});

			if (!installationPositions.isEmpty())
				result.put(installation, installationPositions);

		});

		return result;
	}

	protected Point position(String installation, String beacon, Map<String, List<BeaconEvent>> events, long start,
			long end) {
		List<String> scannerAddr = new ArrayList<>();
		List<BeaconEvent> scannerEvents = new ArrayList<>();
		events.forEach((scanner, detections) -> {
			if (!detections.isEmpty()) {
				BeaconEvent resume = resumeBeaconEvents(detections, start, end);
				if (resume != null) {
					scannerAddr.add(scanner);
					scannerEvents.add(resume);
				}
			}
		});

		return positioningService.position(installation, scannerAddr, scannerEvents);
	}

	protected BeaconEvent resumeBeaconEvents(List<BeaconEvent> events, long start, long end) {
		BeaconEvent result = null;

		List<BeaconEvent> validEvents = removeOutliersEvents(events);
		if (!validEvents.isEmpty()) {
			int rssi = aggregateRssis(validEvents, start, end);
			result = validEvents.get(0).copy();
			result.getBeacon().setRssi(rssi);
		}

		return result;
	}

	private List<BeaconEvent> removeOutliersEvents(List<BeaconEvent> events) {
		List<Short> rssis = extractRssis(events);
		removeOutliers(rssis);

		List<BeaconEvent> normal = new ArrayList<>();
		int size = events.size();
		for (int i = 0; i < size; i++) {
			if (rssis.get(i) != null)
				normal.add(events.get(i));
		}

		return normal;
	}

	private static List<Short> extractRssis(List<BeaconEvent> events) {
		return events.stream()
				.map(BeaconEvent::getBeacon)
				.map(BluetoothLeBeacon::getRssi)
				.map(v -> (short) v.intValue())
				.collect(Collectors.toList());
	}

	private void removeOutliers(List<Short> total) {

		List<Short> partition = total.stream().filter(Objects::nonNull).collect(Collectors.toList());

		int size = total.size();
		int partitionSize = partition.size();
		int outliers = size - partitionSize;

		if (outliers < size) {
			double[] limits = computeLimits(partition, options.getCutoffRate());
			total.replaceAll(v -> (inRange(v, limits)) ? v : null);

			if (total.stream().filter(Objects::isNull).count() > outliers)
				removeOutliers(total);
		}
	}

	private static double[] computeLimits(List<Short> values, double cutoffRate) {
		double arithmeticAverage = arithmeticAverage(values);
		double typicalDeviation = typicalDeviation(values, arithmeticAverage);
		double factor = typicalDeviation * cutoffRate;
		return new double[] { arithmeticAverage - factor, arithmeticAverage + factor };
	}

	private static double arithmeticAverage(List<Short> values) {
		return averageSum(values, Short::shortValue);
	}

	private static double averageSum(List<Short> values, ToDoubleFunction<Short> f) {
		return sum(values, f) / (double) values.size();
	}

	private static double sum(List<Short> values, ToDoubleFunction<Short> f) {
		return values.stream().mapToDouble(f::applyAsDouble).sum();
	}

	private static double typicalDeviation(List<Short> values, double arithmeticAverage) {
		return Math.sqrt(averageSum(values, v -> Math.pow(v - arithmeticAverage, 2)));
	}

	private static boolean inRange(Short v, double[] limits) {
		return v != null && v >= limits[0] && v <= limits[1];
	}

	private int aggregateRssis(List<BeaconEvent> events, long start, long end) {
		List<Double> weights = computeWeights(events, start, end);
		List<Short> rssis = extractRssis(events);
		return (int) IntStream.range(0, rssis.size())
				.mapToDouble(i -> weights.get(i) * rssis.get(i))
				.sum();
	}

	private List<Double> computeWeights(List<BeaconEvent> events, long start, long end) {
		double window = end - start;
		List<Double> weights = events.stream()
				.map(BeaconEvent::getTime)
				.map(t -> end - t)
				.map(delay -> 1d - (((double) delay) / window))
				.map(w -> Math.pow(w, options.getAttenuation()))
				.collect(Collectors.toList());
		normalize(weights);
		return weights;
	}

	protected static void normalize(List<Double> values) {
		double sum = values.stream().mapToDouble(Double::doubleValue).sum();
		values.replaceAll(v -> v / sum);
	}

	protected void saveLocations(long current, Map<String, Map<String, Point>> locations) {
		locations.forEach((installation, detections) -> {
			NavigableMap<Long, Map<String, Point>> map = this.locations.get(installation);
			if (map == null) {
				map = new TreeMap<Long, Map<String, Point>>();
				this.locations.put(installation, map);
			}
			map.put(current, detections);
		});
	}

	protected void notifyLocations(long current, Map<String, Map<String, Point>> locations) {
		if (options.getMqttPublish()) {
			locations.forEach((installation, detections) -> {
				detections.forEach((beacon, point) -> {
					publishLocation(current, installation, beacon, point);
				});
			});
		}
	}

	protected void publishLocation(long current, String installation, String beacon, Point point) {
		try {
			cloudClient.publish(
					getCloudTopic(installation, beacon),
					generatePayload(current, point),
					1,
					false);
		} catch (KuraException e) {
			error("Unable to publish", e);
		}
	}

	protected String getCloudTopic(String installation, String beacon) {
		return MessageFormat.format(
				"{0}/{1}/{2}",
				options.getLocationTopicPrefix(),
				installation,
				beacon);
	}

	protected KuraPayload generatePayload(long current, Point point) {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date(current));
		payload.addMetric(Point.X, point.getX());
		payload.addMetric(Point.Y, point.getY());
		return payload;
	}

	protected void deactivate() {
		shutdownWorker();
		releaseCloudConnection();
		gc.cancel();
		clearLocations();
	}

	private void releaseCloudConnection() {
		if (cloudClient != null) {
			cloudClient.release();
			cloudClient = null;
		}
	}

	protected void clearLocations() {
		synchronized (this) {
			locations.values().forEach(map -> {
				map.values().forEach(Map::clear);
				map.clear();
			});
			locations.clear();
		}
	}
}
