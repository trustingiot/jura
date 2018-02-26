package iot.challenge.jura.graba.web.servlet;

import iot.challenge.jura.graba.aggregator.Options;
import iot.challenge.jura.graba.aggregator.RecordingAggregatorService;

/**
 * {@link ActionableServiceServlet} to manage {@link RecordingAggregatorService}
 */
public class RecordingAggregatorServlet extends ActionableServiceServlet {

	private static final long serialVersionUID = -17411251301916950L;

	protected final String controlTopic = RecordingAggregatorService.CONTROL_TOPIC;

	protected final String[][] entries = new String[][] {
			{ "a", Options.PROPERTY_SUBSCRIPTION_A },
			{ "b", Options.PROPERTY_SUBSCRIPTION_B },
			{ "publication", Options.PROPERTY_PUBLICATION }
	};

	protected final String[][] fixedEntries = new String[][] {};

	public String getControlTopic() {
		return controlTopic;
	}

	public String[][] getEntries() {
		return entries;
	}

	public String[][] getFixedEntries() {
		return fixedEntries;
	}
}
