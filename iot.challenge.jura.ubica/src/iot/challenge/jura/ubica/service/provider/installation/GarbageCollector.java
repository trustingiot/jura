package iot.challenge.jura.ubica.service.provider.installation;

import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import iot.challenge.jura.faro.BeaconEvent;
import iot.challenge.jura.ubica.service.InstallationService;
import iot.challenge.jura.util.trait.ActionRecorder;
import iot.challenge.jura.util.trait.LogLevel;

/**
 * Removes old beacon's events from installation. An event is old if it happened
 * more than 'time' milliseconds ago when {@link GarbageCollector#gc()} is
 * called
 */
public class GarbageCollector implements ActionRecorder {

	public static final String ID = "installation.garbage.collector";

	protected InstallationService installationService;
	protected ScheduledExecutorService worker;
	protected Future<?> nextExecutionHandle;
	protected long time;
	protected boolean canceled;

	public GarbageCollector(InstallationService installationService, long time) {
		super();
		this.installationService = installationService;
		this.worker = Executors.newScheduledThreadPool(1);
		this.time = time;
		canceled = false;
		executeGC();
	}

	@Override
	public String getID() {
		return ID;
	}

	public void scheduleNextExecution() {
		if (!canceled)
			nextExecutionHandle = worker.schedule(this::executeGC, time, TimeUnit.SECONDS);
	}

	private void executeGC() {
		performRegisteredAction(LogLevel.debug, "Executed", this::gc);
	}

	private void gc() {
		if (!canceled) {
			synchronized (installationService) {
				long time = System.currentTimeMillis() - (this.time * 1000l);
				Map<String, NavigableMap<Long, Map<String, BeaconEvent>>> events = installationService.getEvents();

				Set<String> toRemove = new HashSet<>();

				events.forEach((beacon, beaconEvents) -> {
					Long toKey = beaconEvents.floorKey(time);
					if (toKey != null) {
						NavigableMap<Long, Map<String, BeaconEvent>> headMap = beaconEvents.headMap(toKey, true);
						headMap.values().forEach(Map::clear);
						headMap.clear();
					}
					if (beaconEvents.isEmpty()) {
						toRemove.add(beacon);
					}
				});
				toRemove.forEach(events::remove);
			}
			scheduleNextExecution();
		}
	}

	public void cancel() {
		if (!canceled)
			performRegisteredAction(LogLevel.debug, "Cancel", this::cancelGarbageCollector);
	}

	private void cancelGarbageCollector() {
		canceled = true;
		shutdownWorker();
	}

	protected void shutdownWorker() {
		if (nextExecutionHandle != null)
			nextExecutionHandle.cancel(true);

		if (worker != null)
			worker.shutdown();
	}

}
