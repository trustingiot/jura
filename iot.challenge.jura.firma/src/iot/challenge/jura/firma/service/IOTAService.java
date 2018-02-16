package iot.challenge.jura.firma.service;

import java.util.function.Consumer;

import com.eclipsesource.json.JsonObject;

import jota.dto.response.SendTransferResponse;
import jota.model.Transfer;

/**
 * Service to interact with IOTA
 */
public interface IOTAService {

	/**
	 * Reports if the service is sending a transfer
	 *
	 * @return True if it is sending a transfer, False otherwise
	 */
	boolean isTransferring();

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
	 * Returns selected IOTA address
	 *
	 * @return Selected IOTA address
	 */
	String getAddress();

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
	void transfer(String address, String message, Consumer<SendTransferResponse> callback);

	/**
	 * Transfer information to IOTA
	 *
	 * @param address
	 *            IOTA address
	 * @param message
	 *            Message
	 */
	void transfer(String address, String message);

	/**
	 * Transfer information to IOTA using selected address
	 *
	 * @param message
	 *            Message
	 * @param callback
	 *            Callback function
	 */
	void transfer(String message, Consumer<SendTransferResponse> callback);

	/**
	 * Transfer information to IOTA using selected address
	 *
	 * @param message
	 *            Message
	 */
	void transfer(String message);

	/**
	 * Transfer information to IOTA
	 *
	 * @param transfer
	 *            Transfer
	 * @param callback
	 *            Callback function
	 */
	void transfer(Transfer transfer, Consumer<SendTransferResponse> callback);

	/**
	 * Transfer information to IOTA
	 *
	 * @param transfer
	 *            Transfer
	 */
	void transfer(Transfer transfer);

	/**
	 * Read message from IOTA transaction
	 * 
	 * @param hash
	 *            IOTA transaction hash
	 *
	 * @return Message or null if the transaction does not contain a valid json
	 *         message
	 */
	JsonObject readMessage(String hash);
}
