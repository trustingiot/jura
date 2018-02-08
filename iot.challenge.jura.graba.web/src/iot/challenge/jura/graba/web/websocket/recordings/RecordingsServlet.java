package iot.challenge.jura.graba.web.websocket.recordings;

import java.text.MessageFormat;

import iot.challenge.jura.graba.web.service.ServiceProperties;
import iot.challenge.jura.graba.web.websocket.mqttlistener.MqttListenerServlet;
import iot.challenge.jura.graba.web.websocket.mqttlistener.MqttListenerSocket;

/**
 * Websocket to obtain the recordings of Graba
 */
public class RecordingsServlet extends MqttListenerServlet {

	private static final long serialVersionUID = 5406522072292893348L;

	protected static String CONF_BROKER_URL = "tcp://localhost";

	@Override
	protected Class<? extends MqttListenerSocket> getListenerSocketClass() {
		return ListenerSocket.class;
	}

	public static class ListenerSocket extends MqttListenerSocket {
		public ListenerSocket() {
			super(CONF_BROKER_URL,
					ServiceProperties.get(ServiceProperties.PROPERTY_MQTT_USER, String.class),
					ServiceProperties.get(ServiceProperties.PROPERTY_MQTT_PASSWORD, String.class),
					new String[] { MessageFormat.format("+/+/{0}/recording/+",
							ServiceProperties.get(ServiceProperties.PROPERTY_GRABA_APPLICATION, String.class)) },
					new int[] { 0 });
		}
	}

}
