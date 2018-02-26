package iot.challenge.jura.graba.player;

import java.util.Map;

/**
 * Player service options
 */
public class Options extends iot.challenge.jura.graba.Options {

	public static final String PROPERTY_PUBLICATION_DEFAULT = "playback";

	public Options(Map<String, Object> properties) {
		super(properties);
	}
}
