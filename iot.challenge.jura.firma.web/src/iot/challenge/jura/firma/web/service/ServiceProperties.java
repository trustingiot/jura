package iot.challenge.jura.firma.web.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service properties
 */
// FIXME So ugly... -> guice?
public class ServiceProperties {

	public static final String PROPERTY_PASSWORD = "password";
	public static final String PROPERTY_SALT = "salt";

	public static final String PROPERTY_IOTA_SERVICE = "iot.challenge.jura.firma.iota";
	public static final String PROPERTY_SIGN_SERVICE = "iot.challenge.jura.firma.sign";

	public static final String PROPERTY_PASSWORD_DEFAULT = "password";
	public static final String PROPERTY_SALT_DEFAULT = "firma salt";

	private static ServiceProperties global = new ServiceProperties();

	private Map<String, Object> properties;

	private ServiceProperties() {
		properties = new HashMap<>();
	}

	public static ServiceProperties getGlobal() {
		return global;
	}

	public static Object getDefault(String property) {
		switch (property) {
		case PROPERTY_PASSWORD:
			return PROPERTY_PASSWORD_DEFAULT;

		case PROPERTY_SALT:
			return PROPERTY_SALT_DEFAULT;

		default:
			return null;
		}
	}

	public static void setProperties(Map<String, Object> properties) {
		global.setInstance(properties);
	}

	public void setInstance(Map<String, Object> properties) {
		this.properties = properties;
	}

	public static Object put(String property, Object value) {
		return global.putInstance(property, value);
	}

	public Object putInstance(String property, Object value) {
		properties.put(property, (value != null) ? value : getDefault(property));
		return properties.get(property);
	}

	public static Object get(String property) {
		return global.getInstance(property);
	}

	public Object getInstance(String property) {
		Object value = properties.get(property);
		return (value != null) ? value : getDefault(property);
	}

	public static <T> T get(String property, Class<T> clazz) {
		return global.getInstance(property, clazz);
	}

	public <T> T getInstance(String property, Class<T> clazz) {
		Object v = properties.get(property);
		if (v == null) {
			v = getDefault(property);
			if (v == null) {
				return null;
			}
		}

		return clazz.isAssignableFrom(v.getClass()) ? clazz.cast(v) : null;
	}

}
