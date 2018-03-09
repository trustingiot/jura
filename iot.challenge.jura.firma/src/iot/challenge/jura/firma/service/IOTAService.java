package iot.challenge.jura.firma.service;

import java.util.List;
import java.util.function.Consumer;

import com.eclipsesource.json.JsonObject;

import jota.model.Transaction;

/**
 * Service to interact with IOTA
 */
public interface IOTAService {

	public static final String TRANSACTION_EXPLORER = "https://thetangle.org/transaction/";

	/**
	 * Reports if the service can be used
	 * 
	 * @return True if it can be used, False otherwise
	 */
	boolean ready();

	/**
	 * Interrupt current transfer
	 */
	void interrupt();

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
	void transfer(String address, String message, Consumer<String> callback);

	/**
	 * Read message from IOTA transaction
	 * 
	 * @param hash
	 *            IOTA transaction hash
	 *
	 * @return Message or null if the transaction does not contain a valid message
	 */
	JsonObject readMessage(String hash);

	/**
	 * Return all address transactions
	 * 
	 * @param address
	 *            IOTA Address
	 * 
	 * @return List of transactions
	 */
	List<Transaction> getTransactions(String address) throws Exception;

	/**
	 * Extract transaction message
	 * 
	 * @param transaction
	 *            IOTA transaction
	 * 
	 * @return Transaction message
	 */
	String extractMessage(Transaction transaction);
}
