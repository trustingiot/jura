package iot.challenge.jura.graba.recorder;

import java.text.MessageFormat;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.data.DataService;

import iot.challenge.jura.graba.GrabaService;
import iot.challenge.jura.graba.Recording;
import iot.challenge.jura.util.MqttProcessor;
import iot.challenge.jura.util.trait.ActionableService;
import iot.challenge.jura.util.trait.DataServiceAdapter;

/**
 * Timer task capable of perform a recording
 */
public class RecordingTask extends TimerTask implements DataServiceAdapter {

	private ActionableService service;
	private Timer timer;
	private Options options;

	private DataService dataService;
	private CloudPayloadProtoBufDecoder decoder;
	private FinishRecordingTask finishRecordingTask;
	private long startTime;
	private long recordingTime;
	private long endTime;
	private Recording recording;
	private boolean canceled = false;
	private boolean subscribed = false;

	@Override
	public String getID() {
		return service.getID();
	}

	public RecordingTask(GrabaService service) {
		super();
		this.service = service;
		this.dataService = service.getDataService();
		this.decoder = service.getDecoder();
		this.options = (Options) service.getOptions();
		this.timer = service.getTimer();
		this.startTime = options.getStartTime().toEpochMilli();
		this.recordingTime = options.getRecordingTime() * 1000L;
		this.endTime = startTime + recordingTime;
	}

	@Override
	public void run() {
		performRegisteredAction("Start", this::startRecording);
	}

	private void startRecording() {
		if (canRun()) {
			startSubscription();
			if (subscribed)
				timer.schedule(createFinishRecordingTask(), recordingTime);
		} else {
			service.cleanControlTopic();
		}
	}

	private boolean canRun() {
		return (!canceled && endTime > System.currentTimeMillis());
	}

	protected void startSubscription() {
		try {
			String topic = options.getSubscription();
			long duration = options.getRecordingTime();
			recording = new Recording(topic, startTime, duration);
			dataService.addDataServiceListener(this);
			dataService.subscribe(getSubscriptionTopic(), 0);
			subscribed = true;
		} catch (KuraException e) {
			error("Unable to subscribe to {}", getSubscriptionTopic(), e);
			dataService.removeDataServiceListener(this);
			subscribed = false;
			service.cleanControlTopic();
		}
	}

	protected String getSubscriptionTopic() {
		return MessageFormat.format("+/+/+/{0}/+/+", options.getSubscription());
	}

	private FinishRecordingTask createFinishRecordingTask() {
		finishRecordingTask = new FinishRecordingTask(service, this);
		return finishRecordingTask;
	}

	public void finishSubscription() {
		if (subscribed) {
			dataService.removeDataServiceListener(this);
			try {
				dataService.unsubscribe(getSubscriptionTopic());
			} catch (KuraException e) {
				error("Unable to unsubscribe from {}", getSubscriptionTopic(), e);
				service.cleanControlTopic();
			}
			subscribed = false;
		}
	}

	public Recording getRecording() {
		return recording;
	}

	@Override
	public boolean cancel() {
		if (!canceled)
			performRegisteredAction("Cancel", this::cancelRecording);

		return super.cancel();
	}

	private void cancelRecording() {
		canceled = true;
		if (finishRecordingTask != null)
			finishRecordingTask.cancel();
	}

	@Override
	public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
		try {
			if (MqttProcessor.matches(getSubscriptionTopic(), topic)) {
				recording.save(topic.split("/")[4], decoder.buildFromByteArray(payload));
			}
		} catch (Exception e) {
			error(e.getMessage());
		}
	}

}
