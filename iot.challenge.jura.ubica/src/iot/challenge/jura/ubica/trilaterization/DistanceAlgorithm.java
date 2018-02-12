package iot.challenge.jura.ubica.trilaterization;

import java.util.Map;

import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;

import iot.challenge.jura.faro.BeaconEvent;
import iot.challenge.jura.faro.BeaconHelper;
import iot.challenge.jura.faro.Protocol;

/**
 * Algorithm used to compute BLE beacon distance
 */
public enum DistanceAlgorithm {
	Linear("Linear") {
		@Override
		protected double distance(double rssi, double txpower) {
			double ratioDB = txpower - rssi;
			double ratioLinear = Math.pow(10, (double) ratioDB / 10);
			return Math.sqrt(ratioLinear);
		}
	},
	Accuracy("Accuracy") {
		@Override
		protected double distance(double rssi, double txpower) {
			double distance = 0d;

			double ratio = rssi / txpower;
			if (ratio < 1d) {
				distance = Math.pow(ratio, 10);
			} else {
				distance = 0.89976 * Math.pow(ratio, 7.7095) + 0.111;
			}

			return distance;
		}
	};

	private String name;

	private DistanceAlgorithm(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static final String PROPERTY_DISTANCE_ALGORITHM = "distance.algorithm";
	public static final DistanceAlgorithm PROPERTY_DISTANCE_ALGORITHM_DEFAULT = Linear;

	public static DistanceAlgorithm readDistanceAlgorithm(Map<String, Object> properties) {
		return valueOf((String) properties.getOrDefault(PROPERTY_DISTANCE_ALGORITHM,
				PROPERTY_DISTANCE_ALGORITHM_DEFAULT.name()));
	}

	public double computeDistance(BeaconEvent event) {
		BluetoothLeBeacon beacon = event.getBeacon();
		Protocol protocol = Protocol.readProtocol(beacon);
		BeaconHelper helper = protocol.getGenericHelper();
		return distance(beacon.getRssi(), helper.getTxPower(beacon));
	}

	public double computeDistance(BeaconEvent event, DistanceUnits units) {
		return DistanceUnits.convert(
				computeDistance(event),
				DistanceUnits.METERS,
				DistanceUnits.MILIMETERS);
	}

	abstract double distance(double rssi, double txpower);
}
