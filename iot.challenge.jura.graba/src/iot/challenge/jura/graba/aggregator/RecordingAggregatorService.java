package iot.challenge.jura.graba.aggregator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;

import iot.challenge.jura.graba.GrabaService;
import iot.challenge.jura.util.trait.ActionableService;
import iot.challenge.jura.graba.Recording;
import iot.challenge.jura.util.MqttProcessor;

/**
 * Actionable service capable of aggregate two recordings
 */
public class RecordingAggregatorService implements ActionableService {

	private static final String ID = "iot.challenge.jura.graba.aggregator";

	/**
	 * Service control topic (graba/aggregator)
	 */
	public static final String CONTROL_TOPIC = GrabaService.getControlTopic("aggregator");

	////
	//
	// ActionRecorder trait
	//
	//
	@Override
	public String getID() {
		return ID;
	}

	////
	//
	// Parameters
	//
	//
	protected CloudClient cloudClient;
	protected Options options;
	protected ScheduledExecutorService worker;
	protected Future<?> handler;
	protected Recording aggregation;
	protected List<Recording> recordings;
	protected boolean subscribed = false;

	public CloudClient getCloudClient() {
		return cloudClient;
	}

	public void setCloudClient(CloudClient cloudClient) {
		this.cloudClient = cloudClient;
	}

	public Options getOptions() {
		return options;
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
		worker = Executors.newScheduledThreadPool(1);
		connectToCloud(cloudService, Options.PROPERTY_APPLICATION_DEFAULT);
	}

	protected void deactivate() {
		shutdownWorker();
		releaseCloudConnection();
	}

	protected void shutdownWorker() {
		if (handler != null)
			handler.cancel(true);

		if (worker != null)
			worker.shutdown();
	}

	@Override
	public void releaseCloudConnection() {
		unsubscribe();
		ActionableService.super.releaseCloudConnection();
	}

	protected void unsubscribe() {
		if (subscribed) {
			synchronized (this) {
				try {
					cloudClient.unsubscribe(options.getSubscriptionA());
					cloudClient.unsubscribe(options.getSubscriptionB());
				} catch (KuraException e) {
					error("Unable to unsubscribe from CloudClient", e);
					cleanControlTopic();
				}
			}
			subscribed = false;
		}
	}

	////
	//
	// ActionableService
	//
	//
	@Override
	public String getControlTopic() {
		return CONTROL_TOPIC;
	}

	@Override
	public iot.challenge.jura.util.Options createOptions(Map<String, Object> configuration) {
		return new Options(configuration);
	}

	@Override
	public void cancelService() {
		unsubscribe();
	}

	@Override
	public void setOptions(iot.challenge.jura.util.Options options) {
		if (options != null) {
			unsubscribe();
			this.options = (Options) options;
			readRecordings();
		} else {
			this.options = null;
		}
	}

	protected void readRecordings() {
		try {
			recordings = new ArrayList<>();
			subscribed = true;
			cloudClient.subscribe(options.getSubscriptionA(), 0);
			cloudClient.subscribe(options.getSubscriptionB(), 0);
			handler = worker.schedule(this::abort, 10, TimeUnit.SECONDS);
		} catch (KuraException e) {
			error("Unable to subscribe to CloudClient", e);
			cleanControlTopic();
			subscribed = false;
		}
	}

	protected void abort() {
		synchronized (this) {
			if (subscribed) {
				unsubscribe();
				cleanControlTopic();
			}
		}
	}

	@Override
	public void onMessageArrived(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain) {
		if (appTopic.equals(CONTROL_TOPIC)) {
			ActionableService.super.onMessageArrived(deviceId, appTopic, payload, qos, retain);

		} else {
			if (subscribed) {
				synchronized (this) {
					if (MqttProcessor.matches(options.getSubscriptionA(), appTopic)
							|| MqttProcessor.matches(options.getSubscriptionB(), appTopic)) {
						recordings.add(Recording.fromString((String) payload.getMetric(Recording.RECORDING)));
						if (recordings.size() == 2) {
							unsubscribe();
							aggregate();
							cleanControlTopic();
						}
					}
				}
			}
		}
	}

	protected void aggregate() {
		try {
			aggregation = Recording.aggregate(recordings, options.getPublication());
			cloudClient.publish(options.getPublication(), generatePayload(), 0, true);
		} catch (KuraException e) {
			error("Unable to publish", e);
			cloudClient = null;
		}
	}

	protected KuraPayload generatePayload() {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());
		payload.addMetric(Recording.TOPIC, aggregation.getTopic());
		payload.addMetric(Recording.START_TIME, aggregation.getStartTime());
		payload.addMetric(Recording.DURATION, aggregation.getDuration());
		payload.addMetric(Recording.RECORDING, aggregation.toString());
		return payload;
	}

}
