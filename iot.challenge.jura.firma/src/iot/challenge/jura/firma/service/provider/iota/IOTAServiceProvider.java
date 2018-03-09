package iot.challenge.jura.firma.service.provider.iota;

import iot.challenge.jura.firma.service.IOTAService;
import iot.challenge.jura.util.trait.ActionRecorder;
import iot.challenge.jura.util.trait.DataServiceAdapter;
import iot.challenge.jura.worker.iota.IotaNode;
import iot.challenge.jura.util.MqttProcessor;
import jota.model.Transaction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloud.CloudPayloadProtoBufEncoder;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;

import com.eclipsesource.json.JsonObject;

import static iot.challenge.jura.worker.iota.WorkerAPI.*;

/**
 * IOTAService provider
 */
public class IOTAServiceProvider implements IOTAService, ActionRecorder, ConfigurableComponent, DataServiceAdapter {

	public static final String REJECT = "reject";
	public static final String MESSAGE = "message";

	////
	//
	// Action recorder
	//
	//
	public static final String ID = "iot.challenge.jura.firma.iota";

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

	protected Consumer<String> callback;

	protected Map<String, Long> queued;
	protected Map<String, Boolean> workers;
	protected Map<String, Consumer<String>> callbacks;

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
		queued = new HashMap<>();
		workers = new HashMap<>();
		callbacks = new HashMap<>();
		startSubscription();
		createOptions(properties);
	}

	protected void startSubscription() {
		try {
			dataService.addDataServiceListener(this);
			dataService.subscribe(ALL(WORKER_TOPIC), 0);
			dataService.subscribe(ALL(DONE_TOPIC), 0);
		} catch (KuraException e) {
			error("Unable to subscribe to topics", e);
			dataService.removeDataServiceListener(this);
		}
	}

	protected void createOptions(Map<String, Object> properties) {
		stopExecution();
		options = new Options(properties);
		updateConfiguration();
		updateNode();
	}

	protected void stopExecution() {
		interrupt();
		for (String id : workers.keySet())
			workers.put(id, true);

		queued.clear();
	}

	protected void updateConfiguration() {
		KuraPayload payload = new KuraPayload();
		payload.addMetric(HOST_PROPERTY, options.getIotaNodeHost());
		payload.addMetric(PORT_PROPERTY, options.getIotaNodePort());
		payload.addMetric(PROTOCOL_PROPERTY, options.getIotaNodeProtocol());
		payload.addMetric(SEED_PROPERTY, options.getIotaSeed());
		try {
			dataService.publish(CONFIG_TOPIC, encoder.getBytes(payload, false), 2, true, 1);
		} catch (Exception e) {
			error("Unable to update configuration");
		}
	}

	protected void updateNode() {
		if (node == null) {
			try {
				node = new IotaNode();
			} catch (Exception e) {
				error("Create IOTA node failed", e);
				node = null;
			}
		}
		node.setConfiguration(
				options.getIotaNodeProtocol(),
				options.getIotaNodeHost(),
				options.getIotaNodePort(),
				options.getIotaSeed());
	}

	protected void update(Map<String, Object> properties) {
		createOptions(properties);
	}

	protected void deactivate() {
		stopExecution();
		dataService.removeDataServiceListener(this);
	}

	////
	//
	// Transfer service
	//
	//
	@Override
	public boolean ready() {
		return findFirstFreeWorker() != null;
	}

	protected String findFirstFreeWorker() {
		return workers.keySet().stream().filter(workers::get).findFirst().orElse(null);
	}

	@Override
	public void interrupt() {
		try {
			Map<String, Boolean> aux = new HashMap<>();
			byte[] payload = new byte[0];
			for (String id : workers.keySet()) {
				dataService.publish(SUB(TODO_TOPIC, id), payload, 2, true, 0);
				aux.put(id, true);
			}
			workers = aux;
		} catch (KuraException e) {
			error("Send todo failed", e);
		}
	}

	@Override
	public void transfer(String address, String message, Consumer<String> callback) {
		transfer(findFirstFreeWorker(), address, message, callback);
	}

	protected void transfer(String worker, String address, String message, Consumer<String> callback) {
		try {
			queued.put(worker, System.currentTimeMillis());
			workers.put(worker, false);
			callbacks.put(worker, callback);
			dataService.publish(SUB(TODO_TOPIC, worker), generatePayload(address, message), 2, true, 0);
		} catch (KuraException e) {
			error("Unable to transfer message");
			queued.remove(worker);
			workers.put(worker, true);
			callbacks.remove(worker);
		}
	}

	protected byte[] generatePayload(String address, String message) throws KuraException {
		KuraPayload payload = new KuraPayload();
		payload.addMetric("address", address);
		payload.addMetric("message", message);
		return encoder.getBytes(payload, false);
	}

	@Override
	public JsonObject readMessage(String hash) {
		return node.readMessage(hash);
	}

	@Override
	public String extractMessage(Transaction transaction) {
		return node.extractMessage(transaction);
	}

	@Override
	public List<Transaction> getTransactions(String address) throws Exception {
		return node.getTransactions(address);
	}

	@Override
	public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
		if (MqttProcessor.matches(ALL(WORKER_TOPIC), topic)) {
			assignWorker(topic, payload);

		} else if (MqttProcessor.matches(ALL(DONE_TOPIC), topic)) {
			done(topic, payload);
		}
	}

	protected void assignWorker(String topic, byte[] payload) {
		if (MqttProcessor.matches(ALL(WORKER_TOPIC), topic)) {
			String[] tokens = topic.split("/");
			String id = tokens[tokens.length - 1];

			if (payload.length > 0) {
				workers.put(id, true);
			} else {
				workers.remove(id);
			}
		}
	}

	protected void done(String topic, byte[] payload) {
		try {
			String hash = "";
			String worker = null;
			if (payload != null && payload.length != 0) {
				KuraPayload kuraPayload = decoder.buildFromByteArray(payload);
				worker = (String) kuraPayload.getMetric("worker");
				hash = (String) kuraPayload.getMetric("hash");
			} else {
				String[] tokens = topic.split("/");
				worker = tokens[tokens.length - 1];
			}

			done(worker, hash);

		} catch (KuraException e) {
			error("Unable to decode payload", e);
		}
	}

	protected void done(String worker, String hash) {
		if (queued.containsKey(worker)) {
			if (!hash.isEmpty()) {
				logTransfer(queued.remove(worker), hash);
			}
			workers.put(worker, true);
			Consumer<String> callback = callbacks.remove(worker);
			if (callback != null) {
				callback.accept(hash);
			}
		}
	}

	protected void logTransfer(long time, String hash) {
		info("Transaction => {}{} ({} seconds)",
				TRANSACTION_EXPLORER, hash, (System.currentTimeMillis() - time) / 1000);
	}

}
