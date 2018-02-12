package iot.challenge.jura.ubica.service.provider.location;

import java.util.Map;

/**
 * LocationService options
 */
public class Options extends iot.challenge.jura.util.Options {

	public static final String PROPERTY_ENABLE = "enable";
	public static final String PROPERTY_APPLICATION = "application";
	public static final String PROPERTY_LOCATION_TOPIC_PREFIX = "location.topic.prefix";
	public static final String PROPERTY_MQTT_PUBLISH = "mqtt.publish";
	public static final String PROPERTY_RETENTION_TIME = "retention.time";
	public static final String PROPERTY_PUBLICATION_RATE = "publication.rate";
	public static final String PROPERTY_DELAY = "delay";
	public static final String PROPERTY_SCANNING_WINDOW = "scanning.window";
	public static final String PROPERTY_ATTENUATION = "attenuation";
	public static final String PROPERTY_CUTOFF_RATE = "cutoff.rate";

	public static final boolean PROPERTY_ENABLE_DEFAULT = true;
	public static final String PROPERTY_APPLICATION_DEFAULT = "jura";
	public static final String PROPERTY_LOCATION_TOPIC_PREFIX_DEFAULT = "location";
	public static final Boolean PROPERTY_MQTT_PUBLISH_DEFAULT = false;
	public static final int PROPERTY_RETENTION_TIME_DEFAULT = 60;
	public static final int PROPERTY_PUBLICATION_RATE_DEFAULT = 500;
	public static final int PROPERTY_DELAY_DEFAULT = 15;
	public static final int PROPERTY_SCANNING_WINDOW_DEFAULT = 1000;
	public static final double PROPERTY_ATTENUATION_DEFAULT = 1.5;
	public static final double PROPERTY_CUTOFF_RATE_DEFAULT = 1.5;

	protected final boolean enable;

	protected final String application;

	protected final String locationTopicPrefix;

	protected final Boolean mqttPublish;

	protected final int retentionTime;

	protected final int publicationRate;

	protected final int delay;

	protected final int scanningWindow;

	protected final double attenuation;

	protected final double cutoffRate;

	public Options(Map<String, Object> properties) {
		super(properties);
		enable = read(PROPERTY_ENABLE, PROPERTY_ENABLE_DEFAULT);
		application = read(PROPERTY_APPLICATION, PROPERTY_APPLICATION_DEFAULT);
		locationTopicPrefix = read(PROPERTY_LOCATION_TOPIC_PREFIX, PROPERTY_LOCATION_TOPIC_PREFIX_DEFAULT);
		mqttPublish = read(PROPERTY_MQTT_PUBLISH, PROPERTY_MQTT_PUBLISH_DEFAULT);
		retentionTime = read(PROPERTY_RETENTION_TIME, PROPERTY_RETENTION_TIME_DEFAULT);
		publicationRate = read(PROPERTY_PUBLICATION_RATE, PROPERTY_PUBLICATION_RATE_DEFAULT);
		delay = read(PROPERTY_DELAY, PROPERTY_DELAY_DEFAULT);
		scanningWindow = read(PROPERTY_SCANNING_WINDOW, PROPERTY_SCANNING_WINDOW_DEFAULT);
		attenuation = read(PROPERTY_ATTENUATION, PROPERTY_ATTENUATION_DEFAULT, (Integer r) -> r / 100d);
		cutoffRate = read(PROPERTY_CUTOFF_RATE, PROPERTY_CUTOFF_RATE_DEFAULT, (Integer r) -> r / 100d);
	}

	public boolean isEnable() {
		return enable;
	}

	public String getApplication() {
		return application;
	}

	public String getLocationTopicPrefix() {
		return locationTopicPrefix;
	}

	public Boolean getMqttPublish() {
		return mqttPublish;
	}

	public int getRetentionTime() {
		return retentionTime;
	}

	public int getPublicationRate() {
		return publicationRate;
	}

	public int getDelay() {
		return delay;
	}

	public int getScanningWindow() {
		return scanningWindow;
	}

	public double getAttenuation() {
		return attenuation;
	}

	public double getCutoffRate() {
		return cutoffRate;
	}

}
