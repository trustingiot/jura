package iot.challenge.jura.firma.service.provider.transfer.anonymously;

import java.util.Map;

/**
 * TransferService's options for anonymously mode
 */
public class Options extends iot.challenge.jura.firma.service.provider.transfer.Options {

	public static final String PROPERTY_SALT = "salt";

	public static final String PROPERTY_SALT_DEFAULT = "firma salt";

	protected final String salt;

	public Options(Map<String, Object> properties) {
		super(properties);
		salt = read(PROPERTY_SALT, PROPERTY_SALT_DEFAULT);
	}

	public String getSalt() {
		return salt;
	}

}
