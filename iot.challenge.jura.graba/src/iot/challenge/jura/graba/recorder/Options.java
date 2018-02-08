package iot.challenge.jura.graba.recorder;

import java.util.Map;

/**
 * Recorder service options
 */
public class Options extends iot.challenge.jura.graba.Options {

	public static final String PROPERTY_RECORDING_TIME = "recording.time";

	public static final String PROPERTY_SUBSCRIPTION_DEFAULT = "scanner";
	public static final String PROPERTY_PUBLICATION_DEFAULT = "recording";
	public static final int PROPERTY_RECORDING_TIME_DEFAULT = 300;

	protected final int recordingTime;

	public Options(Map<String, Object> properties) {
		super(properties);
		recordingTime = read(PROPERTY_RECORDING_TIME, PROPERTY_RECORDING_TIME_DEFAULT);
	}

	public int getRecordingTime() {
		return recordingTime;
	}
}
