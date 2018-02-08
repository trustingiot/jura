package iot.challenge.jura.graba.web.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.eclipsesource.json.JsonObject;

import iot.challenge.jura.util.trait.ActionableService;

/**
 * {@link JuraHttpServlet} who also knows how to interact with an
 * {@link ActionableService}
 */
public abstract class ActionableServiceServlet extends JuraHttpServlet {

	private static final long serialVersionUID = 6768115561081781569L;

	public static final String CANCEL = "cancel";
	public static final String CONFIGURE = "configure";
	public static final String CMD = "cmd";

	abstract protected String getControlTopic();

	abstract protected String[][] getEntries();

	abstract protected String[][] getFixedEntries();

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JsonObject message = readJson(request);
		if (message != null) {
			String cmd = readJsonString(message, CMD);
			switch (cmd == null ? CONFIGURE : cmd) {
			case CANCEL:
				cancelService();
				break;

			case CONFIGURE:
				configureService(message);
				break;

			default:
				doAction(cmd, message);
			}
		}
	}

	protected void cancelService() {
		publish(getControlTopic(), ActionableService.createCancelPayload(), 1, true);
	}

	protected void configureService(JsonObject message) {
		publish(getControlTopic(), jsonToKuraPayload(message, getEntries(), getFixedEntries()), 1, true);
	}

	protected void doAction(String cmd, JsonObject message) {
	}
}
