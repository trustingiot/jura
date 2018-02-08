package iot.challenge.jura.graba.web.mqtt.client;

import iot.challenge.jura.graba.web.mqtt.DisposableMqttSubscriber;
import iot.challenge.jura.graba.web.mqtt.JuraMqttListener;

/**
 * Interface to be implemented by the classes that wish to subscribe to a
 * {@link DisposableMqttSubscriber}
 */
public interface DisposableMqttListenerClient extends MqttListenerClient, JuraMqttListener {

	DisposableMqttSubscriber getDisposableMqttSubscriber();

	String getBrokerUrl();

	String getUsername();

	String getPassword();

	/**
	 * Creates a {@link DisposableMqttSubscriber} using the instance configuration
	 * 
	 * @return Created subscriber
	 */
	default DisposableMqttSubscriber createDisposableMqttSubscriber() {
		return new DisposableMqttSubscriber(
				getBrokerUrl(),
				getUsername(),
				getPassword(),
				getTopics(),
				getQos());
	}

	/**
	 * Subscribes to {@link MqttListenerClient#getTopics()}
	 */
	default void subscribe() {
		DisposableMqttSubscriber subscriber = getDisposableMqttSubscriber();
		if (subscriber != null && subscriber.isConnected()) {
			subscriber.addListener(this);
			subscriber.subscribe();
		}
	}

	/**
	 * Unsubscribes from {@link MqttListenerClient#getTopics()}
	 */
	default void unsubscribe() {
		DisposableMqttSubscriber subscriber = getDisposableMqttSubscriber();
		if (subscriber != null && subscriber.isConnected())
			subscriber.finish();
	}

}
