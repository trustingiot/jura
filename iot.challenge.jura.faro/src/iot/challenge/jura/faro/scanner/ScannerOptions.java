package iot.challenge.jura.faro.scanner;

import java.util.Map;

import iot.challenge.jura.faro.Options;

/**
 * Scanner options
 */
public class ScannerOptions extends Options {

	private static final String PROPERTY_APPLICATION_TOPIC = "application.topic";
	private static final String PROPERTY_TOPIC_PREFIX = "topic.prefix";
	private static final String PROPERTY_PUBLISH_PERIOD = "publish.period";
	private static final String PROPERTY_SCAN_INTERVAL = "scan.interval";

	private static final String PROPERTY_APPLICATION_TOPIC_DEFAULT = "jura";
	private static final String PROPERTY_TOPIC_PREFIX_DEFAULT = "scanner";
	private static final int PROPERTY_PUBLISH_PERIOD_DEFAULT = 10;
	private static final int PROPERTY_SCAN_INTERVAL_DEFAULT = 60;

	protected final String applicationTopic;

	protected final String topicPrefix;

	protected final int publishPeriod;

	protected final int scanInterval;

	public ScannerOptions(Map<String, Object> properties) {
		super(properties);
		applicationTopic = read(PROPERTY_APPLICATION_TOPIC, PROPERTY_APPLICATION_TOPIC_DEFAULT);
		topicPrefix = read(PROPERTY_TOPIC_PREFIX, PROPERTY_TOPIC_PREFIX_DEFAULT);
		publishPeriod = read(PROPERTY_PUBLISH_PERIOD, PROPERTY_PUBLISH_PERIOD_DEFAULT);
		scanInterval = read(PROPERTY_SCAN_INTERVAL, PROPERTY_SCAN_INTERVAL_DEFAULT);
	}

	public String getApplicationTopic() {
		return applicationTopic;
	}

	public String getTopicPrefix() {
		return topicPrefix;
	}

	public int getPublishPeriod() {
		return publishPeriod;
	}

	public int getScanInterval() {
		return scanInterval;
	}
}
