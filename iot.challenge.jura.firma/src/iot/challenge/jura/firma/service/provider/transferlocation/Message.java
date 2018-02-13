package iot.challenge.jura.firma.service.provider.transferlocation;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import iot.challenge.jura.ubica.installation.Point;
import jota.utils.TrytesConverter;

/**
 * Location's message
 */
public class Message {
	public static final String TOPIC_APP = "app";
	public static final String TOPIC_INSTALLATION = "installation";
	public static final String TOPIC_DEVICE = "device";
	public static final String TOPIC_LOCATION = "location";

	public static final String APP = "jura";

	private final String app;

	private final String installation;

	private final String device;

	private final Point location;

	public Message(String installation, String device, Point location) {
		this.app = APP;
		this.installation = installation;
		this.device = device;
		this.location = location;
	}

	public String getApp() {
		return app;
	}

	public String getInstallation() {
		return installation;
	}

	public String getDevice() {
		return device;
	}

	public Point getLocation() {
		return location;
	}

	public String toTrytes() {
		return TrytesConverter.toTrytes(toString());
	}

	@Override
	public String toString() {
		return toJson().toString();
	}

	public JsonObject toJson() {
		JsonObject message = new JsonObject();
		message.add(TOPIC_APP, APP);
		message.add(TOPIC_INSTALLATION, installation);
		message.add(TOPIC_DEVICE, device);
		message.add(TOPIC_LOCATION, location.toJson());
		return message;
	}

	public static Message fromTrytes(String trytes) {
		return fromString(TrytesConverter.toString(trytes));
	}

	public static Message fromString(String message) {
		return fromJson(Json.parse(message));
	}

	public static Message fromJson(JsonValue jsonValue) {
		JsonObject json = jsonValue.asObject();
		return new Message(
				json.get(TOPIC_INSTALLATION).asString(),
				json.get(TOPIC_DEVICE).asString(),
				Point.fromJson(json.get(TOPIC_LOCATION)));
	}
}
