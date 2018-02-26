package iot.challenge.jura.ubica.service;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import iot.challenge.jura.faro.BeaconEvent;
import iot.challenge.jura.ubica.installation.Installation;

/**
 * Service for the management of installations
 */
public interface InstallationService {

	/**
	 * Returns installations
	 * 
	 * @return Installations
	 */
	Map<String, Installation> getInstallations();

	/**
	 * Returns all beacon's events
	 * 
	 * @return Beacon -> Time -> Scanner -> Event
	 */
	Map<String, NavigableMap<Long, Map<String, BeaconEvent>>> getEvents();

	/**
	 * Returns all beacon's events in a time window
	 * 
	 * @param start
	 *            Start of the time window
	 * @param end
	 *            End of the time window
	 * 
	 * @return Installation -> Beacon -> Scanner -> List<BeaconEvent> (older to
	 *         newer)
	 */
	Map<String, Map<String, Map<String, List<BeaconEvent>>>> getEventWindow(long start, long end);

	/**
	 * Returns a installations
	 * 
	 * @param id
	 *            Installation's id
	 * 
	 * @return Installation
	 */
	Installation getInstallation(String id);

	/**
	 * Adds a installation
	 * 
	 * @param installation
	 *            Installation to be added
	 */
	void addInstallation(Installation installation);

	/**
	 * Removes a installation
	 * 
	 * @param installation
	 *            Installation to be removed
	 */
	void removeInstallation(Installation installation);

	/**
	 * Modifies a installation
	 * 
	 * @param installation
	 *            Installation to be removed
	 */
	void modifyInstallation(Installation installation);
}
