package iot.challenge.jura.graba.web.mqtt.client;

import java.util.Arrays;
import java.util.function.BiConsumer;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.data.DataService;

import iot.challenge.jura.util.trait.DataServiceAdapter;

/**
 * Interface to be implemented by the classes that wish to subscribe to Kura's
 * DataService
 */
public interface LocalMqttListenerClient extends MqttListenerClient, DataServiceAdapter {

	DataService getDataService();

	/**
	 * Adds itself as Kura's DataService listener and subscribes DataService to
	 * {@link MqttListenerClient#getTopics()}
	 */
	default void subscribe() {
		getDataService().addDataServiceListener(this);
		applyForAllTopics((ds, topic) -> {
			try {
				ds.subscribe(topic, 0);
			} catch (KuraException e) {
				error("Unable to subscribe to {}", topic, e);
			}
		});
	}

	/**
	 * Apply 'consumer' to all topics
	 * 
	 * @param consumer
	 *            Consumer to be applied
	 */
	default void applyForAllTopics(BiConsumer<DataService, String> consumer) {
		String[] values = getTopics();
		if (values != null) {
			Arrays.asList(values).forEach(it -> consumer.accept(getDataService(), it));
		}
	}

	/**
	 * Removes itself from Kura's DataService listeners.
	 * 
	 * WARNING: It's not unsubscribe DataService from
	 * {@link MqttListenerClient#getTopics()} since the subscription may be
	 * necessary for other clients. {@link DisposableMqttListenerClient}
	 */
	default void unsubscribe() {
		getDataService().removeDataServiceListener(this);
	}

}
