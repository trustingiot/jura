package iot.challenge.jura.faro.scanner;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconScanner;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconService;
import org.eclipse.kura.bluetooth.le.beacon.listener.BluetoothLeBeaconListener;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;

import iot.challenge.jura.faro.BeaconEvent;
import iot.challenge.jura.faro.BeaconHelper;
import iot.challenge.jura.faro.BeaconManager;

/**
 * Copy & adapted from
 * 
 * org.eclipse.kura.example.ibeacon.scanner.IBeaconScanner &
 * org.eclipse.kura.example.eddystone.scanner.EddystoneScanner
 */
public abstract class Scanner implements BeaconManager, BluetoothLeBeaconListener<BluetoothLeBeacon>, BeaconHelper {

	private static final String ID = "iot.challenge.jura.faro.scanner";

	////
	//
	// ActionRecorder trait
	//
	//
	@Override
	public String getID() {
		return ID;
	}

	////
	//
	// Parameters
	//
	//
	@SuppressWarnings("rawtypes")
	protected BluetoothLeBeaconScanner scanner;
	protected ExecutorService worker;
	protected Future<?> handle;
	protected BluetoothLeAdapter adapter;
	protected CloudClient cloudClient;
	protected Map<String, Long> publishTimes;
	protected Map<String, List<BeaconEvent>> events;
	protected ScannerOptions options;
	protected boolean activated;

	////
	//
	// Registered services
	//
	//
	protected BluetoothLeService bluetoothLeService;
	protected BluetoothLeBeaconService<?> bluetoothLeBeaconService;
	protected CloudService cloudService;

	@Override
	public void setBluetoothLeService(BluetoothLeService service) {
		bluetoothLeService = service;
	}

	@Override
	public void unsetBluetoothLeService(BluetoothLeService service) {
		bluetoothLeService = null;
	}

	@Override
	public void setBluetoothLeBeaconService(BluetoothLeBeaconService<?> service) {
		bluetoothLeBeaconService = service;
	}

	@Override
	public void unsetBluetoothLeBeaconService(BluetoothLeBeaconService<?> service) {
		bluetoothLeBeaconService = null;
	}

	@Override
	public void setCloudService(CloudService service) {
		cloudService = service;
	}

	@Override
	public void unsetCloudService(CloudService service) {
		cloudService = null;
	}

	////
	//
	// Service methods
	//
	//
	@Override
	public void activate(ComponentContext context, Map<String, Object> properties) {
		activated = true;
		performRegisteredAction("Activating", this::activate, properties);
	}

	@Override
	public void updated(ComponentContext context, Map<String, Object> properties) {
		if (!activated) {
			activate(context, properties);
			updated(context, properties);
		} else {
			performRegisteredAction("Updating", this::update, properties);
		}
	}

	@Override
	public void deactivate(ComponentContext context) {
		performRegisteredAction("Deactivating", this::deactivate);
	}

	////
	//
	// Functionality
	//
	//
	protected void activate(Map<String, Object> properties) {
		initialize();
	}

	protected void initialize() {
		publishTimes = new HashMap<>();
		events = new HashMap<>();
	}

	protected void update(Map<String, Object> properties) {
		stopScanning();
		options = new ScannerOptions(properties);
		startScanning();
	}

	protected void stopScanning() {
		releaseResources();
		shutdownWorker();
	}

	@SuppressWarnings("unchecked")
	protected void releaseResources() {
		if (scanner != null) {
			if (scanner.isScanning()) {
				scanner.stopBeaconScan();
			}
			scanner.removeBeaconListener(this);
			bluetoothLeBeaconService.deleteBeaconScanner(scanner);
			scanner = null;
		}
	}

	protected void shutdownWorker() {
		if (handle != null)
			handle.cancel(true);

		if (worker != null)
			worker.shutdown();
	}

	protected void startScanning() {
		connectToCloud();
		if (options.isEnable()) {
			worker = Executors.newSingleThreadExecutor();
			handle = worker.submit(this::setupScanner);
		}
	}

