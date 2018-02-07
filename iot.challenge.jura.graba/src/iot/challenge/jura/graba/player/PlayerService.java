package iot.challenge.jura.graba.player;

import java.util.Map;

import java.util.TimerTask;

import iot.challenge.jura.graba.GrabaService;

/**
 * Actionable service capable of play a recording
 */
public class PlayerService extends GrabaService {

	private static final String ID = "iot.challenge.jura.graba.player";

	/**
	 * Service control topic (graba/player)
	 */
	public static final String CONTROL_TOPIC = getControlTopic("player");

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
		return new PlaybackTask(this);
	}

	@Override
	protected iot.challenge.jura.graba.Options createSpecificOptions(Map<String, Object> configuration) {
		return new Options(configuration);
	}
}
