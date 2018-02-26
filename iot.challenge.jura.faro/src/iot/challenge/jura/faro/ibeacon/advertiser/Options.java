package iot.challenge.jura.faro.ibeacon.advertiser;

import java.util.Map;
import java.util.UUID;

import iot.challenge.jura.faro.advertiser.AdvertiserOptions;

/**
 * Copy & adapted from
 * org.eclipse.kura.example.ibeacon.advertiser.IBeaconAdvertiserOptions
 */
public class Options extends AdvertiserOptions {

	protected static final String PROPERTY_UUID = "uuid";
	protected static final String PROPERTY_MAJOR = "major";
	protected static final String PROPERTY_MINOR = "minor";

	protected static final String PROPERTY_UUID_DEFAULT = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
	protected static final int PROPERTY_MAJOR_DEFAULT = 0;
	protected static final int PROPERTY_MINOR_DEFAULT = 0;

	private final UUID uuid;
	
	private final int major;
	
	private final int minor;

	public Options(Map<String, Object> properties) {
		super(properties);
		major = read(PROPERTY_MAJOR, PROPERTY_MAJOR_DEFAULT);
		minor = read(PROPERTY_MINOR, PROPERTY_MINOR_DEFAULT);
		uuid = read(PROPERTY_UUID, PROPERTY_UUID_DEFAULT, (r, d) -> UUID.fromString(validUUID(r) ? r : d));
	}

	public UUID getUuid() {
		return uuid;
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	protected static boolean validUUID(String candidate) {
		return (candidate != null && candidate.trim().replace("-", "").length() == 32);
	}
}
