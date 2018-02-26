package iot.challenge.jura.ubica.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeacon;

import iot.challenge.jura.faro.BeaconEvent;
import iot.challenge.jura.ubica.installation.Installation;
import iot.challenge.jura.ubica.installation.Point;
import iot.challenge.jura.ubica.installation.Scanner;
import iot.challenge.jura.ubica.service.InstallationService;
import iot.challenge.jura.ubica.service.provider.simulation.Options;

/**
 * Simulates a BLE beacon moving in circles in an installation
 */
public class Simulation {

	public static final String INSTALLATION = "installation";
	public static final String SCANNER_1 = "00:00:00:00:00:01";
	public static final String SCANNER_2 = "00:00:00:00:00:02";
	public static final String SCANNER_3 = "00:00:00:00:00:03";
	public static final String SCANNER_4 = "00:00:00:00:00:04";

	public static final String BEACON = "BE:AC:01:00:00:00";

	public static final UUID BEACON_UUID = UUID.randomUUID();
	public static final short MAJOR = 1;
	public static final short MINOR = 1;
	public static final short TXPOWER = -68;

	public static final int SIZE = 10000;
	public static final int HALF_SIZE = SIZE / 2;

	private static final int X = 0;
	private static final int Y = 1;

	private InstallationService installationService;
	private Options options;
	private Installation installation;
	private Map<String, List<BeaconEvent>> events;
	private double[] position;
	private long time;
	private double radiansToRotate;

	private Simulation() {
		super();
	}

	public Simulation(InstallationService installationService, Options options) {
		this();
		this.installationService = installationService;
		this.options = options;
		this.radiansToRotate = (Math.PI / 180d) * options.getDegrees();
		simulate();
	}

	private void simulate() {
		synchronized (installationService) {
			createInstallation();
			createEvents();
			saveEvents();
		}
	}

	private void createInstallation() {
		installation = new Installation(INSTALLATION);

		installation.addPoint(new Point(0, 0));
		installation.addPoint(new Point(0, SIZE));
		installation.addPoint(new Point(SIZE, SIZE));
		installation.addPoint(new Point(SIZE, 0));
		installation.addPoint(new Point(0, 0));

		installation.addScanner(new Scanner(SCANNER_1, new Point(0, HALF_SIZE)));
		installation.addScanner(new Scanner(SCANNER_2, new Point(HALF_SIZE, SIZE)));
		installation.addScanner(new Scanner(SCANNER_3, new Point(SIZE, HALF_SIZE)));
		installation.addScanner(new Scanner(SCANNER_4, new Point(HALF_SIZE, 0)));

		installationService.addInstallation(installation);
	}

	private void createEvents() {
		position = new double[] { (int) SIZE * 0.9, (int) HALF_SIZE };

		int iterations = options.getRounds() * (360 / options.getDegrees());
		time = System.currentTimeMillis() + 15000;

		events = installation.getScanners()
				.stream()
				.map(Scanner::getAddr)
				.collect(Collectors.toMap(
						it -> it,
						it -> new ArrayList<BeaconEvent>()));

		for (int i = 0; i < iterations; i++) {
			generateEvents();
			rotatePosition();
		}
	}

	private void generateEvents() {
		List<Scanner> scanners = installation.getScanners();
		List<BeaconEvent> aux = scanners.stream()
				.map(Scanner::getPosition)
				.map(position -> new double[] { position.getX(), position.getY() })
				.map(this::distance)
				.map(this::rssi)
				.map(rssi -> {
					BluetoothLeIBeacon beacon = new BluetoothLeIBeacon(BEACON_UUID, MAJOR, MINOR, TXPOWER);
					beacon.setAddress(BEACON);
					beacon.setRssi(rssi);
					return new BeaconEvent(beacon, time + (long) (Math.random() * options.getBeaconInterval()));
				}).collect(Collectors.toList());

		for (int i = 0; i < aux.size(); i++) {
			events.get(scanners.get(i).getAddr()).add(aux.get(i));
		}

		time += options.getBeaconInterval();
	}

	private double distance(double[] p) {
		return distance(position, p);
	}

	private static double distance(double[] p1, double[] p2) {
		return Math.sqrt(Math.pow(p1[X] - p2[X], 2) + Math.pow(p1[Y] - p2[Y], 2));
	}

	private int rssi(double distance) {
		return rssi(distance / 1000d, TXPOWER);
	}

	private static int rssi(double distance, int txPower) {
		double linear = Math.pow(distance, 2);
		double ratio = (Math.log(linear) / Math.log(10d)) * 10d;
		int rssi = (int) Math.round(ratio);
		return txPower - rssi;
	}

	private void rotatePosition() {
		double result[] = new double[2];
		result[X] = (position[X] - HALF_SIZE) * Math.cos(radiansToRotate)
				- (position[Y] - HALF_SIZE) * Math.sin(radiansToRotate);
		result[Y] = (position[X] - HALF_SIZE) * Math.sin(radiansToRotate)
				+ (position[Y] - HALF_SIZE) * Math.cos(radiansToRotate);
		position[X] = result[X] + HALF_SIZE;
		position[Y] = result[Y] + HALF_SIZE;
	}

	private void saveEvents() {
		events.keySet().forEach(this::saveEvents);
	}

	private void saveEvents(String scanner) {
		saveEvents(scanner, BEACON, events.get(scanner));
	}

	// Copy & adapted from InstallationServiceProvider
	private void saveEvents(String scanner, String beacon, List<BeaconEvent> events) {
		synchronized (installationService) {
			Map<String, NavigableMap<Long, Map<String, BeaconEvent>>> installationEvents = installationService
					.getEvents();
			NavigableMap<Long, Map<String, BeaconEvent>> beaconEvents = installationEvents.get(beacon);
			if (beaconEvents == null) {
				beaconEvents = new TreeMap<Long, Map<String, BeaconEvent>>();
				installationEvents.put(beacon, beaconEvents);
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
}
