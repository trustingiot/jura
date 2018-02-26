package iot.challenge.jura.ubica.service.provider.positioning;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;

import iot.challenge.jura.faro.BeaconEvent;
import iot.challenge.jura.ubica.installation.Installation;
import iot.challenge.jura.ubica.installation.Point;
import iot.challenge.jura.ubica.installation.Scanner;
import iot.challenge.jura.ubica.service.InstallationService;
import iot.challenge.jura.ubica.service.PositioningService;
import iot.challenge.jura.ubica.trilaterization.DistanceUnits;
import iot.challenge.jura.util.trait.ActionRecorder;

/**
 * PositioningService provider
 */
public class PositioningServiceProvider implements PositioningService, ActionRecorder, ConfigurableComponent {

	////
	//
	// Action recorder
	//
	//
	public static final String ID = "iot.challenge.jura.ubica.positioning";

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

	////
	//
	// Registered services
	//
	//
	protected InstallationService installationService;

	protected void setInstallationService(InstallationService service) {
		installationService = service;
	}

	protected void unsetInstallationService(InstallationService service) {
		installationService = null;
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
	private void activate(Map<String, Object> properties) {
		options = new Options(properties);
	}

	private void update(Map<String, Object> properties) {
		options = new Options(properties);
	}

	private void deactivate() {
		// Nothing to do
	}

	public Point position(String installationId, List<String> scannersAddr, List<BeaconEvent> events) {
		if (scannersAddr.size() < options.getMinScanners())
			return null;

		return options.getLeastSquaresAlgorithm().computePosition(
				obtainPositions(
						installationService.getInstallation(installationId),
						scannersAddr),
				obtainDistances(events));
	}

	protected static double[][] obtainPositions(Installation installation, List<String> scannersAddr) {
		return scannersAddr.stream()
				.map(it -> findScannerByAddr(installation.getScanners(), it))
				.map(Scanner::getPosition)
				.map(point -> new double[] { point.getX(), point.getY() })
				.toArray(double[][]::new);
	}

	protected static Scanner findScannerByAddr(List<Scanner> scanners, String addr) {
		return scanners.stream()
				.filter(scanner -> addr.equals(scanner.getAddr()))
				.findFirst()
				.orElse(null);
	}

	protected double[] obtainDistances(List<BeaconEvent> events) {
		return events.stream()
				.mapToDouble(event -> options.getDistanceAlgorithm().computeDistance(event, DistanceUnits.MILIMETERS))
				.toArray();
	}
}
