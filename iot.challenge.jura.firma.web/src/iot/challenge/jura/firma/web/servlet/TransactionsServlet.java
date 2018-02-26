package iot.challenge.jura.firma.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import iot.challenge.jura.firma.crypto.DIW;
import iot.challenge.jura.firma.crypto.EncryptedTransaction;
import iot.challenge.jura.firma.service.IOTAService;
import iot.challenge.jura.firma.web.service.ServiceProperties;
import iot.challenge.jura.util.trait.Loggable;
import jota.model.Transaction;

/**
 * Servlet for obtaining device's transactions
 */
public class TransactionsServlet extends HttpServlet implements Loggable {

	private static final long serialVersionUID = 3695407735278451197L;

	public static final String DEVICE = "device";
	public static final String DIW = "diw";
	public static final String TRANSACTIONS = "transactions";
	public static final String REJECT = "reject";

	public static final String TRANSACTION = "transaction";
	public static final String ADDRESS = "address";
	public static final String KEY = "key";

	private static final String MSG_REJECT_AUTHENTICATION_FAILURE = "Authentication failure.";
	private static final String MSG_REJECT_UNKNOWN = "Unknown failure. Validation failed (and we do not know why).";

	protected static Map<EncryptedTransaction, JsonObject> cache;

	static {
		cache = new HashMap<>();
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JsonObject message = readJson(request);
		if (validDIW(message)) {
			addTransactions(message);
		} else {
			message.add(REJECT, MSG_REJECT_AUTHENTICATION_FAILURE);
		}
		generateResponseMessage(response, message);
	}

	protected static JsonObject readJson(HttpServletRequest request) throws IOException {
		try {
			return Json.parse(request.getReader()).asObject();
		} catch (NullPointerException npe) {
			return null;
		}
	}

	protected boolean validDIW(JsonObject message) {
		String device = readJsonString(message, DEVICE);
		String diw = readJsonString(message, DIW);
		String salt = ServiceProperties.get(ServiceProperties.PROPERTY_SALT, String.class);
		return new DIW(salt, device).check(device, diw);
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

	protected void addTransactions(JsonObject message) {
		EncryptedTransaction encryptedTransaction = new EncryptedTransaction(
				ServiceProperties.get(ServiceProperties.PROPERTY_SALT, String.class),
				readJsonString(message, DEVICE));

		JsonArray transactions = loadFromCache(encryptedTransaction);

		IOTAService iotaService = ServiceProperties.get(ServiceProperties.PROPERTY_IOTA_SERVICE, IOTAService.class);

		Function<EncryptedTransaction, List<Transaction>> generator = transaction -> {
			try {
				return iotaService.getTransactions(transaction.getAddress());
			} catch (Exception e) {
				return null;
			}
		};

		Predicate<Transaction> filter = transaction -> {
			try {
				String encryptedMessage = iotaService.extractMessage(transaction);
				String decryptedMessage = encryptedTransaction.decrypt(encryptedMessage).trim();
				JsonObject value = Json.parse(decryptedMessage).asObject();

				value.add(TRANSACTION, transaction.getHash());
				value.add(ADDRESS, encryptedTransaction.getAddress());
				value.add(KEY, encryptedTransaction.getKey());

				transactions.add(value);
				return true; // It is a JSON encrypted with this key
			} catch (Exception e) {
				return false;
			}
		};
		try {
			encryptedTransaction.end(generator, filter);
		} catch (Exception e) {
			message.add(REJECT, MSG_REJECT_UNKNOWN);
		}

		message.add(TRANSACTIONS, transactions);
	}

	protected static JsonArray loadFromCache(EncryptedTransaction encryptedTransaction) {
		JsonArray transactions = new JsonArray();
		boolean cached;
		do {
			cached = false;
			encryptedTransaction.next();
			if (cache.containsKey(encryptedTransaction)) {
				transactions.add(cache.get(encryptedTransaction));
				cached = true;
			}
		} while (cached);
		encryptedTransaction.back();

		return transactions;
	}

	protected static void generateResponseMessage(HttpServletResponse response, JsonObject message) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.print(message);
		out.flush();
	}
}
