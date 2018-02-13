package iot.challenge.jura.firma.service.provider.transferlocation;

import java.util.Map;

/**
 * TransferLocationService's options
 */
public class Options extends iot.challenge.jura.util.Options {

	public static final String PROPERTY_ENABLE = "enable";
	public static final String PROPERTY_UPDATE_RATE = "update.rate";
	public static final String PROPERTY_LOCATION_TIMEOUT = "location.timeout";
	public static final String PROPERTY_PUBLICATION_RATE = "publication.rate";

	public static final boolean PROPERTY_ENABLE_DEFAULT = true;
	public static final int PROPERTY_UPDATE_RATE_DEFAULT = 30;
	public static final int PROPERTY_LOCATION_TIMEOUT_DEFAULT = 600;
	public static final int PROPERTY_PUBLICATION_RATE_DEFAULT = 300;

	protected final boolean enable;
	protected final int updateRate;
	protected final int locationTimeout;
	protected final int publicationRate;

	public Options(Map<String, Object> properties) {
		super(properties);
		enable = read(PROPERTY_ENABLE, PROPERTY_ENABLE_DEFAULT);
		updateRate = read(PROPERTY_UPDATE_RATE, PROPERTY_UPDATE_RATE_DEFAULT);
		locationTimeout = read(PROPERTY_LOCATION_TIMEOUT, PROPERTY_LOCATION_TIMEOUT_DEFAULT);
		publicationRate = read(PROPERTY_PUBLICATION_RATE, PROPERTY_PUBLICATION_RATE_DEFAULT);
	}

	public boolean isEnable() {
		return enable;
	}

	public int getUpdateRate() {
		return updateRate;
	}

	public int getLocationTimeout() {
		return locationTimeout;
	}

	public int getPublicationRate() {
		return publicationRate;
	}

}
