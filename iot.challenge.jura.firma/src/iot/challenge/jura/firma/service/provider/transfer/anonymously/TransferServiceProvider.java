package iot.challenge.jura.firma.service.provider.transfer.anonymously;

import iot.challenge.jura.firma.service.provider.transfer.Dated;
import iot.challenge.jura.firma.service.provider.transfer.Message;
import jota.model.Transaction;
import jota.utils.TrytesConverter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eclipsesource.json.Json;

import iot.challenge.jura.firma.crypto.AES;
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

	////
	//
	// Parameters
	//
	//
	protected static MessageDigest messageDigest;
	protected Map<String, Integer> addressIndex;

	protected int index;
	protected String address;
	protected String key;

	////
	//
	// Functionality
	//
	//
	protected void activate(Map<String, Object> properties) {
		addressIndex = new HashMap<>();
		instantiateMessageDigest();
		super.activate(properties);
	}

	@Override
	protected void createOptions(Map<String, Object> properties) {
		Options newOptions = new Options(properties);
		resetIndexes((Options) options, newOptions);
		options = newOptions;

	}

	protected void resetIndexes(Options oldOptions, Options newOptions) {
		if (oldOptions != null) {
			if (!newOptions.getSalt().equals(oldOptions.getSalt()))
				addressIndex.clear();
		}
	}

	protected void instantiateMessageDigest() {
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			error("It was not possible to instantiate MessageDigest");
			messageDigest = null;
		}
	}

	@Override
	protected String buildMessage(Dated<Message> message) {
		String device = message.getElement().getDevice();
		try {
			prepareNextTransaction(device);
			return encrypt(super.buildMessage(message), key);
		} catch (Exception e) {
			return null;
		}

	}

	protected void prepareNextTransaction(String device) throws Exception {
		String salt = getSalt();
		String hash = getHash(salt, device, index);

		index = getIndex(device);
		address = TrytesConverter.toTrytes(hash).substring(0, 81);
		key = getKey(salt, hash);
	}

	protected String getSalt() {
		return sha256(((Options) options).getSalt());
	}

	protected static String sha256(String value) {
		byte[] bytes = messageDigest.digest(value.getBytes());
		return Base64.getEncoder().encodeToString(bytes);
	}

	protected static String getHash(String salt, String device, int index) {
		return sha256(salt + device + Integer.toString(index));
	}

	protected int getIndex(String d) throws Exception {
		addressIndex.put(d, addressIndex.containsKey(d) ? addressIndex.get(d) + 1 : countTransactions(d));
		return addressIndex.get(d);
	}

	protected int countTransactions(String device) throws Exception {
		int index = -1;
		boolean condition = false;
		String salt = getSalt();
		do {
			String hash = getHash(salt, device, ++index);
			String address = TrytesConverter.toTrytes(hash).substring(0, 81);
			String key = getKey(salt, hash);
			List<Transaction> transactions = iotaService.getTransactions(address);
			condition = transactions.stream()
					.filter(transaction -> {
						String encryptedMessage = iotaService.extractMessage(transaction);
						String message = decrypt(encryptedMessage, key);
						try {
							Json.parse(message.trim()).asObject();
							return true; // It is a JSON encrypted with this key
						} catch (Exception e) {
							return false;
						}
					})
					.findFirst()
					.isPresent();
		} while (condition);

		return index;
	}

	protected static String getKey(String salt, String hash) {
		return sha256(salt + hash).substring(12);
	}

	@Override
	protected String getAddress(String device) {
		return address;
	}

	protected String encrypt(String input, String key) {
		try {
			return AES.encrypt(input, key);
		} catch (Exception e) {
			error("It was not possible to encrypt message");
			return null;
		}
	}

	protected String decrypt(String input, String key) {
		try {
			return AES.decrypt(input, key);
		} catch (Exception e) {
			error("It was not possible to decrypt message");
			return null;
		}
	}
}
