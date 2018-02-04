package iot.challenge.jura.faro.advertiser;

import java.util.Map;

import org.eclipse.kura.KuraBluetoothBeaconAdvertiserNotAvailable;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconAdvertiser;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconService;
import org.eclipse.kura.cloud.CloudService;
import org.osgi.service.component.ComponentContext;

import iot.challenge.jura.faro.BeaconHelper;
import iot.challenge.jura.faro.BeaconManager;

/**
 * Copy & adapted from
 * 
 * org.eclipse.kura.example.ibeacon.advertiser.IBeaconAdvertiser &
 * org.eclipse.kura.example.eddystone.advertiser.EddystoneAdvertiser
 */
public abstract class Advertiser implements BeaconManager, BeaconHelper {

	private static final String ID = "iot.challenge.jura.faro.advertiser";

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
	protected BluetoothLeBeaconAdvertiser advertiser;
	protected BluetoothLeAdapter adapter;
	protected AdvertiserOptions options;
	protected boolean activated;

	////
	//
	// Registered services
	//
	//
	protected BluetoothLeService bluetoothLeService;
	protected BluetoothLeBeaconService<?> bluetoothLeBeaconService;

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
		// Nothing to do
	}

	@Override
	public void unsetCloudService(CloudService service) {
		// Nothing to do
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
		performRegisteredAction("Deactivating", this::stopAdvertising);
	}

	////
	//
	// Functionality
	//
	//

	protected void activate(Map<String, Object> properties) {
		// Nothing to do
	}

	protected void update(Map<String, Object> properties) {
		options = createAdvertiserOptions(properties);
		stopAdvertising();
		startAdvertising();
	}

	@SuppressWarnings("unchecked")
	protected void stopAdvertising() {
		if (advertiser != null) {
			try {
				advertiser.stopBeaconAdvertising();
				bluetoothLeBeaconService.deleteBeaconAdvertiser(advertiser);
			} catch (KuraException e) {
				error("Stop beacon advertising failed", e);
			}
		}
		adapter = null;
	}

	protected void startAdvertising() {
		if (options.isEnable()) {
			obtainAdapter();
			if (adapter != null) {
				showAdapterInfo();
				activateAdapter();
				activateAdvertiser();
			} else {
				warn("No Bluetooth adapter found ...");
			}
		}
	}

	protected void obtainAdapter() {
		adapter = bluetoothLeService.getAdapter(options.getIname());
	}

	protected void showAdapterInfo() {
		info("Bluetooth adapter interface => {}", options.getIname());
		info("Bluetooth adapter address => {}", adapter.getAddress());
	}

	protected void activateAdapter() {
		if (!adapter.isPowered()) {
			info("Enabling bluetooth adapter...");
			adapter.setPowered(true);
		}
	}

	protected void activateAdvertiser() {
		try {
			advertiser = bluetoothLeBeaconService.newBeaconAdvertiser(adapter);
			configureBeacon();
		} catch (KuraBluetoothBeaconAdvertiserNotAvailable e) {
			error("Beacon advertiser not available on {}", adapter.getInterfaceName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	protected void configureBeacon() {
		try {
			BluetoothLeBeacon beacon = createBeacon(options);
			advertiser.updateBeaconAdvertisingData(beacon);
			advertiser.updateBeaconAdvertisingInterval(
					options.getMinInterval(),
					options.getMaxInterval());
			advertiser.startBeaconAdvertising();
		} catch (KuraException e) {
			error("Advertiser configuration failed", e);
		}
	}

}
