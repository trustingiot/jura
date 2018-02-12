package iot.challenge.jura.ubica.service;

import java.util.Map;
import java.util.NavigableMap;

import iot.challenge.jura.ubica.installation.Point;

/**
 * Service for the location of BLE beacons
 */
public interface LocationService {

	/**
	 * Returns all locations of BLE beacons
	 * 
	 * @return Installation -> Time -> Beacon -> Point
	 */
	Map<String, NavigableMap<Long, Map<String, Point>>> getLocations();
}
