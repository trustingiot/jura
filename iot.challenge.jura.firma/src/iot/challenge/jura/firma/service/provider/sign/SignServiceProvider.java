package iot.challenge.jura.firma.service.provider.sign;

import iot.challenge.jura.firma.crypto.PGP;
import iot.challenge.jura.firma.service.SignService;
import iot.challenge.jura.util.trait.ActionRecorder;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.command.CommandService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.service.component.ComponentContext;

import com.eclipsesource.json.JsonObject;

/**
 * SignService provider
 */
public class SignServiceProvider implements SignService, ActionRecorder, ConfigurableComponent {

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
	protected Options options;
	protected PGP pgp;

	////
	//
	// Registered services
	//
	//
	protected CommandService commandService;
	protected ConfigurationService configurationService;

	protected void setCommandService(CommandService service) {
		commandService = service;
	}

	protected void unsetCommandService(CommandService service) {
		commandService = null;
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
		performRegisteredAction("Activating", this::update, properties);
	}

	protected void updated(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Updating", this::update, properties);
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
	protected void update(Map<String, Object> properties) {
		options = new Options(properties);
		pgp = new PGP(commandService, options);
		disableGenerateOption(properties);
	}

	protected void disableGenerateOption(Map<String, Object> properties) {
		if ((boolean) properties.get(Options.PROPERTY_GENERATE)) {
			Map<String, Object> map = new HashMap<>(properties);
			map.put(Options.PROPERTY_GENERATE, false);
			try {
				configurationService.updateConfiguration(ID, map);
			} catch (KuraException e) {
				error("Unable to update configuartion", e);
			}
		}
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
		signJSON.add("key", Long.toHexString(pgp.getKeys().getPublicKey().getKeyID()));

		JsonObject result = new JsonObject();
		result.add("body", body);
		result.add("sign", signJSON);
		return result;
	}

	@Override
	public boolean validate(JsonObject sign) {
		try {
			if (sign == null)
				return false;

			JsonObject bodyJSON = sign.get("body").asObject();
			JsonObject signJSON = sign.get("sign").asObject();

			String key = signJSON.get("key").asString();

			String message = recreateSignMessage(
					bodyJSON.toString(),
					signJSON.get("hash").asString(),
					signJSON.get("value").asString());

			return pgp.verify(message, key);
		} catch (Exception e) {
			return false;
		}
	}

	protected static String recreateSignMessage(String body, String hash, String sign) {
		return MessageFormat.format("-----BEGIN PGP SIGNED MESSAGE-----\n" +
				"Hash: {0}\n" +
				"\n" +
				"{1}\n" +
				"-----BEGIN PGP SIGNATURE-----\n" +
				"Version: BCPG v1.59\n" +
				"\n" +
				"{2}" +
				"-----END PGP SIGNATURE-----",
				hash,
				body,
				sign);
	}
}
