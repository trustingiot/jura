package iot.challenge.jura.firma.crypto;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import jota.utils.TrytesConverter;

/**
 * Encrypted transaction
 */
public class EncryptedTransaction {
	protected String device;
	protected String salt;

	protected int index;
	protected String hash;
	protected String address;
	protected String key;

	private EncryptedTransaction() {
		super();
	}

	public EncryptedTransaction(String salt, String device) {
		this();
		this.index = -1;
		this.device = device;
		this.salt = SHA256.digest(salt);
	}

	public EncryptedTransaction(String salt, String device, int index) {
		this(salt, device);
		setIndex(index);
	}

	public int getIndex() {
		return index;
	}

	public String getAddress() {
		return address;
	}

	public String getKey() {
		return key;
	}

	public void setIndex(int index) {
		this.index = index - 1;
		next();
	}

	public void next() {
		index++;
		hash = hash();
		address = address();
		key = key();
	}

	protected String hash() {
		return SHA256.digest(salt + device + Integer.toString(index));
	}

	protected String address() {
		return TrytesConverter.toTrytes(hash).substring(0, 81);
	}

	protected String key() {
		return SHA256.digest(salt + hash).substring(12);
	}

	public <T> EncryptedTransaction end(
			Function<EncryptedTransaction, List<T>> generator,
			Predicate<T> filter) throws Exception {
		do {
			next();
		} while (generator.apply(this).stream().filter(filter).findFirst().isPresent());
		return this;
	}

	public String encrypt(String input) throws Exception {
		return AES.encrypt(input, key);
	}

	public String decrypt(String input) throws Exception {
		return AES.decrypt(input, key);
	}

}
