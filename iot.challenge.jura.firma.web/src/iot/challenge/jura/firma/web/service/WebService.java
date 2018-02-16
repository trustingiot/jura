package iot.challenge.jura.firma.web.service;

import iot.challenge.jura.firma.service.IOTAService;
import iot.challenge.jura.firma.service.SignService;
import iot.challenge.jura.firma.web.servlet.ValidateServlet;
import iot.challenge.jura.util.trait.ActionRecorder;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * Registers web resources in Kura's HttpService and manages its life cycle
 */
public class WebService implements ActionRecorder, ConfigurableComponent {

	private static final String ID = "iot.challenge.jura.firma.web";

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

	////
	//
	// Registered services
	//
	//
	protected HttpService httpService;

	// FIXME guice?
	public static IOTAService iotaService;
	public static SignService signService;

	protected void setHttpService(HttpService service) {
		httpService = service;
	}

	protected void unsetHttpService(HttpService service) {
		httpService = null;
	}

	protected void setIOTAService(IOTAService service) {
		iotaService = service;
	}

	protected void unsetIOTAService(IOTAService service) {
		iotaService = null;
	}

	protected void setSignService(SignService service) {
		signService = service;
	}

	protected void unsetSignService(SignService service) {
		signService = null;
	}

	////
	//
	// Service methods
	//
	//
	protected void activate(ComponentContext context) {
		performRegisteredAction("Activating", this::activate);
	}

	protected void deactivate(ComponentContext context) {
		performRegisteredAction("Deactivating", this::shutdown);
	}

	////
	//
	// Functionality
	//
	//
	protected void activate() {
		context = httpService.createDefaultHttpContext();
		init();
	}

	protected void shutdown() {
		releaseServlets();
		releaseResources();
	}

	protected void releaseServlets() {
		if (servlets != null) {

			if (httpService != null) {
				servlets.keySet().forEach(httpService::unregister);
			}

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

	protected void init() {
		try {
			registerResources(context);
			registerServlets(context);
		} catch (NamespaceException | ServletException e) {
			error("Error registering firma app", e);
		}
	}

	protected void registerResources(HttpContext context) throws NamespaceException {
		resources = new HashMap<>();
		resources.put("/firma", "resources/web/index.html");
		resources.put("/firma/main.js", "resources/web/main.js");
		resources.put("/firma/img", "resources/web/img");
		resources.put("/firma/css", "resources/web/css");
		resources.put("/firma/js", "resources/web/js");
		for (String alias : resources.keySet()) {
			httpService.registerResources(alias, resources.get(alias), context);
		}
	}

	protected void registerServlets(HttpContext context) throws ServletException, NamespaceException {
		servlets = new HashMap<>();

		servlets.put("/firma/validate", new ValidateServlet());

		for (String alias : servlets.keySet()) {
			httpService.registerServlet(alias, servlets.get(alias), null, context);
		}
	}
}
