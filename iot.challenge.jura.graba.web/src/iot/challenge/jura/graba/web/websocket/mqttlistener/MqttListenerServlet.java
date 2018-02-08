package iot.challenge.jura.graba.web.websocket.mqttlistener;

import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import iot.challenge.jura.graba.web.websocket.DelayedWebSocketServlet;

/**
 * Class to be implemented by websockets that will use sockets capable of
 * receiving MQTT messages
 */
public abstract class MqttListenerServlet extends DelayedWebSocketServlet {

	private static final long serialVersionUID = -9165401577278926349L;

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.getPolicy().setIdleTimeout(0L);
		factory.register(getListenerSocketClass());
	}

	protected abstract Class<? extends MqttListenerSocket> getListenerSocketClass();

}
