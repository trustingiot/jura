package iot.challenge.jura.ubica.service.provider.simulation;

import java.util.Map;

/**
 * SimulationService options
 */
public class Options extends iot.challenge.jura.util.Options {

	public static final String PROPERTY_ENABLE = "enable";
	public static final String PROPERTY_DEGREES = "degrees";
	public static final String PROPERTY_ROUNDS = "rounds";
	public static final String PROPERTY_BEACON_INTERVAL = "beacon.interval";
	public static final String PROPERTY_DELAY = "delay";

	public static final boolean PROPERTY_ENABLE_DEFAULT = false;
	public static final int PROPERTY_DEGREES_DEFAULT = 6;
	public static final int PROPERTY_ROUNDS_DEFAULT = 3;
	public static final int PROPERTY_BEACON_INTERVAL_DEFAULT = 250;
	public static final int PROPERTY_DELAY_DEFAULT = 15000;

	protected final boolean enable;
	protected final int degrees;
	protected final int rounds;
	protected final int beaconInterval;
	protected final int delay;

	public Options(Map<String, Object> properties) {
		super(properties);
		enable = read(PROPERTY_ENABLE, PROPERTY_ENABLE_DEFAULT);
		degrees = read(PROPERTY_DEGREES, PROPERTY_DEGREES_DEFAULT);
		rounds = read(PROPERTY_ROUNDS, PROPERTY_ROUNDS_DEFAULT);
		beaconInterval = read(PROPERTY_BEACON_INTERVAL, PROPERTY_BEACON_INTERVAL_DEFAULT);
		delay = read(PROPERTY_DELAY, PROPERTY_DELAY_DEFAULT);
	}

	public boolean isEnable() {
		return enable;
	}

	public int getDegrees() {
		return degrees;
	}

	public int getRounds() {
		return rounds;
	}

	public int getBeaconInterval() {
		return beaconInterval;
	}

	public int getDelay() {
		return delay;
	}

}
