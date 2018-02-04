package iot.challenge.jura.faro;

import java.util.Map;

import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconService;
import org.eclipse.kura.cloud.CloudService;
import org.osgi.service.component.ComponentContext;

import iot.challenge.jura.util.trait.ActionRecorder;

/**
 * Manager for a given configuration (WorkingMode & Protocol)
 */
public interface BeaconManager extends ActionRecorder {

	////
	//
	// Registered services
	//
	//
	void setBluetoothLeService(BluetoothLeService service);

	void unsetBluetoothLeService(BluetoothLeService service);

	void setBluetoothLeBeaconService(BluetoothLeBeaconService<?> service);

	void unsetBluetoothLeBeaconService(BluetoothLeBeaconService<?> service);

	void setCloudService(CloudService service);

	void unsetCloudService(CloudService service);

	////
	//
	// Service methods
	//
	//
	void activate(ComponentContext context, Map<String, Object> properties);

	void updated(ComponentContext context, Map<String, Object> properties);

	void deactivate(ComponentContext context);
}
