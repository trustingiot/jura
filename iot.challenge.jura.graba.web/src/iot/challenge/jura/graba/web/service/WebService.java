package iot.challenge.jura.graba.web.service;

import iot.challenge.jura.util.trait.ActionRecorder;
import iot.challenge.jura.graba.web.mqtt.client.MqttListenerClient;
import iot.challenge.jura.graba.web.servlet.DonwloadRecordingsServlet;
import iot.challenge.jura.graba.web.servlet.RecorderServlet;
import iot.challenge.jura.graba.web.servlet.RecordingAggregatorServlet;
import iot.challenge.jura.graba.web.servlet.PlayerServlet;
import iot.challenge.jura.graba.web.websocket.graba.GrabaServlet;
import iot.challenge.jura.graba.web.websocket.recordings.RecordingsServlet;
import iot.challenge.jura.graba.web.websocket.scanner.ScannerServlet;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.data.DataService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * Registers web resources in Kura's HttpService and manages its life cycle
 */
public class WebService implements ActionRecorder, ConfigurableComponent {

	private static final String ID = "iot.challenge.jura.graba.web";

	////
	//
	// ActionRecorder trait
	//
	//
	@Override
	public String getID() {
		return ID;
	}

	////
	//
	// Parameters
	//
	//
	protected Map<String, HttpServlet> servlets;
	protected Map<String, String> resources;
	protected HttpContext context;
	protected boolean activated = false;

	////
	//
	// Registered services
	//
	//
	protected HttpService httpService;
	protected CloudService cloudService;
	protected DataService dataService;

	protected void setHttpService(HttpService service) {
		httpService = service;
	}

	protected void unsetHttpService(HttpService service) {
		httpService = null;
	}

	protected void setCloudService(CloudService service) {
		cloudService = service;
	}

	protected void unsetCloudService(CloudService service) {
		cloudService = null;
	}

	protected void setDataService(DataService service) {
		dataService = service;
	}

	protected void unsetDataService(DataService service) {
		dataService = null;
	}

	protected void setCloudPayloadProtoBufDecoder(CloudPayloadProtoBufDecoder decoder) {
		ServiceProperties.put(ServiceProperties.PROPERTY_CLOUD_PAYLOAD_PROTOBUF_DECODER, decoder);
	}

	protected void unsetCloudPayloadProtoBufDecoder(CloudPayloadProtoBufDecoder decoder) {
		ServiceProperties.put(ServiceProperties.PROPERTY_CLOUD_PAYLOAD_PROTOBUF_DECODER, null);
	}

	////
	//
	// Service methods
	//
	//
	protected void activate(ComponentContext context, Map<String, Object> properties) {
		activated = true;
		performRegisteredAction("Activating", this::activate, properties);
	}

	protected void updated(ComponentContext context, Map<String, Object> properties) {
		if (!activated) {
			activate(context, properties);
		}
		performRegisteredAction("Updating", this::update, properties);
	}

	protected void deactivate(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Deactivating", this::shutdown);
	}

	////
	//
	// Functionality
	//
	//
	protected void activate(Map<String, Object> properties) {
		context = httpService.createDefaultHttpContext();
	}

	protected void update(Map<String, Object> properties) {
		shutdown();
		properties.forEach(ServiceProperties::put);
		init();
	}

	protected void shutdown() {
		releaseServlets();
		releaseResources();
		releaseCloudConnection();
	}

	protected void releaseServlets() {
		if (servlets != null) {

			if (httpService != null) {
				servlets.keySet().forEach(httpService::unregister);
			}

			servlets.values().stream()
					.filter(MqttListenerClient.class::isInstance)
					.map(MqttListenerClient.class::cast)
					.forEach(MqttListenerClient::unsubscribe);

			servlets.clear();
			servlets = null;
		}
	}

	protected void releaseResources() {
		if (resources != null) {

			if (httpService != null) {
				resources.keySet().forEach(httpService::unregister);
			}

			resources.clear();
			resources = null;
		}
	}

	protected void releaseCloudConnection() {
		CloudClient client = ServiceProperties.get(ServiceProperties.PROPERTY_CLOUD_CLIENT, CloudClient.class);
		if (client != null) {
			client.release();
			ServiceProperties.put(ServiceProperties.PROPERTY_CLOUD_CLIENT, null);
		}
	}

	protected void init() {
		connectToCloud();
		try {
			registerResources(context);
			registerServlets(context);
		} catch (NamespaceException | ServletException e) {
			error("Error registering jura app", e);
		}
	}

	protected void connectToCloud() {
		CloudClient cloudClient = null;
		try {
			String application = ServiceProperties.get(ServiceProperties.PROPERTY_APPLICATION, String.class);
			cloudClient = cloudService.newCloudClient(application);
		} catch (KuraException e) {
			error("Unable to get CloudClient", e);
		}
		ServiceProperties.put(ServiceProperties.PROPERTY_CLOUD_CLIENT, cloudClient);
	}

	protected void registerResources(HttpContext context) throws NamespaceException {
		resources = new HashMap<>();
		resources.put("/jura", "resources/web/index.html");
		resources.put("/jura/main.js", "resources/web/main.js");
		resources.put("/jura/img", "resources/web/img");
		resources.put("/jura/css", "resources/web/css");
		resources.put("/jura/js", "resources/web/js");
		for (String alias : resources.keySet()) {
			httpService.registerResources(alias, resources.get(alias), context);
		}
	}

	protected void registerServlets(HttpContext context) throws ServletException, NamespaceException {
		servlets = new HashMap<>();

		// Websockets
		servlets.put("/jura/scanner", new ScannerServlet());
		servlets.put("/jura/graba", new GrabaServlet());
		servlets.put("/jura/recordings", new RecordingsServlet());

		// Servlets
		servlets.put("/jura/aggregate", new RecordingAggregatorServlet());
		servlets.put("/jura/player", new PlayerServlet());
		servlets.put("/jura/recorder", new RecorderServlet());
		servlets.put("/jura/recording", new DonwloadRecordingsServlet());

		for (String alias : servlets.keySet()) {
			httpService.registerServlet(alias, servlets.get(alias), null, context);
		}

		servlets.values().stream()
				.filter(MqttListenerClient.class::isInstance)
				.map(MqttListenerClient.class::cast)
				.forEach(MqttListenerClient::subscribe);
	}
}
