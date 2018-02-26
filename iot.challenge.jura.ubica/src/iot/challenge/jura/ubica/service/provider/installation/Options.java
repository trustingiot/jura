package iot.challenge.jura.ubica.service.provider.installation;

import java.util.Map;

/**
 * InstallationService options
 */
public class Options extends iot.challenge.jura.util.Options {

	public static final String PROPERTY_APPLICATION = "application";
	public static final String PROPERTY_INSTALLATIONS = "installations";
	public static final String PROPERTY_INSTALLATION_TOPIC_PREFIX = "installation.topic.prefix";
	public static final String PROPERTY_SCANNER_TOPIC_PREFIX = "scanner.topic.prefix";
	public static final String PROPERTY_MQTT_PUBLISH = "mqtt.publish";
	public static final String PROPERTY_RETENTION_TIME = "retention.time";

	public static final String PROPERTY_APPLICATION_DEFAULT = "jura";
	public static final String PROPERTY_INSTALLATIONS_DEFAULT = "";
	public static final String PROPERTY_INSTALLATION_TOPIC_PREFIX_DEFAULT = "installation";
	public static final String PROPERTY_SCANNER_TOPIC_PREFIX_DEFAULT = "scanner";
	public static final Boolean PROPERTY_MQTT_PUBLISH_DEFAULT = false;
	public static final int PROPERTY_RETENTION_TIME_DEFAULT = 60;

	protected final String application;

	protected final String installations;

	protected final String installationTopicPrefix;

	protected final String scannerTopicPrefix;

	protected final Boolean mqttPublish;

	protected final int retentionTime;

	public Options(Map<String, Object> properties) {
		super(properties);
		application = read(PROPERTY_APPLICATION, PROPERTY_APPLICATION_DEFAULT);
		installations = read(PROPERTY_INSTALLATIONS, PROPERTY_INSTALLATIONS_DEFAULT);
		installationTopicPrefix = read(PROPERTY_INSTALLATION_TOPIC_PREFIX, PROPERTY_INSTALLATION_TOPIC_PREFIX_DEFAULT);
		scannerTopicPrefix = read(PROPERTY_SCANNER_TOPIC_PREFIX, PROPERTY_SCANNER_TOPIC_PREFIX_DEFAULT);
		mqttPublish = read(PROPERTY_MQTT_PUBLISH, PROPERTY_MQTT_PUBLISH_DEFAULT);
		retentionTime = read(PROPERTY_RETENTION_TIME, PROPERTY_RETENTION_TIME_DEFAULT);
	}

	public String getApplication() {
		return application;
	}

	public String getInstallations() {
		return installations;
	}

	public String getInstallationTopicPrefix() {
		return installationTopicPrefix;
	}

	public String getScannerTopicPrefix() {
		return scannerTopicPrefix;
	}

	public Boolean getMqttPublish() {
		return mqttPublish;
	}

	public int getRetentionTime() {
		return retentionTime;
	}

}
