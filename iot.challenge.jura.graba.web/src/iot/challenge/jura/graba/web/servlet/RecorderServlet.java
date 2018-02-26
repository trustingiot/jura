package iot.challenge.jura.graba.web.servlet;

import java.util.Date;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.message.KuraPayload;
import com.eclipsesource.json.JsonObject;

import iot.challenge.jura.graba.recorder.Options;
import iot.challenge.jura.graba.recorder.RecorderService;
import iot.challenge.jura.graba.web.mqtt.DisposableMqttFetch;

/**
 * {@link ActionableServiceServlet} to manage {@link RecorderServer}
 */
public class RecorderServlet extends ActionableServiceServlet {

	private static final long serialVersionUID = 4453952412199673717L;

	protected static final String RENAME = "rename";
	protected static final String REMOVE = "remove";
	protected static final String UPLOAD = "upload";

	protected final String controlTopic = RecorderService.CONTROL_TOPIC;

	protected final String[][] entries = new String[][] {
			{ "duration", Options.PROPERTY_RECORDING_TIME },
			{ "startTime", Options.PROPERTY_START_TIME }
	};

	protected final String[][] fixedEntries = new String[][] {
			{ Options.PROPERTY_SUBSCRIPTION, Options.PROPERTY_SUBSCRIPTION_DEFAULT },
			{ Options.PROPERTY_PUBLICATION, Options.PROPERTY_PUBLICATION_DEFAULT }
	};

	public String getControlTopic() {
		return controlTopic;
	}

	public String[][] getEntries() {
		return entries;
	}

	public String[][] getFixedEntries() {
		return fixedEntries;
	}

	@Override
	protected void doAction(String cmd, JsonObject message) {
		switch (cmd) {
		case RENAME:
			renameRecording(message);
			break;

		case REMOVE:
			removeRecording(message);
			break;

		case UPLOAD:
			uploadRecording(message);
			break;
		}
	}

	protected void renameRecording(JsonObject message) {
		String oldTopic = readJsonString(message, "old");
		String newTopic = readJsonString(message, "new");
		if (oldTopic != null && newTopic != null) {
			CloudClient cloudClient = getCloudClient();
			String oldName = oldTopic.split(cloudClient.getApplicationId() + "/")[1];
			String newName = oldName.split("/")[0] + "/" + newTopic;
			DisposableMqttFetch.fetchAndConsume(oldTopic, mqttMessage -> {
				try {
					KuraPayload payload = getCloudPayloadProtobufDecoder().buildFromByteArray(mqttMessage.getPayload());
					cloudClient.publish(oldName, null, 1, true);
					cloudClient.publish(newName, payload, 1, true);
				} catch (KuraException e) {
					error("Fail to rename recording", e);
				}
			});
		}
	}

	protected void removeRecording(JsonObject message) {
		String topic = readJsonString(message, "topic");
		if (topic != null) {
			topic = topic.split(getCloudClient().getApplicationId())[1];
			topic = topic.substring(1);
			publish(topic, null, 1, true);
		}
	}

	protected void uploadRecording(JsonObject message) {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date(message.getLong("timestamp", 0L)));
		for (String key : new String[] { "topic", "startTime", "duration", "recording" }) {
			payload.addMetric(key, message.getString(key, ""));
		}
		String topic = "recording/upload" + System.currentTimeMillis();
		publish(topic, payload, 1, true);
	}
}
