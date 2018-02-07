package iot.challenge.jura.graba.aggregator;

import java.util.Map;

/**
 * Aggregator service options
 */
public class Options extends iot.challenge.jura.graba.Options {

	public static final String PROPERTY_SUBSCRIPTION_A = "subscription.a";
	public static final String PROPERTY_SUBSCRIPTION_B = "subscription.b";

	public static final String PROPERTY_SUBSCRIPTION_A_DEFAULT = "a";
	public static final String PROPERTY_SUBSCRIPTION_B_DEFAULT = "b";

	public static final String PROPERTY_PUBLICATION_DEFAULT = iot.challenge.jura.graba.recorder.Options.PROPERTY_PUBLICATION_DEFAULT
			+ "/aggregate";

	protected final String subscriptionA;
	protected final String subscriptionB;

	public Options(Map<String, Object> properties) {
		super(properties);
		subscriptionA = read(PROPERTY_SUBSCRIPTION_A, PROPERTY_SUBSCRIPTION_A_DEFAULT);
		subscriptionB = read(PROPERTY_SUBSCRIPTION_B, PROPERTY_SUBSCRIPTION_B_DEFAULT);
	}

	public String getSubscriptionA() {
		return subscriptionA;
	}

	public String getSubscriptionB() {
		return subscriptionB;
	}
}
