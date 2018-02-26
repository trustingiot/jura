package iot.challenge.jura.faro;

import java.util.Map;

/**
 * Beacon manager options
 */
public abstract class Options extends iot.challenge.jura.util.Options {

	protected static final String PROPERTY_ENABLE = "enable";
	protected static final String PROPERTY_INAME = "iname";

	protected static final boolean PROPERTY_ENABLE_DEFAULT = false;
	protected static final String PROPERTY_INAME_DEFAULT = "hci0";

	protected final boolean enable;
	
	protected final String iname;

	public Options(Map<String, Object> properties) {
		super(properties);
		enable = read(PROPERTY_ENABLE, PROPERTY_ENABLE_DEFAULT);
		iname = read(PROPERTY_INAME, PROPERTY_INAME_DEFAULT);
	}

	public boolean isEnable() {
		return enable;
	}

	public String getIname() {
		return iname;
	}

}
