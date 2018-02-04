package iot.challenge.jura.faro.advertiser;

import java.util.Map;

import iot.challenge.jura.faro.Options;

/**
 * Advertiser options
 */
public class AdvertiserOptions extends Options {

	private static final String PROPERTY_MIN_INTERVAL = "min.beacon.interval";
	private static final String PROPERTY_MAX_INTERVAL = "max.beacon.interval";
	private static final String PROPERTY_TX_POWER = "tx.power";

	private static final int PROPERTY_MIN_INTERVAL_DEFAULT = 1000;
	private static final int PROPERTY_MAX_INTERVAL_DEFAULT = 1000;
	private static final int PROPERTY_TX_POWER_DEFAULT = 0;

	protected final Integer minInterval;

	protected final Integer maxInterval;

	protected final Integer txPower;

	public AdvertiserOptions(Map<String, Object> properties) {
		super(properties);
		minInterval = (int) (read(PROPERTY_MIN_INTERVAL, PROPERTY_MIN_INTERVAL_DEFAULT) / 0.625);
		maxInterval = (int) (read(PROPERTY_MAX_INTERVAL, PROPERTY_MAX_INTERVAL_DEFAULT) / 0.625);
		txPower = read(PROPERTY_TX_POWER, PROPERTY_TX_POWER_DEFAULT);
	}

	public Integer getMinInterval() {
		return minInterval;
	}

	public Integer getMaxInterval() {
		return maxInterval;
	}

	public Integer getTxPower() {
		return txPower;
	}
}
