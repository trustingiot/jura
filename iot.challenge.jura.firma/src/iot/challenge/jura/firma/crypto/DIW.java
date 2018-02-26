package iot.challenge.jura.firma.crypto;

/**
 * Device Identification Word
 */
public class DIW {

	public static final int LENGTH = 8;

	protected String device;
	protected String salt;
	protected String word;

	private DIW() {
		super();
	}

	public DIW(String salt, String device) {
		this();
		this.salt = SHA256.digest(salt);
		this.device = device;
		this.word = word();
	}

	protected String word() {
		return SHA256.digest(salt + SHA256.digest(salt + device)).substring(0, LENGTH);
	}

	public String getWord() {
		return word;
	}

	public boolean check(String device, String word) {
		return this.device.equals(device) && this.word.equals(word);
	}

}
