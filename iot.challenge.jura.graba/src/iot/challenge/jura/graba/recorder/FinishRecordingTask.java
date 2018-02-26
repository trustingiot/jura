package iot.challenge.jura.graba.recorder;

import java.text.MessageFormat;
import java.util.Date;
import java.util.TimerTask;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.message.KuraPayload;

import iot.challenge.jura.graba.Recording;
import iot.challenge.jura.util.trait.ActionRecorder;
import iot.challenge.jura.util.trait.ActionableService;

/**
 * Timer task to finish a recording task
 */
public class FinishRecordingTask extends TimerTask implements ActionRecorder {

	private ActionableService service;

	private CloudClient cloudClient;
	private Options options;
	private RecordingTask recordingTask;

	@Override
	public String getID() {
		return service.getID();
	}

	public FinishRecordingTask(ActionableService service, RecordingTask recordingTask) {
		super();
		this.service = service;
		this.cloudClient = service.getCloudClient();
		this.options = (Options) service.getOptions();
		this.recordingTask = recordingTask;
	}

	@Override
	public void run() {
		recordingTask.finishSubscription();
		performRegisteredAction("Publish", this::publishRecording);
		service.cleanControlTopic();
	}

	private void publishRecording() {
		try {
			cloudClient.publish(getPublicationTopic(), generatePayload(), 0, true);
		} catch (KuraException e) {
			error("Unable to publish in {}", getPublicationTopic(), e);
		}
	}

	protected String getPublicationTopic() {
		return MessageFormat.format("{0}/{1}",
				options.getPublication(),
				Long.toString(options.getStartTime().toEpochMilli()));
	}

	private KuraPayload generatePayload() {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());
		payload.addMetric(Recording.TOPIC, options.getSubscription());
		payload.addMetric(Recording.START_TIME, options.getStartTime().toEpochMilli());
		payload.addMetric(Recording.DURATION, options.getRecordingTime());
		payload.addMetric(Recording.RECORDING, recordingTask.getRecording().toString());
		return payload;
	}

	@Override
	public boolean cancel() {
		recordingTask.finishSubscription();
		return super.cancel();
	}
}