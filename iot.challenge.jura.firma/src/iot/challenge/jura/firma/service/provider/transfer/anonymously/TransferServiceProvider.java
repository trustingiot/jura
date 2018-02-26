package iot.challenge.jura.firma.service.provider.transfer.anonymously;

import iot.challenge.jura.firma.service.provider.transfer.Dated;
import iot.challenge.jura.firma.service.provider.transfer.Message;
import jota.model.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import com.eclipsesource.json.Json;

import iot.challenge.jura.firma.crypto.EncryptedTransaction;
import iot.challenge.jura.firma.service.TransferService;

/**
 * Provider for anonymous transactions
 */
public class TransferServiceProvider
		extends iot.challenge.jura.firma.service.provider.transfer.publicly.TransferServiceProvider
		implements TransferService {

	////
	//
	// Action recorder
	//
	//
	public static final String ID = "iot.challenge.jura.firma.transfer.anonymously";

	@Override
	public String getID() {
		return ID;
	}

	////
	//
	// Parameters
	//
	//
	protected Map<String, Integer> addressIndex;
	protected Map<String, EncryptedTransaction> addressTransactions;

	////
	//
	// Functionality
	//
	//
	protected void activate(Map<String, Object> properties) {
		addressTransactions = new HashMap<>();
		super.activate(properties);
	}

	@Override
	protected void createOptions(Map<String, Object> properties) {
		Options newOptions = new Options(properties);
		resetAddreses((Options) options, newOptions);
		options = newOptions;

	}

	protected void resetAddreses(Options oldOptions, Options newOptions) {
		if (oldOptions != null) {
			if (!newOptions.getSalt().equals(oldOptions.getSalt()))
				addressTransactions.clear();
		}
	}

	// For each transaction, buildMessage and getAddress are called. buildMessage is
	// called before getAddress, so 'end(.)' is called here
	@Override
	protected String buildMessage(Dated<Message> message) {
		String device = message.getElement().getDevice();
		try {
			return end(device).encrypt(super.buildMessage(message));
		} catch (Exception e) {
			return null;
		}

	}

	protected EncryptedTransaction end(String device) throws Exception {
		return end(getEncryptedTransaction(device));
	}

	protected EncryptedTransaction getEncryptedTransaction(String device) {
		EncryptedTransaction result = addressTransactions.get(device);
		if (result == null) {
			addressTransactions.put(device, result = new EncryptedTransaction(((Options) options).getSalt(), device));
		}
		return result;
	}

	protected EncryptedTransaction end(EncryptedTransaction encryptedTransaction) throws Exception {
		Function<EncryptedTransaction, List<Transaction>> generator = transaction -> {
			try {
				return iotaService.getTransactions(transaction.getAddress());
			} catch (Exception e) {
				return null;
			}
		};
		Predicate<Transaction> filter = transaction -> {
			try {
				String message = iotaService.extractMessage(transaction);
				message = encryptedTransaction.decrypt(message).trim();
				Json.parse(message).asObject();
				return true; // It is a JSON encrypted with this key
			} catch (Exception e) {
				return false;
			}
		};
		return encryptedTransaction.end(generator, filter);
	}

	@Override
	protected String getAddress(String device) {
		return getEncryptedTransaction(device).getAddress();
	}
}
