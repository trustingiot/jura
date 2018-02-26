package iot.challenge.jura.firma.web.service;

import iot.challenge.jura.firma.service.IOTAService;
import iot.challenge.jura.firma.service.SignService;
import iot.challenge.jura.firma.web.servlet.DIWServlet;
import iot.challenge.jura.firma.web.servlet.TransactionsServlet;
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
	protected boolean activated = false;

	////
	//
	// Registered services
	//
	//
	protected HttpService httpService;

	protected void setHttpService(HttpService service) {
		httpService = service;
	}

	protected void unsetHttpService(HttpService service) {
		httpService = null;
	}

	protected void setIOTAService(IOTAService service) {
		ServiceProperties.put(ServiceProperties.PROPERTY_IOTA_SERVICE, service);
	}

	protected void unsetIOTAService(IOTAService service) {
		ServiceProperties.put(ServiceProperties.PROPERTY_IOTA_SERVICE, null);
	}

	protected void setSignService(SignService service) {
		ServiceProperties.put(ServiceProperties.PROPERTY_SIGN_SERVICE, service);
	}

	protected void unsetSignService(SignService service) {
		ServiceProperties.put(ServiceProperties.PROPERTY_SIGN_SERVICE, null);
	}

	////
	//
	// Service methods
	//
	// FIXME service methods were no longer invoked at the beginning (perhaps due
	// to residual configuration in Kura or Maven). They have been patched to fix
	// the problem
	protected void activate(ComponentContext context, Map<String, Object> properties) {
		activated = true;
		properties.forEach(ServiceProperties::put);
		performRegisteredAction("Activating", this::activate);
	}

	protected void updated(ComponentContext context, Map<String, Object> properties) {
		if (!activated) {
			activate(context, properties);
		} else {
			properties.forEach(ServiceProperties::put);
			performRegisteredAction("Updating", this::update);
		}
	}

	protected void deactivate(ComponentContext context, Map<String, Object> properties) {
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

	protected void update() {
		shutdown();
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
		servlets.put("/firma/diw", new DIWServlet());
		servlets.put("/firma/transactions", new TransactionsServlet());

		for (String alias : servlets.keySet()) {
			httpService.registerServlet(alias, servlets.get(alias), null, context);
		}
	}
}
