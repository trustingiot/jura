package iot.challenge.jura.graba.web.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.message.KuraPayload;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import iot.challenge.jura.util.trait.Loggable;
import iot.challenge.jura.graba.web.mqtt.DisposableMqttFetch;
import iot.challenge.jura.graba.web.service.ServiceProperties;

/**
 * Downloads a Graba's recording
 */
public class DonwloadRecordingsServlet extends JuraHttpServlet implements Loggable {

	private static final long serialVersionUID = -5729654113543820728L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException {

		String path = request.getPathInfo();

		if (path != null && path.length() > 1) {
			final String id = path.substring(1);

			synchronized (this) {
				if (timeouts == null)
					timeouts = new HashMap<>();
				timeouts.put(id, System.currentTimeMillis());
			}
			String topic = MessageFormat.format("+/+/{0}/recording/{1}",
					ServiceProperties.get(
							ServiceProperties.PROPERTY_GRABA_APPLICATION,
							String.class),
					id);
			DisposableMqttFetch.fetchAndConsume(topic, message -> {
				try {
					if (timeouts.containsKey(id)) {
						JsonObject object = Json.object();
						KuraPayload payload = getCloudPayloadProtobufDecoder().buildFromByteArray(message.getPayload());
						payload.metrics().forEach((k, v) -> object.add(k, v.toString()));
						object.add("timestamp", getTimestamp(payload));

						response.setContentType("text/txt");
						response.setHeader("Content-disposition", "attachment; filename=" + id + ".txt");

						OutputStream out = response.getOutputStream();
						out.write(object.toString().getBytes());
						out.flush();
						timeouts.remove(id);
					}
				} catch (KuraException | IOException e) {
					error("Fail to download recording", e);
				}
			});

			waitFor(id, 10000);
		}
	}

}
