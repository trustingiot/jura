package iot.challenge.jura.ubica.service.provider.positioning;

import java.util.Map;

import iot.challenge.jura.ubica.trilaterization.DistanceAlgorithm;
import iot.challenge.jura.ubica.trilaterization.LeastSquaresAlgorithm;

/**
 * PositioningService options
 */
public class Options extends iot.challenge.jura.util.Options {

	public static final String PROPERTY_MIN_SCANNERS = "min.scanners";

	public static final int PROPERTY_MIN_SCANNERS_DEFAULT = 4;

	protected final LeastSquaresAlgorithm leastSquaresAlgorithm;

	protected final DistanceAlgorithm distanceAlgorithm;

	protected final int minScanners;

	public Options(Map<String, Object> properties) {
		super(properties);
		leastSquaresAlgorithm = LeastSquaresAlgorithm.readLeastSquaresAlgorithm(properties);
		distanceAlgorithm = DistanceAlgorithm.readDistanceAlgorithm(properties);
		minScanners = read(PROPERTY_MIN_SCANNERS, PROPERTY_MIN_SCANNERS_DEFAULT);
	}

	public LeastSquaresAlgorithm getLeastSquaresAlgorithm() {
		return leastSquaresAlgorithm;
	}

	public DistanceAlgorithm getDistanceAlgorithm() {
		return distanceAlgorithm;
	}

	public int getMinScanners() {
		return minScanners;
	}

}
