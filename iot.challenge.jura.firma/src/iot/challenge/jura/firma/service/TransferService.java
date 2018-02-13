package iot.challenge.jura.firma.service;

import java.util.function.Consumer;

import jota.dto.response.SendTransferResponse;
import jota.model.Transfer;

/**
 * Service to transfer information to IOTA
 */
public interface TransferService {

	/**
	 * Reports if the service is sending a transfer
	 *
	 * @return True if it is sending a transfer, False otherwise
	 */
	boolean sendingTransfer();

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
}
