package iot.challenge.jura.worker.iota;

/**
 * Worker's API
 */
public interface WorkerAPI {
	public static String SUB(String topic, String sub) {
		return topic + "/" + sub;
	};

	public static String ALL(String topic) {
		return SUB(topic, "#");
	};

	public static final String BASE_TOPIC = "/jura/cluster/iota";

	public static final String CONFIG_TOPIC = SUB(BASE_TOPIC, "configuration");
	public static final String WORKER_TOPIC = SUB(BASE_TOPIC, "worker");

	public static final String TODO_TOPIC = SUB(BASE_TOPIC, "todo");
	public static final String DONE_TOPIC = SUB(BASE_TOPIC, "done");

	public static final String HOST_PROPERTY = "host";
	public static final String PORT_PROPERTY = "port";
	public static final String PROTOCOL_PROPERTY = "protocol";
	public static final String SEED_PROPERTY = "seed";
}
