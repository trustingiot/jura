package iot.challenge.jura.graba;

import java.time.Instant;
import java.util.Map;

/**
 * Graba service options
 */
public class Options extends iot.challenge.jura.util.Options {

	public static final String PROPERTY_APPLICATION = "application";
	public static final String PROPERTY_SUBSCRIPTION = "subscription";
	public static final String PROPERTY_PUBLICATION = "publication";
	public static final String PROPERTY_START_TIME = "start.time";

	public static final String PROPERTY_APPLICATION_DEFAULT = "jura";
	public static final String PROPERTY_SUBSCRIPTION_DEFAULT = "subscription";
	public static final String PROPERTY_PUBLICATION_DEFAULT = "publication";
	public static final Instant PROPERTY_START_TIME_DEFAULT = Instant.EPOCH;

	protected final String application;
	protected final String subscription;
	protected final String publication;
	protected final Instant startTime;

	public Options(Map<String, Object> properties) {
		super(properties);
		application = read(PROPERTY_APPLICATION, PROPERTY_APPLICATION_DEFAULT);
		subscription = read(PROPERTY_SUBSCRIPTION, PROPERTY_SUBSCRIPTION_DEFAULT);
		publication = read(PROPERTY_PUBLICATION, PROPERTY_PUBLICATION_DEFAULT);
		startTime = read(PROPERTY_START_TIME, PROPERTY_START_TIME_DEFAULT, (String source) -> Instant.parse(source));
	}

	public String getApplication() {
		return application;
	}

	public String getSubscription() {
		return subscription;
	}

	public String getPublication() {
		return publication;
	}

	public Instant getStartTime() {
		return startTime;
	}
}
