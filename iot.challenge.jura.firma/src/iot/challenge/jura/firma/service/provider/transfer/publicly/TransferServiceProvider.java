package iot.challenge.jura.firma.service.provider.transfer.publicly;

import iot.challenge.jura.firma.service.SignService;
import iot.challenge.jura.firma.service.provider.transfer.Candidate;
import iot.challenge.jura.firma.service.provider.transfer.Dated;
import iot.challenge.jura.firma.service.provider.transfer.Message;
import iot.challenge.jura.firma.service.provider.transfer.Options;
import iot.challenge.jura.firma.service.provider.transfer.Rule;
import iot.challenge.jura.firma.service.TransferService;
import iot.challenge.jura.firma.service.IOTAService;
import iot.challenge.jura.ubica.installation.Point;
import iot.challenge.jura.util.trait.ActionRecorder;
import iot.challenge.jura.worker.iota.IotaNode;
import iot.challenge.jura.ubica.service.LocationService;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;

import com.eclipsesource.json.JsonObject;

/**
 * Provider for public transactions
 */
public class TransferServiceProvider implements TransferService, ActionRecorder, ConfigurableComponent {

	////
	//
	// Action recorder
	//
	//
	public static final String ID = "iot.challenge.jura.firma.transfer.publicly";

	@Override
	public String getID() {
		return ID;
	}

	////
	//
	// Parameters
	//
	//
	protected Options options;
	protected boolean transferASAP;
	protected Boolean transferInProgress;
	protected ScheduledExecutorService updateWorker;
	protected ScheduledExecutorService transferWorker;
	protected Future<?> updateHandle;
	protected Future<?> transferHandle;
	protected Map<String, Map<String, Long>> last;
	protected Map<String, Map<String, Dated<Point>>> buffer;
	protected Map<Integer, Rule> rules;

	////
	//
	// Registered services
	//
	//
	protected LocationService locationService;
	protected IOTAService iotaService;
	protected SignService signService;

	protected void setLocationService(LocationService service) {
		locationService = service;
	}

	protected void unsetLocationService(LocationService service) {
		locationService = null;
	}

	protected void setIOTAService(IOTAService service) {
		iotaService = service;
	}

	protected void unsetIOTAService(IOTAService service) {
		iotaService = null;
	}

	protected void setSignService(SignService service) {
		signService = service;
	}

	protected void unsetSignService(SignService service) {
		signService = null;
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
		transferASAP = false;
		last = new HashMap<>();
		buffer = new HashMap<>();
		createOptions(properties);
	}

	protected void createOptions(Map<String, Object> properties) {
		options = new Options(properties);
	}

	protected void update(Map<String, Object> properties) {
		stopWorkers();
		createOptions(properties);
		startWorkers();
	}

	protected void stopWorkers() {
		synchronized (this) {
			if (transferHandle != null) {
				iotaService.interrupt();
				transferHandle.cancel(true);
				transferHandle = null;
			}

			if (transferWorker != null) {
				transferWorker.shutdown();
				transferWorker = null;
			}

			if (updateHandle != null) {
				updateHandle.cancel(true);
				updateHandle = null;
			}

			if (updateWorker != null) {
				updateWorker.shutdown();
				updateWorker = null;
			}
		}
	}

	protected void startWorkers() {
		if (options.isEnable()) {
			updateBuffer();
			transfer();
		}
	}

	protected void updateBuffer() {
		synchronized (this) {
			long timeout = System.currentTimeMillis() - (options.getLocationTimeout() * 1000l);

			boolean previouslyEmpty = buffer.isEmpty();

			// Clean older locations
			Set<String> installationsToRemove = new HashSet<>();
			buffer.forEach((installation, installationLocations) -> {
				Set<String> beaconsToRemove = new HashSet<>();
				installationLocations.forEach((beacon, location) -> {
					if (location.getTime() < timeout)
						beaconsToRemove.add(beacon);
				});
				beaconsToRemove.forEach(installationLocations::remove);

				if (installationLocations.isEmpty())
					installationsToRemove.add(installation);
			});
			installationsToRemove.forEach(buffer::remove);

			// Update with the newest locations
			synchronized (locationService) {
				Map<String, NavigableMap<Long, Map<String, Point>>> locations = locationService.getLocations();
				locations.forEach((installation, installationLocations) -> {
					installationLocations.forEach((time, instantLocations) -> {
						if (time >= timeout) {
							instantLocations.forEach((beacon, location) -> {
								if (isNewLocation(installation, beacon, time)) {
									if (!buffer.containsKey(installation))
										buffer.put(installation, new HashMap<>());

									Dated<Point> newDP = new Dated<Point>(time, location);
									Dated<Point> oldDP = buffer.get(installation).get(beacon);
									if (oldDP != null) {
										newDP = ((newDP.compareTo(oldDP)) > 0) ? newDP : oldDP;
									}

									buffer.get(installation).put(beacon, newDP);
								}
							});
						}
					});
				});
			}

			if (previouslyEmpty && !buffer.isEmpty()) {
				rescheduleTransfer(true);
			}

			updateHandle = null;
		}
		scheduleUpdate();
	}

