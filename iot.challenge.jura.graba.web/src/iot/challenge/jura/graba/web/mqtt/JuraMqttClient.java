package iot.challenge.jura.graba.web.mqtt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import iot.challenge.jura.util.trait.Loggable;

/**
 * Represents a MQTT client. It's a wrapper of a MqttAsyncClient of Paho
 * {@link MqttAsyncClient}
 */
public class JuraMqttClient implements Loggable, MqttCallback {

	protected boolean connected;

	protected final String brokerUrl;

	protected final String clientId;

	protected final MqttClientPersistence persistence;

	protected final MqttConnectOptions connectOptions;

	protected MqttAsyncClient client;

	protected long timeToWait;

	protected List<JuraMqttListener> listeners;

	public JuraMqttClient(String brokerUrl, String clientId, MqttConnectOptions connectOptions) {
		super();
		this.brokerUrl = brokerUrl;
		this.clientId = clientId;
		this.connectOptions = connectOptions;

		connected = false;
		timeToWait = connectOptions.getConnectionTimeout() * 2000L;
		persistence = new MemoryPersistence();

		listeners = new ArrayList<>();
	}

	public JuraMqttClient(String brokerUrl, String clientId, MqttConnectOptions connectOptions, boolean connect) {
		this(brokerUrl, clientId, connectOptions);
		if (connect) {
			buildClient();
			connect();
		}
	}

	public boolean isConnected() {
		return connected;
	}

	public String getBrokerUrl() {
		return brokerUrl;
	}

	public String getClientId() {
		return clientId;
	}

	public MqttClientPersistence getPersistence() {
		return persistence;
	}

	public MqttConnectOptions getConnectOptions() {
		return connectOptions;
	}

	public MqttAsyncClient getClient() {
		return client;
	}

	public long getTimeToWait() {
		return timeToWait;
	}

	/**
	 * Creates the MqttAsyncClient and set this instance as its callback listener
	 */
	public void buildClient() {
		try {
			client = new MqttAsyncClient(brokerUrl, clientId, persistence);
			client.setCallback(this);
		} catch (MqttException e) {
			error("Client instantiation failed", e);
			client = null;
		}
	}

	/**
	 * Connects to the broker
	 */
	public void connect() {
		if (client != null) {
			try {
				IMqttToken token = client.connect(connectOptions);
				token.waitForCompletion(getTimeToWait());
				connected = true;
			} catch (MqttException e) {
				warn("Connection failed: {}", e);
				connected = false;
			}
		}
	}

	/**
	 * Disconnects from the broker
	 */
	public void disconnect() {
		if (connected) {
			try {
				if (client.isConnected()) {
					client.disconnect().waitForCompletion(getTimeToWait());
				}
				connected = false;
				client.close();
				client = null;
			} catch (MqttException e) {
				warn("Disconnection failed: {}", e);
			}
		}
	}

	/**
	 * Adds 'listener' to MQTT client listeners
	 * 
	 * @param listener
	 *            Listener to be added
	 */
	public void addListener(JuraMqttListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes 'listener' from MQTT client listeners
	 * 
	 * @param listener
	 *            Listener to be removed
	 */
	public void removeListener(JuraMqttListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void connectionLost(Throwable arg0) {
		// Nothing to do
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// Nothing to do
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		listeners.forEach(it -> it.messageArrived(topic, message));
	}
}
