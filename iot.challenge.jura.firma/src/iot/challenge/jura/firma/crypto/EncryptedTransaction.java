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

	public void back() {
		setIndex(index - 1);
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + ((device == null) ? 0 : device.hashCode());
		result = prime * result + ((hash == null) ? 0 : hash.hashCode());
		result = prime * result + index;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((salt == null) ? 0 : salt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EncryptedTransaction other = (EncryptedTransaction) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (device == null) {
			if (other.device != null)
				return false;
		} else if (!device.equals(other.device))
			return false;
		if (hash == null) {
			if (other.hash != null)
				return false;
		} else if (!hash.equals(other.hash))
			return false;
		if (index != other.index)
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (salt == null) {
			if (other.salt != null)
				return false;
		} else if (!salt.equals(other.salt))
			return false;
		return true;
	}

}
