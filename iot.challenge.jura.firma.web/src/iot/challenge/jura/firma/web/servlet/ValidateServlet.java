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

import iot.challenge.jura.firma.service.IOTAService;
import iot.challenge.jura.firma.web.service.WebService;
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
				JsonObject json = WebService.iotaService.readMessage(transaction);
				if (json != null) {
					if (json.get("reject") == null) {
						message.add("sign", json);
						if (!WebService.signService.validate(json))
							message.add("reject",
									"Invalid signature. The transaction contains an intelligible message, but not a valid signature.");
					} else {
						int cause = json.get("reject").asInt();
						String m = "";
						switch (cause) {
						case IOTAService.READ_REJECT_API_EXCEPTION:
							m = "Connection problem. The transaction query failed.";
							break;

						case IOTAService.READ_REJECT_NOT_FOUND:
							m = "Not found. It has been moved, is no longer available or has never existed.";
							break;

						case IOTAService.READ_REJECT_PARSE_EXCEPTION:
							m = "Invalid message. The transaction does not contain an intelligible message for Jura.";
							break;

						default:
							m = "Unknown failure. Validation failed (and we do not know why).";
						}

						message.add("reject", m);
					}
				}
			}

		}
		generateResponseMessage(response, message);
	}

	protected static void generateResponseMessage(HttpServletResponse response, JsonObject message) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.print(message);
		out.flush();
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
