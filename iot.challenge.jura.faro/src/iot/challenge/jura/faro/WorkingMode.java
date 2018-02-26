package iot.challenge.jura.faro;

import java.util.Map;

/**
 * Beacon manager working mode
 */
public enum WorkingMode {
	scanner("scanner"), advertiser("advertiser");

	private String name;

	private WorkingMode(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static final String PROPERTY_BEACON_WORKING_MODE = "beacon.working.mode";
	public static final WorkingMode PROPERTY_BEACON_WORKING_MODE_DEFAULT = advertiser;

	/**
	 * Discover WorkingMode from properties
	 * 
	 * @param properties
	 *            Map of properties
	 * @return WorkingMode
	 */
	public static WorkingMode read(Map<String, Object> properties) {
		return valueOf(
				(String) properties.getOrDefault(PROPERTY_BEACON_WORKING_MODE,
						PROPERTY_BEACON_WORKING_MODE_DEFAULT.name()));
	}
}
