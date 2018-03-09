package iot.challenge.jura.worker.iota;

import iot.challenge.jura.util.MqttProcessor;
import iot.challenge.jura.util.trait.ActionRecorder;
import iot.challenge.jura.util.trait.DataServiceAdapter;
import jota.model.Transfer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloud.CloudPayloadProtoBufEncoder;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;

import static iot.challenge.jura.worker.iota.WorkerAPI.*;

/**
 * Worker
 */
public class Worker implements ActionRecorder, ConfigurableComponent, DataServiceAdapter {

	////
	//
	// Action recorder
	//
	//
	public static final String ID = "iot.challenge.jura.worker.iota";

	@Override
	public String getID() {
		return ID;
	}

	////
	//
	// Parameters
	//
	//
	protected IotaNode node;

	protected Options options;

	protected String protocol;
	protected String host;
	protected String port;
	protected String seed;

	protected KuraPayload buffer = null;

	protected List<Transfer> transfers;
	protected boolean wip;
	protected boolean interrupted = false;

	protected ExecutorService transferWorker;
	protected Future<?> transferHandle;

	////
	//
	// Registered services
	//
	//
	protected DataService dataService;
	protected CloudPayloadProtoBufDecoder decoder;
	protected CloudPayloadProtoBufEncoder encoder;

	protected void setDataService(DataService service) {
		dataService = service;
	}

	protected void unsetDataService(DataService service) {
		dataService = null;
	}

	protected void setCloudPayloadProtoBufDecoder(CloudPayloadProtoBufDecoder decoder) {
		this.decoder = decoder;
	}

	protected void unsetCloudPayloadProtoBufDecoder(CloudPayloadProtoBufDecoder decoder) {
		this.decoder = null;
	}

	protected void setCloudPayloadProtoBufEncoder(CloudPayloadProtoBufEncoder encoder) {
		this.encoder = encoder;
	}

	protected void unsetCloudPayloadProtoBufEncoder(CloudPayloadProtoBufEncoder encoder) {
		this.encoder = null;
	}

	////
	//
	// Service methods
	//
	//
	protected void activate(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Activating", this::activate, properties);
	}

	protected void updated(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Updating", this::update, properties);
	}

	protected void deactivate(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Deactivating", this::deactivate);
	}

	////
	//
	// Functionality
	//
	//
	protected void activate(Map<String, Object> properties) {
		createOptions(properties);
		startSubscription();
	}

	protected void createOptions(Map<String, Object> properties) {
		options = new Options(properties);
		updateAPI();
	}

	protected void updateAPI() {
		if (options != null) {
			if (node == null) {
				try {
					node = new IotaNode();
				} catch (Exception e) {
					error("Create IOTA node failed", e);
					node = null;
				}
			}
			node.setConfiguration(protocol, host, port, seed);
		}
	}

	protected void startSubscription() {
		try {
			dataService.addDataServiceListener(this);
			dataService.subscribe(CONFIG_TOPIC, 0);
			dataService.subscribe(SUB(TODO_TOPIC, options.getID()), 0);
			notifyWorker();
		} catch (KuraException e) {
			error("Unable to subscribe to topics", e);
			dataService.removeDataServiceListener(this);
		}
	}

	protected void notifyWorker() throws KuraException {
		KuraPayload payload = new KuraPayload();
		payload.addMetric("free", true);
		dataService.publish(SUB(WORKER_TOPIC, options.getID()), encoder.getBytes(payload, false), 2,
				true, 0);
	}

	protected void update(Map<String, Object> properties) {
		node.interrupt();
		String oldId = options.getID();
		createOptions(properties);
		if (!options.getID().equals(oldId)) {
			try {
				dataService.publish(SUB(WORKER_TOPIC, oldId), new byte[0], 2, true, 0);
				notifyWorker();
			} catch (KuraException e) {
				error("Unable to notify worker", e);
			}
		}
	}

	protected void deactivate() {
		node.interrupt();
		dataService.removeDataServiceListener(this);
	}

	@Override
	public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
		if (MqttProcessor.matches(CONFIG_TOPIC, topic)) {
			configure(payload);

		} else if (MqttProcessor.matches(SUB(TODO_TOPIC, options.getID()), topic)) {
			if (payload.length == 0) {
				node.interrupt();
			} else {
				todo(payload);
			}
		}
	}

	protected void configure(byte[] payload) {
		try {
			KuraPayload kuraPayload = decoder.buildFromByteArray(payload);
			host = (String) kuraPayload.getMetric(HOST_PROPERTY);
			port = (String) kuraPayload.getMetric(PORT_PROPERTY);
			protocol = (String) kuraPayload.getMetric(PROTOCOL_PROPERTY);
			seed = (String) kuraPayload.getMetric(SEED_PROPERTY);
			updateAPI();
		} catch (KuraException e) {
			error("Unable to configure");
		}
	}

	protected void todo(byte[] payload) {
		try {
			KuraPayload buffer = decoder.buildFromByteArray(payload);
			String address = (String) buffer.getMetric("address");
			String message = (String) buffer.getMetric("message");
			node.transfer(address, message, this::done);
		} catch (KuraException e) {
			error("Unable to decode");
		}
	}

	protected void done(String hash) {
		String workerID = options.getID();

		KuraPayload payload = new KuraPayload();
		payload.addMetric("worker", workerID);
		payload.addMetric("hash", hash);
		try {
			dataService.publish(SUB(DONE_TOPIC, workerID), encoder.getBytes(payload, false), 2, true, 0);
		} catch (Exception e) {
			error("Unable to notify result");
		}
		buffer = null;
	}
}
