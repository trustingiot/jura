package iot.challenge.jura.graba.player;

import java.text.MessageFormat;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.message.KuraPayload;

import iot.challenge.jura.faro.BeaconEvent;
import iot.challenge.jura.graba.Recording;
import iot.challenge.jura.util.MqttProcessor;
import iot.challenge.jura.util.trait.ActionableService;
import iot.challenge.jura.util.trait.CloudClientAdapter;

/**
 * Timer task capable of playing a recording
 */
public class PlaybackTask extends TimerTask implements CloudClientAdapter {

	private ActionableService service;
	private CloudClient cloudClient;
	private Options options;

	protected ScheduledExecutorService worker;
	protected Future<?> playNextEventHandle, unsubscribeHandle;
	private Queue<PlaybackEvent> events;
	private boolean canceled = false;
	private boolean subscribed = false;
	private long lastEventTime;
	private long nextEventTime;

	private class PlaybackEvent implements Comparable<PlaybackEvent> {
		String scanner;
		BeaconEvent beaconEvent;

		public PlaybackEvent(String scanner, BeaconEvent beaconEvent) {
			this.scanner = scanner;
			this.beaconEvent = beaconEvent;
		}

		@Override
		public int compareTo(PlaybackEvent o) {
			return beaconEvent.compareTo(o.beaconEvent);
		}
	}

	@Override
	public String getID() {
		return service.getID();
	}

	public PlaybackTask(ActionableService service) {
		super();
		this.service = service;
		this.cloudClient = service.getCloudClient();
		this.options = (Options) service.getOptions();
	}

	@Override
	public void run() {
		performRegisteredAction("Start", this::startPlayback);
	}

	private void startPlayback() {
		if (!canceled) {
			worker = Executors.newScheduledThreadPool(2);
			readRecording();
		}
	}

	protected void readRecording() {
		try {
			cloudClient.addCloudClientListener(this);
			cloudClient.subscribe(options.getSubscription(), 0);
			unsubscribeHandle = worker.schedule(this::abort, 10, TimeUnit.SECONDS);
			subscribed = true;
		} catch (KuraException e) {
			error("Unable to subscribe to {}", options.getSubscription(), e);
			cloudClient.removeCloudClientListener(this);
			subscribed = false;
		}
	}

	@Override
	public boolean cancel() {
		if (!canceled)
			performRegisteredAction("Cancel", this::cancelPlayback);

		return super.cancel();
	}

	private void cancelPlayback() {
		canceled = true;
		finishSubscription();
		shutdownWorker();
	}

	protected void abort() {
		synchronized (this) {
			if (subscribed) {
				finishSubscription();
				service.cleanControlTopic();
			}
		}
	}

	protected void finishSubscription() {
		if (subscribed) {
			cloudClient.removeCloudClientListener(this);
			try {
				cloudClient.unsubscribe(options.getSubscription());
			} catch (KuraException e) {
				error("Unable to unsubscribe from {}", options.getSubscription(), e);
			}
			subscribed = false;
		}
	}

	protected void shutdownWorker() {
		if (playNextEventHandle != null)
			playNextEventHandle.cancel(true);

		if (worker != null)
			worker.shutdown();
	}

	@Override
	public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
		if (MqttProcessor.matches(options.getSubscription(), appTopic)) {
			try {
				// Finish subscription
				finishSubscription();

				// Parse recording
				String recordingString = (String) msg.getMetric(Recording.RECORDING);
				Recording recording = Recording.fromString(recordingString);

				// Play recording
				events = new PriorityQueue<>();
				recording.getEvents().forEach((scanner, scannerEvents) -> {
					scannerEvents.forEach(event -> events.add(new PlaybackEvent(scanner, event)));
				});
				nextEventTime = lastEventTime = 0;
				scheduleNextEvent();
			} catch (Exception e) {
				error("Invalid message", e);
				cancel();
			}
		}
	}

	protected void scheduleNextEvent() {
		if (!canceled) {
			PlaybackEvent event = events.poll();
			if (event != null) {
				nextEventTime = event.beaconEvent.getTime() - lastEventTime;
				lastEventTime = event.beaconEvent.getTime();
				scheduleEvent(event);
			} else {
				performRegisteredAction("Finish", this::finishPlayback);
			}
		}
	}

	protected void scheduleEvent(PlaybackEvent event) {
		playNextEventHandle = worker.schedule(() -> playNextEvent(event), nextEventTime, TimeUnit.MILLISECONDS);
	}

	protected void playNextEvent(PlaybackEvent event) {
		playEvent(event);
		scheduleNextEvent();
	}

	protected void playEvent(PlaybackEvent event) {
		try {
			cloudClient.publish(getCloudTopic(event), event.beaconEvent.encode(), 1, false);
		} catch (KuraException e) {
			error("Unable to publish", e);
		}
	}

	protected String getCloudTopic(PlaybackEvent event) {
		return MessageFormat.format(
				"{0}/{1}/{2}",
				options.getPublication(),
				event.scanner,
				event.beaconEvent.getBeacon().getAddress());
	}

	protected void finishPlayback() {
		shutdownWorker();
		service.cleanControlTopic();
	}

}
