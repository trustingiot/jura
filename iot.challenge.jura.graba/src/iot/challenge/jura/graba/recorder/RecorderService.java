package iot.challenge.jura.graba.recorder;

import java.util.Map;
import java.util.TimerTask;

import iot.challenge.jura.graba.GrabaService;

/**
 * Actionable service capable of make a recording
 */
public class RecorderService extends GrabaService {

	private static final String ID = "iot.challenge.jura.graba.recorder";

	/**
	 * Service control topic (graba/recorder)
	 */
	public static final String CONTROL_TOPIC = getControlTopic("recorder");

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
	// Functionality
	//
	//
	@Override
	public String getControlTopic() {
		return CONTROL_TOPIC;
	}

	@Override
	protected TimerTask createTimerTask() {
		return new RecordingTask(this);
	}

	@Override
	protected iot.challenge.jura.graba.Options createSpecificOptions(Map<String, Object> configuration) {
		configuration.put(
				Options.PROPERTY_RECORDING_TIME,
				Integer.parseInt((String) configuration.get(Options.PROPERTY_RECORDING_TIME)));
		return new Options(configuration);
	}

}
