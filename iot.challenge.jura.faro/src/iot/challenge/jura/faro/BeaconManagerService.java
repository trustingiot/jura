package iot.challenge.jura.faro;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconService;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;

/**
 * Manages BeaconManagers life cycle
 */
public class BeaconManagerService implements ConfigurableComponent {

	////
	//
	// Parameters
	//
	//
	protected Map<BeaconManagerDescriptor, BeaconManager> managers;
	protected BeaconManager currentManager;

	////
	//
	// Registered services
	//
	//
	protected BluetoothLeService bluetoothLeService;
	protected Map<Protocol, BluetoothLeBeaconService<?>> beaconServices;
	protected CloudService cloudService;

	public BeaconManagerService() {
		super();
		managers = new HashMap<>();
		beaconServices = new HashMap<>();
	}

	protected void setBluetoothLeService(BluetoothLeService service) {
		bluetoothLeService = service;
		managers.forEach((k, v) -> v.setBluetoothLeService(service));
	}

	protected void unsetBluetoothLeService(BluetoothLeService service) {
		bluetoothLeService = null;
		managers.forEach((k, v) -> v.unsetBluetoothLeService(service));
	}

	protected void setBluetoothLeIBeaconService(BluetoothLeBeaconService<?> service) {
		setBluetoothLeBeaconService(Protocol.iBeacon, service);
	}

	protected void unsetBluetoothLeIBeaconService(BluetoothLeBeaconService<?> service) {
		unsetBluetoothLeBeaconService(Protocol.iBeacon, service);
	}

	protected void setBluetoothLeEddystoneService(BluetoothLeBeaconService<?> service) {
		setBluetoothLeBeaconService(Protocol.Eddystone, service);
	}

	protected void unsetBluetoothLeEddystoneService(BluetoothLeBeaconService<?> service) {
		unsetBluetoothLeBeaconService(Protocol.Eddystone, service);
	}

	protected void setBluetoothLeBeaconService(Protocol protocol, BluetoothLeBeaconService<?> service) {
		Arrays.stream(WorkingMode.values())
				.forEach(workingMode -> setBluetoothLeBeaconService(protocol, workingMode, service));
	}

	protected void setBluetoothLeBeaconService(Protocol protocol, WorkingMode workingMode,
			BluetoothLeBeaconService<?> service) {
		beaconServices.put(protocol, service);
		BeaconManager manager = getBeaconManager(protocol, workingMode);
		if (manager != null)
			manager.setBluetoothLeBeaconService(service);
	}

	protected void unsetBluetoothLeBeaconService(Protocol protocol, BluetoothLeBeaconService<?> service) {
		Arrays.stream(WorkingMode.values())
				.forEach(workingMode -> unsetBluetoothLeBeaconService(protocol, workingMode, service));
	}

	protected void unsetBluetoothLeBeaconService(Protocol protocol, WorkingMode workingMode,
			BluetoothLeBeaconService<?> service) {
		beaconServices.remove(protocol);
		BeaconManager manager = getBeaconManager(protocol, workingMode);
		if (manager != null)
			manager.unsetBluetoothLeBeaconService(service);
	}

	protected void setCloudService(CloudService service) {
		cloudService = service;
		managers.forEach((k, v) -> v.setCloudService(service));
	}

	protected void unsetCloudService(CloudService service) {
		cloudService = null;
		managers.forEach((k, v) -> v.unsetCloudService(service));
	}

	protected BeaconManager getBeaconManager(Protocol protocol, WorkingMode workingMode) {
		return managers.get(BeaconManagerDescriptor.build(protocol, workingMode));
	}

	protected void setBeaconManager(Protocol protocol, WorkingMode workingMode, BeaconManager beaconManager) {
		managers.put(BeaconManagerDescriptor.build(protocol, workingMode), beaconManager);
	}

	////
	//
	// Service methods
	//
	//
	protected void activate(ComponentContext context, Map<String, Object> properties) {
		getSelectedManager(context, properties).activate(context, properties);
	}

	protected void updated(ComponentContext context, Map<String, Object> properties) {
		getSelectedManager(context, properties).updated(context, properties);
	}

	protected void deactivate(ComponentContext context, Map<String, Object> properties) {
		getSelectedManager(context, properties).deactivate(context);
	}

	////
	//
	// Functionality
	//
	//
	protected BeaconManager getSelectedManager(ComponentContext context, Map<String, Object> properties) {
		return setActiveManager(context, getManager(properties));
	}

	protected BeaconManager getManager(Map<String, Object> properties) {
		return getManager(Protocol.readProtocol(properties), WorkingMode.read(properties));
	}

	protected BeaconManager getManager(Protocol protocol, WorkingMode workingMode) {
		BeaconManager manager = getBeaconManager(protocol, workingMode);
		if (manager == null) {
			manager = protocol.createBeaconManager(workingMode);
			manager.setBluetoothLeService(bluetoothLeService);
			manager.setBluetoothLeBeaconService(beaconServices.get(protocol));
			manager.setCloudService(cloudService);
			setBeaconManager(protocol, workingMode, manager);
		}
		return manager;
	}

	protected BeaconManager setActiveManager(ComponentContext context, BeaconManager manager) {
		if (currentManager != null && currentManager != manager) {
			currentManager.deactivate(context);
		}
		currentManager = manager;
		return currentManager;
	}

}
