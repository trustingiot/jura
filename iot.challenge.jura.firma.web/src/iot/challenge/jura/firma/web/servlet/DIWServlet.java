package iot.challenge.jura.firma.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import iot.challenge.jura.firma.crypto.DIW;
import iot.challenge.jura.firma.crypto.SHA256;
import iot.challenge.jura.firma.web.service.ServiceProperties;
import iot.challenge.jura.util.trait.Loggable;

/**
 * Servlet for obtaining device's DIW
 */
public class DIWServlet extends HttpServlet implements Loggable {

	private static final long serialVersionUID = -557716762897875821L;

	public static final String DEVICE = "device";
	public static final String SEED = "seed";
	public static final String VALUE = "value";

	public static final String DIW = "diw";

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JsonObject message = readJson(request);
		if (validPassword(message)) {
			String device = readJsonString(message, DEVICE);
			String salt = ServiceProperties.get(ServiceProperties.PROPERTY_SALT, String.class);
			message.add(DIW, new DIW(salt, device).getWord());
		}
		generateResponseMessage(response, message);
	}

	protected boolean validPassword(JsonObject message) {
		String device = readJsonString(message, DEVICE);
		String password = ServiceProperties.get(ServiceProperties.PROPERTY_PASSWORD, String.class);
		String seed = readJsonString(message, SEED);

		return SHA256.digest(device + password + seed).equals(readJsonString(message, VALUE));
	}

	protected static JsonObject readJson(HttpServletRequest request) throws IOException {
		try {
			return Json.parse(request.getReader()).asObject();
		} catch (NullPointerException npe) {
			return null;
		}
	}

	protected static String readJsonString(JsonObject object, String key) {
		JsonValue jsonValue = object.get(key);
		if (jsonValue == null)
			return null;

		String value = jsonValue.asString().trim();
		if (value.isEmpty())
			return null;

		return value;
	}

	protected static void generateResponseMessage(HttpServletResponse response, JsonObject message) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.print(message);
		out.flush();
	}
}
