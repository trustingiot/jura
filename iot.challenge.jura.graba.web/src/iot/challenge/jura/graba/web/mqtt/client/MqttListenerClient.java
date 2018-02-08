package iot.challenge.jura.graba.web.mqtt.client;

/**
 * Interface to be implemented by a MQTT listener client
 */
public interface MqttListenerClient {
	String[] getTopics();

	int[] getQos();

	void subscribe();

	void unsubscribe();
}