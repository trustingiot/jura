package iot.challenge.jura.firma.web.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import iot.challenge.jura.util.trait.Loggable;

/**
 * Servlet for the validation of signed transactions
 */
public class ValidateServlet extends HttpServlet implements Loggable {

	private static final long serialVersionUID = -6477462392686027274L;

	public static final String TRANSACTION = "transaction";

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JsonObject message = readJson(request);
		if (message != null) {
			String transaction = readJsonString(message, TRANSACTION);
			if (transaction != null) {
				// TODO Obtain transaction message
				// TODO Parse message
				// TODO Validate message
				// TODO Obtain key
				// TODO Validate sign
				// TODO Generate response
				// TODO Visualize response
			}
		}
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
}
