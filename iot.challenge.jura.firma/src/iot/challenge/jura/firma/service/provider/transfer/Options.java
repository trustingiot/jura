package iot.challenge.jura.firma.service.provider.transfer;

import java.util.Map;

/**
 * TransferService's options
 */
public class Options extends iot.challenge.jura.util.Options {

	public static final String PROPERTY_IOTA_WALLET_SEED = "iota.wallet.seed";
	public static final String PROPERTY_IOTA_NODE_PROTOCOL = "iota.node.protocol";
	public static final String PROPERTY_IOTA_NODE_HOST = "iota.node.host";
	public static final String PROPERTY_IOTA_NODE_PORT = "iota.node.port";

	public static final String PROPERTY_IOTA_WALLET_SEED_DEFAULT = "IHDEENZYITYVYSPKAURUZAQKGVJEREFDJMYTANNXXGPZ9GJWTEOJJ9IPMXOGZNQLSNMFDSQOTZAEFTUEB";
	public static final String PROPERTY_IOTA_NODE_PROTOCOL_DEFAULT = "http";
	public static final String PROPERTY_IOTA_NODE_HOST_DEFAULT = "iota-tangle.io";
	public static final String PROPERTY_IOTA_NODE_PORT_DEFAULT = "14265"; // IotaAPI.Builder().port(String port)

	protected final String iotaSeed;

	protected final String iotaNodeProtocol;

	protected final String iotaNodeHost;

	protected final String iotaNodePort;

	public Options(Map<String, Object> properties) {
		super(properties);
		iotaSeed = read(PROPERTY_IOTA_WALLET_SEED, PROPERTY_IOTA_WALLET_SEED_DEFAULT);
		iotaNodeProtocol = read(PROPERTY_IOTA_NODE_PROTOCOL, PROPERTY_IOTA_NODE_PROTOCOL_DEFAULT);
		iotaNodeHost = read(PROPERTY_IOTA_NODE_HOST, PROPERTY_IOTA_NODE_HOST_DEFAULT);
		iotaNodePort = read(PROPERTY_IOTA_NODE_PORT, PROPERTY_IOTA_NODE_PORT_DEFAULT);
	}

	public String getIotaSeed() {
		return iotaSeed;
	}

	public String getIotaNodeProtocol() {
		return iotaNodeProtocol;
	}

	public String getIotaNodeHost() {
		return iotaNodeHost;
	}

	public String getIotaNodePort() {
		return iotaNodePort;
	}
}
