package iot.challenge.jura.firma.service.provider.sign;

import iot.challenge.jura.firma.service.SignService;
import iot.challenge.jura.util.trait.ActionRecorder;

import org.osgi.service.component.ComponentContext;

import com.eclipsesource.json.JsonObject;

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

	@Override
	public String extractSignature(String message) {
		if (message != null) {
			try {
				return message.substring(
						message.lastIndexOf("\n\n"),
						message.lastIndexOf("----END"))
						.trim();
			} catch (Exception e) {
			}
		}
		return null;
	}

	@Override
	public String extractHash(String message) {
		if (message != null) {
			try {
				String prefix = "Hash:";
				return message.substring(
						message.indexOf(prefix) + prefix.length(),
						message.indexOf("\n\n"))
						.trim();
			} catch (Exception e) {
			}
		}
		return null;
	}

	@Override
	public JsonObject sign(JsonObject body) {
		String sign = sign(body.toString());

		JsonObject signJSON = new JsonObject();
		signJSON.add("hash", extractHash(sign));
		signJSON.add("value", extractSignature(sign));

		JsonObject result = new JsonObject();
		result.add("body", body);
		result.add("sign", signJSON);
		return result;
	}
}
