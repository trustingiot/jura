package iot.challenge.jura.graba.web.servlet;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.message.KuraPayload;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import iot.challenge.jura.util.trait.Loggable;
import iot.challenge.jura.graba.web.service.ServiceProperties;

/**
 * HttpServlets with superpowers. It knows how to serialize Json in KuraPayload
 * (and vice versa) and can publish using the WebService MQTT client
 */
public abstract class JuraHttpServlet extends HttpServlet implements Loggable {

	private static final long serialVersionUID = -6907197036688790389L;

	protected Map<String, Long> timeouts;

	protected static CloudPayloadProtoBufDecoder getCloudPayloadProtobufDecoder() {
		return ServiceProperties.get(
				ServiceProperties.PROPERTY_CLOUD_PAYLOAD_PROTOBUF_DECODER,
				CloudPayloadProtoBufDecoder.class);
	}

	protected static CloudClient getCloudClient() {
		return ServiceProperties.get(
				ServiceProperties.PROPERTY_CLOUD_CLIENT,
				CloudClient.class);
	}

	protected static JsonObject readJson(HttpServletRequest request) throws IOException {
		try {
			return Json.parse(request.getReader()).asObject();
		} catch (NullPointerException npe) {
			return null;
		}
	}

	protected static KuraPayload jsonToKuraPayload(JsonObject message, String[][] entries) {
		return jsonToKuraPayload(message, entries, new String[][] {});
	}

	protected static KuraPayload jsonToKuraPayload(JsonObject message, String[][] entries, String[][] fixed) {
		Map<String, String> configuration = readMessage(message, entries);
		for (String[] f : fixed)
			configuration.put(f[0], f[1]);
		return mapToKuraPayload(configuration);
	}

	protected static Map<String, String> readMessage(JsonObject message, String[][] entries) {
		if (message == null)
			return null;

		Map<String, String> result = new HashMap<>();

		for (String[] entry : entries) {
			String value = readJsonString(message, entry[0]);
			if (value == null)
				return null;

			result.put(entry[1], value);
		}

		return result;
	}

	protected static String readJsonString(JsonObject object, String key) {
		JsonValue jsonValue = object.get(key);
		if (jsonValue == null)
			return null;

		String value = jsonValue.asString().trim();
		if (value.isEmpty())
			return null;

		return value;
	}

	protected static KuraPayload mapToKuraPayload(Map<String, String> values) {
		KuraPayload payload = null;
		if (values != null) {
			payload = new KuraPayload();
			payload.setTimestamp(new Date());
			values.forEach(payload::addMetric);
		}
		return payload;
	}

	protected static long getTimestamp(KuraPayload payload) {
		Date timestamp = payload.getTimestamp();
		return (timestamp != null) ? timestamp.getTime() : System.currentTimeMillis();
	}

	protected void publish(String topic, KuraPayload payload, int qos, boolean retain) {
		CloudClient cloudClient = ServiceProperties.get(
				ServiceProperties.PROPERTY_CLOUD_CLIENT,
				CloudClient.class);

		if (cloudClient != null) {
			try {
				cloudClient.publish(topic, payload, qos, retain);
			} catch (KuraException e) {
				error("Fail to publish", e);
			}
		}
	}

	protected void waitFor(String id, long timeout) {
		Long t;
		do {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			t = timeouts.get(id);
		} while (t != null && (System.currentTimeMillis() - t) < timeout);

		synchronized (this) {
			timeouts.remove(id);
		}
	}
}
