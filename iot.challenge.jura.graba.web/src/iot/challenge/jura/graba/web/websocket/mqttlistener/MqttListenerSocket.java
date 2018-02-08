package iot.challenge.jura.graba.web.websocket.mqttlistener;

import java.util.Date;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import iot.challenge.jura.util.trait.Loggable;
import iot.challenge.jura.graba.web.mqtt.DisposableMqttSubscriber;
import iot.challenge.jura.graba.web.mqtt.client.DisposableMqttListenerClient;
import iot.challenge.jura.graba.web.service.ServiceProperties;

/**
 * Socket capable of receiving MQTT messages
 */
public abstract class MqttListenerSocket extends WebSocketAdapter
		implements DisposableMqttListenerClient, Loggable {

	protected static final String MSG_TIMESTAMP = "timestamp";
	protected static final String MSG_BROKER_URL = "broker";
	protected static final String MSG_SUBSCRIPTION = "subscription";
	protected static final String MSG_TOPIC = "topic";

	protected String brokerUrl;

	protected String username;

	protected String password;

	protected String[] topics;

	protected int[] qos;

	protected String configurationTopic;

	protected DisposableMqttSubscriber disposableMqttSubscriber;

	private MqttListenerSocket() {
		super();
	}

	protected MqttListenerSocket(String brokerUrl, String userName, String password, String[] topics, int[] qos) {
		this();
		this.brokerUrl = brokerUrl;
		this.username = userName;
		this.password = password;
		this.topics = topics;
		this.qos = qos;
		this.configurationTopic = (topics != null && topics.length > 0) ? topics[0] : null;
	}

	public String getBrokerUrl() {
		return brokerUrl;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String[] getTopics() {
		return topics;
	}

	public int[] getQos() {
		return qos;
	}

	public String getConfigurationTopic() {
		return configurationTopic;
	}

	public DisposableMqttSubscriber getDisposableMqttSubscriber() {
		return disposableMqttSubscriber;
	}

	@Override
	public void onWebSocketConnect(Session session) {
		super.onWebSocketConnect(session);
		disposableMqttSubscriber = createDisposableMqttSubscriber();
		subscribe();
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		super.onWebSocketClose(statusCode, reason);
		unsubscribe();
		disposableMqttSubscriber = null;
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		super.onWebSocketError(cause);
		cause.printStackTrace(System.err);
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		if (isConnected()) {
			try {
				JsonObject object = Json.object();
				CloudPayloadProtoBufDecoder decoder = ServiceProperties.get(
						ServiceProperties.PROPERTY_CLOUD_PAYLOAD_PROTOBUF_DECODER,
						CloudPayloadProtoBufDecoder.class);
				KuraPayload payload = decoder.buildFromByteArray(message.getPayload());
				payload.metrics().forEach((k, v) -> object.add(k, v.toString()));
				object.add(MSG_TIMESTAMP, getTimestamp(payload));
				object.add(MSG_BROKER_URL, getBrokerUrl());
				object.add(MSG_SUBSCRIPTION, getConfigurationTopic());
				object.add(MSG_TOPIC, topic);
				getRemote().sendStringByFuture(object.toString());
			} catch (KuraException e) {
				error("Decoding fail", e);
			}
		}
	}

	private static long getTimestamp(KuraPayload payload) {
		Date timestamp = payload.getTimestamp();
		return (timestamp != null) ? timestamp.getTime() : System.currentTimeMillis();
	}
}