	protected void rescheduleTransfer(boolean asap) {
		if (iotaService.ready()) {
			if (transferHandle != null) {
				transferHandle.cancel(true);
				transferHandle = null;
			}
			transferASAP = asap;
			scheduleTransfer();
		}
	}

	protected boolean isNewLocation(String installation, String beacon, long time) {
		if (last.containsKey(installation) && last.get(installation).containsKey(beacon)) {
			return time > last.get(installation).get(beacon);
		}

		return true;
	}

	protected void scheduleUpdate() {
		if (updateWorker == null) {
			updateWorker = Executors.newScheduledThreadPool(1);
		}

		updateHandle = updateWorker.schedule(this::updateBuffer, options.getUpdateRate(), TimeUnit.SECONDS);
	}

	protected void transfer() {
		synchronized (this) {
			if (iotaService.ready()) {
				transferASAP = false;
				Dated<Message> location = selectLocationToTransfer();
				if (location != null) {
					updateLast(location);
					removeFromBuffer(location);
					transferLocation(location);
				}

			} else {
				transferASAP = true;
			}

			transferHandle = null;
		}
		scheduleTransfer();
	}

	protected Dated<Message> selectLocationToTransfer() {
		Queue<Candidate<Message>> candidates = new PriorityQueue<>();

		synchronized (this) {
			for (String installation : buffer.keySet()) {
				for (String beacon : buffer.get(installation).keySet()) {
					candidates.add(createCandidate(installation, beacon));
				}
			}
		}

		Candidate<Message> candidate = candidates.poll();
		return (candidate != null) ? candidate.getDated() : null;
	}

	protected Candidate<Message> createCandidate(String installation, String beacon) {
		return new Candidate<Message>(
				(!last.containsKey(installation) || !last.get(installation).containsKey(beacon)) ? 0 : 1,
				createDatedMessage(installation, beacon));
	}

	protected Dated<Message> createDatedMessage(String installation, String beacon) {
		Dated<Point> location = buffer.get(installation).get(beacon);
		return new Dated<Message>(
				location.getTime(),
				new Message(installation, beacon, location.getElement()));
	}

	protected void updateLast(Dated<Message> next) {
		String i = next.getElement().getInstallation();
		String d = next.getElement().getDevice();

		if (!last.containsKey(i))
			last.put(i, new HashMap<>());

		last.get(i).put(d, next.getTime());
	}

	protected void removeFromBuffer(Dated<Message> next) {
		String i = next.getElement().getInstallation();
		String d = next.getElement().getDevice();

		buffer.get(i).remove(d);
		if (buffer.get(i).isEmpty())
			buffer.remove(i);
	}

	protected void transferLocation(Dated<Message> message) {
		String iotaMessage = buildMessage(message);
		if (iotaMessage != null) {
			iotaService.transfer(getAddress(message.getElement().getDevice()), iotaMessage,
					(r) -> doAfterTransfer(message.getElement(), r));
		} else {
			error("The location could not be sent");
			rescheduleTransfer(true);
		}
	}

	protected String buildMessage(Dated<Message> message) {
		JsonObject body = new JsonObject();
		body.add("timestamp", message.getTime());
		body.add("location", message.getElement().toJson());
		return signService.sign(body).toString();
	}

	protected String getAddress(String device) {
		return IotaNode.generateValidAddress(options.getAddress());
	}

	protected void doAfterTransfer(Message message, String hash) {
		logTransfer(message, hash);
		if (transferASAP || hash == null)
			rescheduleTransfer(false);
	}

	protected void logTransfer(Message message, String hash) {
		String location = MessageFormat.format("[{0},{1} = {2}]",
				message.getInstallation(),
				message.getDevice(),
				message.getLocation());

		if (hash != null && !hash.isEmpty()) {
			debug("Location {} transferred -> {}/{}", location, IOTAService.TRANSACTION_EXPLORER, hash);

		} else {
			error("The transfer of the location {} to IOTA failed", location);
		}
	}

	protected void scheduleTransfer() {
		if (transferWorker == null) {
			transferWorker = Executors.newScheduledThreadPool(1);
		}

		transferHandle = transferWorker.schedule(
				this::transfer,
				transferASAP ? 5 : options.getPublicationRate(),
				TimeUnit.SECONDS);
	}

	protected void deactivate() {
		stopWorkers();
	}
}
