package iot.challenge.jura.graba.web.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service properties
 */
// FIXME So ugly... -> guice?
public class ServiceProperties {

	public static final String PROPERTY_APPLICATION = "application";
	public static final String PROPERTY_GRABA_APPLICATION = "graba.application";
	public static final String PROPERTY_FARO_APPLICATION = "faro.application";
	public static final String PROPERTY_MQTT_USER = "mqtt.user";
	public static final String PROPERTY_MQTT_PASSWORD = "mqtt.password";
	public static final String PROPERTY_CLOUD_CLIENT = "CloudClient";
	public static final String PROPERTY_CLOUD_PAYLOAD_PROTOBUF_DECODER = "CloudPayloadProtoBufDecoder";

	public static final String PROPERTY_APPLICATION_DEFAULT = "jura";
	public static final String PROPERTY_GRABA_APPLICATION_DEFAULT = "jura";
	public static final String PROPERTY_FARO_APPLICATION_DEFAULT = "jura";
	public static final String PROPERTY_MQTT_USER_DEFAULT = "mqtt";
	public static final String PROPERTY_MQTT_PASSWORD_DEFAULT = "mqtt";

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
		case PROPERTY_APPLICATION:
			return PROPERTY_APPLICATION_DEFAULT;

		case PROPERTY_GRABA_APPLICATION:
			return PROPERTY_GRABA_APPLICATION_DEFAULT;

		case PROPERTY_FARO_APPLICATION:
			return PROPERTY_FARO_APPLICATION_DEFAULT;

		case PROPERTY_MQTT_USER:
			return PROPERTY_MQTT_USER_DEFAULT;

		case PROPERTY_MQTT_PASSWORD:
			return PROPERTY_MQTT_PASSWORD_DEFAULT;

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
