package iot.challenge.jura.firma.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Advanced Encryption Standard
 */
public class AES {

	protected static final String CIPHER = "AES";

	protected static Cipher cipher;

	static {
		try {
			cipher = Cipher.getInstance(CIPHER);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
			cipher = null;
		}
	}

	public static String encrypt(String text, String key) throws Exception {
		byte[] input = text.getBytes();
		byte[] k = key.getBytes();
		byte[] output = execute(Cipher.ENCRYPT_MODE, input, k);
		return Base64.getEncoder().encodeToString(output);
	}

	public static String decrypt(String text, String key) throws Exception {
		byte[] input = Base64.getDecoder().decode(text);
		byte[] k = key.getBytes();
		byte[] output = execute(Cipher.DECRYPT_MODE, input, k);
		return new String(output);
	}

	private static byte[] execute(int mode, byte[] input, byte[] key) throws Exception {
		initCipher(mode, key);
		return cipher.doFinal(input);
	}

	private static void initCipher(int mode, byte[] key) throws InvalidKeyException {
		cipher.init(mode, new SecretKeySpec(key, CIPHER));
	}
}
