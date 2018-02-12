package iot.challenge.jura.ubica.service.provider.location;

import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import iot.challenge.jura.ubica.installation.Point;
import iot.challenge.jura.ubica.service.LocationService;
import iot.challenge.jura.util.trait.ActionRecorder;
import iot.challenge.jura.util.trait.LogLevel;

/**
 * Removes old beacon's locations. A location is old if it happened more than
 * 'time' milliseconds ago when {@link GarbageCollector#gc()} is called
 */
public class GarbageCollector implements ActionRecorder {

	public static final String ID = "location.garbage.collector";

	protected LocationService locationService;
	protected ScheduledExecutorService worker;
	protected Future<?> nextExecutionHandle;
	protected long time;
	protected boolean canceled;

	public GarbageCollector(LocationService locationService, long time) {
		super();
		this.locationService = locationService;
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
			synchronized (locationService) {
				long time = System.currentTimeMillis() - (this.time * 1000l);
				Map<String, NavigableMap<Long, Map<String, Point>>> locations = locationService.getLocations();

				Set<String> toRemove = new HashSet<>();

				locations.forEach((installation, installationLocations) -> {
					Long toKey = installationLocations.floorKey(time);
					if (toKey != null) {
						NavigableMap<Long, Map<String, Point>> headMap = installationLocations.headMap(toKey, true);
						headMap.values().forEach(Map::clear);
						headMap.clear();
					}
					if (installationLocations.isEmpty()) {
						toRemove.add(installation);
					}
				});
				toRemove.forEach(locations::remove);
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
