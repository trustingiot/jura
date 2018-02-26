package iot.challenge.jura.ubica.service;

import java.util.List;

import iot.challenge.jura.faro.BeaconEvent;
import iot.challenge.jura.ubica.installation.Point;

/**
 * Service for the positioning of BLE beacons
 */
public interface PositioningService {

	/**
	 * Computes the position of a BLE beacon in an installation
	 * 
	 * @param installationId
	 *            Installation id
	 * @param scannersAddr
	 *            List of the scanner's addresses that have generated the list of
	 *            beacon's events
	 * @param events
	 *            List of beacon's events
	 * 
	 * @return BLE beacon position in installation
	 */
	Point position(String installationId, List<String> scannersAddr, List<BeaconEvent> events);
}
