package iot.challenge.jura.graba.web.servlet;

import iot.challenge.jura.graba.player.Options;
import iot.challenge.jura.graba.player.PlayerService;

/**
 * {@link ActionableServiceServlet} to manage {@link PlayerServlet}
 */
public class PlayerServlet extends ActionableServiceServlet {

	private static final long serialVersionUID = 8596479029109665490L;

	protected final String controlTopic = PlayerService.CONTROL_TOPIC;

	protected final String[][] entries = new String[][] {
			{ "subscription", Options.PROPERTY_SUBSCRIPTION },
			{ "startTime", Options.PROPERTY_START_TIME }
	};

	protected final String[][] fixedEntries = new String[][] {
			{ Options.PROPERTY_PUBLICATION, Options.PROPERTY_PUBLICATION_DEFAULT }
	};

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
