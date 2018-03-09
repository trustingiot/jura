package iot.challenge.jura.worker.iota;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import cfb.pearldiver.PearlDiver;
import cfb.pearldiver.PearlDiverLocalPoW;
import iot.challenge.jura.util.trait.Loggable;
import jota.IotaAPI;
import jota.IotaLocalPoW;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;
import jota.model.Transfer;
import jota.utils.TrytesConverter;

/**
 * IOTA node
 */
public class IotaNode implements Loggable {

	public static final String REJECT = "reject";
	public static final String MESSAGE = "message";

	public static final int READ_REJECT_API_EXCEPTION = 0;
	public static final int READ_REJECT_NOT_FOUND = 1;

	public static final int ADDRESS_LENGTH = 81;

	////
	//
	// Parameters
	//
	//
	protected IotaAPI api;
	protected IotaLocalPoW pow;
	protected PearlDiver diver;

	protected String protocol;
	protected String host;
	protected String port;
	protected String seed;

	protected ExecutorService transferWorker;
	protected Future<?> transferHandle;
	protected Consumer<String> callback;

	protected boolean wip;
	protected boolean interrupted;

	protected List<Transfer> transfers;

	public IotaNode() throws Exception {
		super();
		createPoW();
		wip = interrupted = false;
	}

	public IotaNode(String protocol, String host, String port, String seed) throws Exception {
		this();
		setConfiguration(protocol, host, port, seed);
	}

	/**
	 * Set IOTA node configuration
	 * 
	 * @param protocol
	 *            Protocol
	 * @param host
	 *            Host
	 * @param port
	 *            Port
	 * @param seed
	 *            IOTA's seed
	 */
	public void setConfiguration(String protocol, String host, String port, String seed) {
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.seed = seed;
		updateAPI();
	}

	protected void createPoW() throws Exception {
		try {
			pow = new PearlDiverLocalPoW();
			Field field = Arrays.asList(PearlDiverLocalPoW.class.getDeclaredFields())
					.stream()
					.filter(f -> f.getType().equals(PearlDiver.class))
					.findFirst()
					.orElse(null);

			if (field == null)
				throw new IllegalArgumentException();

			boolean flag = field.isAccessible();
			field.setAccessible(true);
			diver = (PearlDiver) field.get(pow);
			field.setAccessible(flag);

		} catch (Exception e) {
			pow = null;
			diver = null;
			throw e;
		}
	}

	protected void updateAPI() {
		if (validParameters() && shouldUpdateAPI()) {
			shutdownWorker();
			api = new IotaAPI.Builder()
					.localPoW(pow)
					.protocol(protocol)
					.host(host)
					.port(port)
					.build();
		}
	}

	protected void shutdownWorker() {
		synchronized (this) {
			interrupt();

			if (transferHandle != null) {
				transferHandle.cancel(true);
				transferHandle = null;
			}

			if (transferWorker != null) {
				transferWorker.shutdown();
				transferWorker = null;
			}
		}
	}

	/**
	 * Interrupt transaction
	 */
	public void interrupt() {
		if (wip) {
			interrupted = true;
			diver.cancel();
		}
		wip = false;
	}

	protected boolean validParameters() {
		return protocol != null && host != null && port != null;
	}

	protected boolean shouldUpdateAPI() {
		if (api == null)
			return true;

		if (!api.getProtocol().equals(protocol))
			return true;

		if (!api.getHost().equals(host))
			return true;

		if (!api.getPort().equals(port))
			return true;

		return false;
	}

	/**
	 * Generate valid IOTA's address
	 * 
	 * @param address
	 *            Original address
	 * 
	 * @return Valid address
	 */
	public static String generateValidAddress(String address) {
		if (address == null)
			address = "";

		if (address.length() > ADDRESS_LENGTH)
			address.substring(0, ADDRESS_LENGTH);

		while (address.length() < ADDRESS_LENGTH)
			address = '9' + address;

		return address.toUpperCase();
	}

	/**
	 * Transfer information to IOTA
	 *
	 * @param address
	 *            IOTA address
	 * @param message
	 *            Message
	 * @param callback
	 *            Callback function
	 */
	public void transfer(String address, String message, Consumer<String> callback) {
		if (transferWorker == null)
			transferWorker = Executors.newScheduledThreadPool(1);

		this.callback = callback;
		Transfer transfer = new Transfer(address, 0, TrytesConverter.toTrytes(message), "");
		transfers = Arrays.asList(transfer);
		transferHandle = transferWorker.submit(this::sendTransfer);
	}

	protected void sendTransfer() {
		SendTransferResponse response = null;
		wip = true;
		try {
			info("Started transmission");
			interrupted = false;
			response = sendToIOTA(transfers);
			callback.accept(response.getTransactions().get(0).getHash());
		} catch (Exception e) {
			if (!interrupted) {
				error("Transfer fail");
			}
			callback.accept(null);
		}

		transferHandle = null;
		wip = interrupted = false;
	}

	protected SendTransferResponse sendToIOTA(List<Transfer> transfers) throws ArgumentException {
		return api.sendTransfer(
				seed, // Seed
				2, // Security
				9, // Depth
				14, // Min weight
				transfers, // Transfers
				null, // Inputs
				null, // Remainders
				false); // Validate inputs
	}

	/**
	 * Read message from IOTA transaction
	 * 
	 * @param hash
	 *            IOTA transaction hash
	 *
	 * @return Message or null if the transaction does not contain a valid message
	 */
	public JsonObject readMessage(String hash) {
		if (hash != null) {
			JsonObject result = new JsonObject();

			List<Transaction> transactions;
			try {
				transactions = api.findTransactionsObjectsByHashes(new String[] { hash });
			} catch (Exception e) {
				result.add(REJECT, READ_REJECT_API_EXCEPTION);
				return result;
			}

			Transaction transaction = transactions.get(0);
			if (!hash.equals(transaction.getHash())) {
				result.add(REJECT, READ_REJECT_NOT_FOUND);
				return result;
			}

			String message = extractMessage(transaction);
			try {
				result.add(MESSAGE, Json.parse(message.trim()).asObject());
			} catch (Exception e) {
				result.add(MESSAGE, message);
			}

			return result;
		}

		return null;
	}

	/**
	 * Extract transaction message
	 * 
	 * @param transaction
	 *            IOTA transaction
	 * 
	 * @return Transaction message
	 */
	public String extractMessage(Transaction transaction) {
		String message = transaction.getSignatureFragments();
		// FIXME transaction.getSignatureFragments().length == 2187 (it must be 2188)
		// bug in JOTA?
		while (message.length() < 2188)
			message += '9';
		return TrytesConverter.toString(message).trim();
	}

	/**
	 * Return all address transactions
	 * 
	 * @param address
	 *            IOTA Address
	 * 
	 * @return List of transactions
	 */
	public List<Transaction> getTransactions(String address) throws Exception {
		return (address != null) ? api.findTransactionObjectsByAddresses(new String[] { address }) : new ArrayList<>();
	}

}
