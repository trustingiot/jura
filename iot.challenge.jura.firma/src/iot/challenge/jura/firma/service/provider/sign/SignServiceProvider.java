package iot.challenge.jura.firma.service.provider.sign;

import iot.challenge.jura.firma.service.SignService;
import iot.challenge.jura.util.trait.ActionRecorder;

import org.osgi.service.component.ComponentContext;

/**
 * SignService provider
 */
public class SignServiceProvider implements SignService, ActionRecorder {

	////
	//
	// Action recorder
	//
	//
	public static final String ID = "iot.challenge.jura.firma.sign";

	@Override
	public String getID() {
		return ID;
	}

	////
	//
	// Parameters
	//
	//
	protected PGP pgp;

	////
	//
	// Service methods
	//
	//
	protected void activate(ComponentContext context) {
		performRegisteredAction("Activating", this::activate);
	}

	protected void deactivate(ComponentContext context) {
		performRegisteredAction("Deactivating", () -> {
		});
	}

	////
	//
	// Functionality
	//
	//
	protected void activate() {
		pgp = new PGP();
	}

	////
	//
	// Sign service
	//
	//
	@Override
	public String sign(String message) {
		return pgp.sign(message);
	}
}
