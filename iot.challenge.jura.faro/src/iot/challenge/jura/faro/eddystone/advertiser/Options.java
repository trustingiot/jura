package iot.challenge.jura.faro.eddystone.advertiser;

import java.util.Map;

/**
 * Copy & adapted from
 * org.eclipse.kura.example.eddystone.advertiser.EddystoneAdvertiserOptions
 */
public class Options extends iot.challenge.jura.faro.advertiser.AdvertiserOptions {

	protected static final String PROPERTY_TYPE = "eddystone.type";
	protected static final String PROPERTY_NAMESPACE = "eddystone.uid.namespace";
	protected static final String PROPERTY_INSTANCE = "eddystone.uid.instance";
	protected static final String PROPERTY_URL = "eddystone.url";

	protected static final String PROPERTY_TYPE_DEFAULT = "UID";
	protected static final String PROPERTY_NAMESPACE_DEFAULT = "00112233445566778899";
	protected static final String PROPERTY_INSTANCE_DEFAULT = "001122334455";
	protected static final String PROPERTY_URL_DEFAULT = "https://trusted827.wordpress.com/";

	protected final String eddystoneFrametype;
	
	protected final String uidNamespace;
	
	protected final String uidInstance;
	
	protected final String url;

	public Options(Map<String, Object> properties) {
		super(properties);
		eddystoneFrametype = read(PROPERTY_TYPE, PROPERTY_TYPE_DEFAULT);
		url = read(PROPERTY_URL, PROPERTY_URL_DEFAULT);
		uidNamespace = read(PROPERTY_NAMESPACE, PROPERTY_NAMESPACE_DEFAULT, (r, d) -> (r.length() == 20) ? r : d);
		uidInstance = read(PROPERTY_INSTANCE, PROPERTY_INSTANCE_DEFAULT, (r, d) -> (r.length() == 12) ? r : d);
	}
	
	public String getEddystoneFrametype() {
		return eddystoneFrametype;
	}

	public String getUidNamespace() {
		return uidNamespace;
	}

	public String getUidInstance() {
		return uidInstance;
	}

	public String getUrl() {
		return url;
	}
}
