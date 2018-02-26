package iot.challenge.jura.graba.web.mqtt;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import iot.challenge.jura.graba.web.service.ServiceProperties;

/**
 * This class is a hack to obtain specific MQTT topic messages (usually with the
 * 'retain' flag).
 */
public class DisposableMqttFetch implements JuraMqttListener {

	protected static final String CONF_BROKER_URL = "tcp://localhost";

	private Subscriber subscriber;
	private Consumer<MqttMessage> consumer;
	private ScheduledExecutorService worker;

	private DisposableMqttFetch() {
		super();
		worker = Executors.newScheduledThreadPool(1);
		worker.schedule(() -> subscriber.finish(), 5, TimeUnit.SECONDS);
	}

	private DisposableMqttFetch(String topic, Consumer<MqttMessage> consumer) {
		this();
		this.consumer = consumer;
		subscriber = new Subscriber(topic);
		if (subscriber.isConnected()) {
			subscriber.addListener(this);
			subscriber.subscribe();
		}
	}

	/**
	 * Subscribe to 'topic' and calls 'consumer' with the obtained MQTT message
	 * 
	 * @param topic
	 *            MQTT topic
	 * @param consumer
	 *            Consumer to be called with the MqttMessage
	 */
	public static void fetchAndConsume(String topic, Consumer<MqttMessage> consumer) {
		new DisposableMqttFetch(topic, consumer);
	}

	class Subscriber extends DisposableMqttSubscriber {
		public Subscriber(String topic) {
			super(CONF_BROKER_URL,
					ServiceProperties.get(ServiceProperties.PROPERTY_MQTT_USER, String.class),
					ServiceProperties.get(ServiceProperties.PROPERTY_MQTT_PASSWORD, String.class),
					topic,
					1);
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		synchronized (this) {
			subscriber.removeListener(this);
			consumer.accept(message);
		}
	}
}
