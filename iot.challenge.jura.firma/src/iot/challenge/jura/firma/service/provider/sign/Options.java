package iot.challenge.jura.firma.service.provider.sign;

import java.util.Map;

/**
 * SignService's options
 */
public class Options extends iot.challenge.jura.util.Options {

	public static final String PROPERTY_IDENTITY = "identity";
	public static final String PROPERTY_PASS = "pass";
	public static final String PROPERTY_GENERATE = "generate";

	public static final String PROPERTY_IDENTITY_DEFAULT = "jura";
	public static final String PROPERTY_PASS_DEFAULT = "jura";
	public static final boolean PROPERTY_GENERATE_DEFAULT = false;

	protected final String identity;

	protected final String pass;

	protected final boolean generate;

	public Options(Map<String, Object> properties) {
		super(properties);
		identity = read(PROPERTY_IDENTITY, PROPERTY_IDENTITY_DEFAULT);
		pass = read(PROPERTY_PASS, PROPERTY_PASS_DEFAULT);
		generate = read(PROPERTY_GENERATE, PROPERTY_GENERATE_DEFAULT);
	}

	public String getIdentity() {
		return identity;
	}

	public String getPass() {
		return pass;
	}

	public boolean isGenerate() {
		return generate;
	}

}
