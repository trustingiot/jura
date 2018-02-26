package iot.challenge.jura.firma.crypto;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Base64.Encoder;

/**
 * Secure Hash Algorithm
 */
public class SHA256 {

	protected static final String DIGEST = "SHA-256";

	protected static MessageDigest digest;
	protected static Encoder encoder;

	static {
		try {
			digest = MessageDigest.getInstance(DIGEST);
			encoder = Base64.getEncoder();
		} catch (Exception e) {
			digest = null;
			encoder = null;
		}
	}

	public static String digest(String value) {
		return digest(value.getBytes());
	}

	public static String digest(byte[] value) {
		byte[] bytes = digest.digest(value);
		return encoder.encodeToString(bytes);
	}
}
