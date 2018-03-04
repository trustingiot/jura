package iot.challenge.jura.graba;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.data.DataService;
import org.osgi.service.component.ComponentContext;

import iot.challenge.jura.util.trait.ActionableService;

/**
 * Actionable service for scheduled tasks
 */
public abstract class GrabaService implements ActionableService {

	/**
	 * MQTT Base Topic
	 */
	public static String BASE_TOPIC = "graba";

	/**
	 * Returns the MQTT control topic
	 *
	 * @param topic
	 *            Specific topic
	 *
	 * @return BASE_TOPIC/<topic>
	 */
	public static String getControlTopic(String topic) {
		return BASE_TOPIC + "/" + topic;
	}

	////
	//
	// Parameters
	//
	//
	protected CloudClient cloudClient;
	protected Options options;
	protected Timer timer;
	protected TimerTask timerTask;

	public CloudClient getCloudClient() {
		return cloudClient;
	}

	public void setCloudClient(CloudClient cloudClient) {
		this.cloudClient = cloudClient;
	}

	public Options getOptions() {
		return options;
	}

	public Timer getTimer() {
		return timer;
	}

	////
	//
	// Registered services
	//
	//
	protected CloudService cloudService;
	protected DataService dataService;
	protected CloudPayloadProtoBufDecoder decoder;

	protected void setCloudService(CloudService service) {
		cloudService = service;
	}

	protected void unsetCloudService(CloudService service) {
		cloudService = null;
	}

	protected void setDataService(DataService service) {
		dataService = service;
	}

	protected void unsetDataService(DataService service) {
		dataService = service;
	}

	public DataService getDataService() {
		return dataService;
	}

	protected void setCloudPayloadProtoBufDecoder(CloudPayloadProtoBufDecoder decoder) {
		this.decoder = decoder;
	}

	protected void unsetCloudPayloadProtoBufDecoder(CloudPayloadProtoBufDecoder decoder) {
		this.decoder = null;
	}

	public CloudPayloadProtoBufDecoder getDecoder() {
		return decoder;
	}

	////
	//
	// Service methods
	//
	//
	protected void activate(ComponentContext context) {
		performRegisteredAction("Activating", this::activate);
	}

	protected void deactivate(ComponentContext context) {
		performRegisteredAction("Deactivating", this::deactivate);
	}

	////
	//
	// Functionality
	//
	//
	protected void activate() {
		connectToCloud(cloudService, Options.PROPERTY_APPLICATION_DEFAULT);
	}

	protected void deactivate() {
		stopTask();
		releaseCloudConnection();
	}

	protected void stopTask() {
		shutdownTask();
	}

	protected void shutdownTask() {
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
	}

	protected void startTask() {
		if (taskMustBeScheduled()) {
			scheduleTask();
		} else {
			cleanControlTopic();
		}
	}

	protected boolean taskMustBeScheduled() {
		return options.getStartTime().isAfter(Instant.now());
	}

	protected void scheduleTask() {
		if (timer == null) {
			timer = new Timer();
		}
		timerTask = createTimerTask();
		timer.schedule(timerTask, Date.from(options.getStartTime()));
	}

	abstract protected TimerTask createTimerTask();

	////
	//
	// ActionableService
	//
	//
	@Override
	public void setOptions(iot.challenge.jura.util.Options options) {
		if (options != null) {
			stopTask();
			this.options = (Options) options;
			startTask();
		} else {
			this.options = null;
		}
	}

	@Override
	public void cancelService() {
		stopTask();
	}

	@Override
	public Options createOptions(Map<String, Object> configuration) {
		try {
			configuration.put(Options.PROPERTY_START_TIME,
					Instant.ofEpochMilli(Long.parseLong((String) configuration.get(Options.PROPERTY_START_TIME)))
							.toString());
			return createSpecificOptions(configuration);
		} catch (Exception e) {
			return null;
		}
	}

	protected Options createSpecificOptions(Map<String, Object> properties) {
		return new Options(properties);
	}

}
