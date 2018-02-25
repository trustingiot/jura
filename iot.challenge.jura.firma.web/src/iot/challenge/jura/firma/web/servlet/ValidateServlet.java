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
import iot.challenge.jura.firma.crypto.AES;
import iot.challenge.jura.firma.service.IOTAService;
import iot.challenge.jura.firma.web.service.WebService;
import iot.challenge.jura.util.trait.Loggable;

/**
 * Servlet for the validation of transactions
 */
public class ValidateServlet extends HttpServlet implements Loggable {

	private static final long serialVersionUID = -6477462392686027274L;

	public static final String MODE = "mode";
	public static final String TRANSACTION = "transaction";
	public static final String KEY = "key";
	public static final String SIGN = "sign";
	public static final String ENCRYPT = "encrypt";
	public static final String REJECT = "reject";
	public static final String MESSAGE = "message";

	private static final String MSG_INVALID_JSON = "Invalid message. The transaction does not contain an intelligible message for Jura.";
	private static final String MSG_INVALID_SIGNATURE = "Invalid signature. The transaction contains an intelligible message, but not a valid signature.";
	private static final String MSG_DECRYPT_EXCEPTION = "Decrypt exception. The message could not be decrypted.";
	private static final String MSG_REJECT_API_EXCEPTION = "Connection problem. The transaction query failed.";
	private static final String MSG_REJECT_NOT_FOUND = "Not found. It has been moved, is no longer available or has never existed.";
	private static final String MSG_REJECT_UNKNOWN = "Unknown failure. Validation failed (and we do not know why).";

	private enum Mode {
		publicly {
			@Override
			void validateResponse(JsonObject request, JsonObject response) {
				JsonValue message = response.get(MESSAGE);
				response.remove(MESSAGE);
				response.add(SIGN, message);
				response.add(TRANSACTION, readJsonString(request, TRANSACTION));
				if (message.isObject()) {
					JsonObject sign = message.asObject();
					if (!WebService.signService.validate(sign))
						response.add(REJECT, MSG_INVALID_SIGNATURE);

				} else {
					response.add(REJECT, MSG_INVALID_JSON);
				}
			}
		},
		anonymously {
			@Override
			void validateResponse(JsonObject request, JsonObject response) {
				JsonValue message = response.get(MESSAGE);
				response.remove(MESSAGE);
				response.add(ENCRYPT, message);
				String key = readJsonString(request, KEY);
				response.add(KEY, key);
				try {
					String sign = AES.decrypt(message.asString(), key);
					response.add(MESSAGE, Json.parse(sign));
					Mode.publicly.validateResponse(request, response);

				} catch (Exception e) {
					response.add(REJECT, MSG_DECRYPT_EXCEPTION);
				}
			}
		};

		static JsonObject validateRequest(JsonObject request) {
			JsonObject result = null;
			if (request != null) {
				try {
					String transaction = readJsonString(request, TRANSACTION);
					if (transaction != null) {
						result = WebService.iotaService.readMessage(transaction);
						if (result != null) {
							if (result.get(REJECT) == null) {
								Mode mode = valueOf(readJsonString(request, MODE));
								mode.validateResponse(request, result);

							} else {
								setRejectionMessage(result);
							}
						}
					}
				} catch (Exception e) {
					// Nothing to do
				}
			}
			return result;
		}

		abstract void validateResponse(JsonObject request, JsonObject response);

		static void setRejectionMessage(JsonObject response) {
			int cause = response.get(REJECT).asInt();
			String m = "";
			switch (cause) {
			case IOTAService.READ_REJECT_API_EXCEPTION:
				m = MSG_REJECT_API_EXCEPTION;
				break;

			case IOTAService.READ_REJECT_NOT_FOUND:
				m = MSG_REJECT_NOT_FOUND;
				break;

			default:
				m = MSG_REJECT_UNKNOWN;
			}

			response.remove(REJECT);
			response.add(REJECT, m);
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JsonObject message = readJson(request);
		JsonObject validation = Mode.validateRequest(message);
		generateResponseMessage(response, validation);
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