	protected void connectToCloud() {
		releaseExpiredCloudClient();
		if (cloudClient == null)
			createCloudClient();
	}

	protected void releaseExpiredCloudClient() {
		if (cloudClient != null && !options.getApplicationTopic().equals(cloudClient.getApplicationId())) {
			cloudClient.release();
			cloudClient = null;
		}
	}

	protected void createCloudClient() {
		try {
			cloudClient = cloudService.newCloudClient(options.getApplicationTopic());
		} catch (KuraException e) {
			error("Unable to get CloudClient", e);
			cloudClient = null;
		}
	}

	protected void setupScanner() {
		obtainAdapter();
		if (adapter != null) {
			showAdapterInfo();
			activateAdapter();
			activateScanner();
		} else {
			warn("No bluetooth adapter found ...");
		}
	}

	protected void obtainAdapter() {
		adapter = bluetoothLeService.getAdapter(options.getIname());
	}

	protected void showAdapterInfo() {
		info("Bluetooth adapter (interface: {}, address: {})", options.getIname(), adapter.getAddress());
	}

	@SuppressWarnings("unchecked")
	protected void activateScanner() {
		scanner = bluetoothLeBeaconService.newBeaconScanner(adapter);
		scanner.addBeaconListener(this);
		try {
			scanner.startBeaconScan(options.getScanInterval() * 1000L);
		} catch (KuraException e) {
			error("Beacon scanning failed", e);
		}
	}

	protected void activateAdapter() {
		if (!adapter.isPowered()) {
			info("Enabling bluetooth adapter...");
			adapter.setPowered(true);
		}
	}

	protected void deactivate() {
		stopScanning();
		releaseCloudConnection();
		publishTimes.clear();
		events.values().forEach(List::clear);
		events.clear();
	}

	protected void releaseCloudConnection() {
		if (cloudClient != null)
			cloudClient.release();
	}

	@Override
	public void onBeaconsReceived(BluetoothLeBeacon beacon) {
		long now = System.currentTimeMillis();
		String addr = beacon.getAddress();
		showBeaconInfo(beacon);
		saveBeacon(beacon);
		if (beaconMustBePublished(addr, now)) {
			publishBeacon(addr);
		}
	}

	protected void saveBeacon(BluetoothLeBeacon beacon) {
		saveBeaconEvent(new BeaconEvent(beacon));
	}

	protected void saveBeaconEvent(BeaconEvent event) {
		String addr = event.getBeacon().getAddress();
		List<BeaconEvent> beaconEvents = events.get(addr);
		if (beaconEvents == null) {
			beaconEvents = new ArrayList<>();
			events.put(addr, beaconEvents);
		}
		beaconEvents.add(event);
	}

	protected boolean beaconMustBePublished(String addr, long now) {
		Long last = publishTimes.get(addr);

		if (last == null || (now - last) > options.getPublishPeriod() * 1000L) {
			publishTimes.put(addr, now);
			return true;
		}

		return false;
	}

	protected void publishBeacon(String addr) {
		showBeaconsInfo(events.get(addr));
		publishBeacons(events.get(addr));
		events.get(addr).clear();
	}

	// Last in topic & all received in body
	protected void publishBeacons(List<BeaconEvent> beacons) {
		if (cloudClient != null) {
			BeaconEvent event = beacons.get(beacons.size() - 1);
			KuraPayload payload = event.encode();
			payload.setBody(BeaconEvent.toByteArray(beacons));

			try {
				cloudClient.publish(
						getCloudTopic(event.getBeacon()), // topic
						payload, // payload
						2, // QoS
						false); // retain
			} catch (KuraException e) {
				error("Unable to publish", e);
			}
		}
	}

	protected String getCloudTopic(BluetoothLeBeacon beacon) {
		return MessageFormat.format(
				"{0}/{1}/{2}",
				options.getTopicPrefix(),
				adapter.getAddress(),
				beacon.getAddress());
	}

}
