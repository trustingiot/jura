package iot.challenge.jura.ubica.service.provider.simulation;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.service.component.ComponentContext;

import iot.challenge.jura.ubica.service.InstallationService;
import iot.challenge.jura.ubica.service.SimulationService;
import iot.challenge.jura.ubica.simulation.Simulation;
import iot.challenge.jura.util.trait.ActionRecorder;

/**
 * SimulationService provider
 */
public class SimulationServiceProvider implements SimulationService, ActionRecorder, ConfigurableComponent {

	////
	//
	// Action recorder
	//
	//
	public static final String ID = "iot.challenge.jura.ubica.simulation";

	@Override
	public String getID() {
		return ID;
	}

	////
	//
	// Parameters
	//
	//
	protected Options options;

	////
	//
	// Registered services
	//
	//
	protected InstallationService installationService;
	protected ConfigurationService configurationService;

	protected void setInstallationService(InstallationService service) {
		installationService = service;
	}

	protected void unsetInstallationService(InstallationService service) {
		installationService = null;
	}

	protected void setConfigurationService(ConfigurationService service) {
		configurationService = service;
	}

	protected void unsetConfigurationService(ConfigurationService service) {
		configurationService = null;
	}

	////
	//
	// Service methods
	//
	//
	protected void activate(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Activating", () -> {});
	}

	protected void updated(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Updating", this::update, properties);
	}

	protected void deactivate(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Deactivating", () -> {});
	}

	////
	//
	// Functionality
	//
	//
	private void update(Map<String, Object> properties) {
		options = new Options(properties);
		if (options.isEnable()) {
			new Simulation(installationService, options);
			disable(properties);
		}
	}
	
	private void disable(Map<String, Object> properties) {
		Map<String, Object> map = new HashMap<>(properties);
		map.put(Options.PROPERTY_ENABLE, false);
		try {
			configurationService.updateConfiguration(ID, map);
		} catch (KuraException e) {
			error("Unable to update configuartion", e);
		}
	}
}
