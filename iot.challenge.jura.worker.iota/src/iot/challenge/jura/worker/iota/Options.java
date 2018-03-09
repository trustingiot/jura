package iot.challenge.jura.worker.iota;

import java.util.Map;

/**
 * Worker's options
 */
public class Options extends iot.challenge.jura.util.Options {

	public static final String PROPERTY_ID = "id";

	public static final String PROPERTY_ID_DEFAULT = "worker";

	protected final String id;

	public Options(Map<String, Object> properties) {
		super(properties);
		id = read(PROPERTY_ID, PROPERTY_ID_DEFAULT);
	}

	public String getID() {
		return id;
	}
}