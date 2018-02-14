package iot.challenge.jura.firma.service.provider.sign;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.bouncycastle.openpgp.examples.ClearSignedFileProcessor;

/**
 * PGP functions
 */
public class PGP {

	protected static String PARAMETER_SIGN = "-s";
	protected static String PARAMETER_VERIFY = "-v";

	protected static String BUFFER = "/tmp/tmp-sign";
	protected static String BUFFER_SIGN = BUFFER + ".asc";

	protected PGPKeys keys;

	public PGP() {
		keys = PGPKeys.getInstance();
	}

	public PGPKeys getKeys() {
		return keys;
	}

	/**
	 * Sign a message
	 * 
	 * @param message
	 *            Message to sign
	 * @return Signed message
	 */
	public String sign(String message) {
		try {
			if (!message.endsWith("\n"))
				message += "\n";

			Files.write(Paths.get(BUFFER), message.getBytes());

			String path = Paths.get(PGPKeys.KEY_SECRET).toFile().getAbsolutePath();

			ClearSignedFileProcessor.main(new String[] {
					PARAMETER_SIGN, BUFFER, path, PGPKeys.PASS
			});

			String result = readFile(BUFFER_SIGN);

			Files.delete(Paths.get(BUFFER));
			Files.delete(Paths.get(BUFFER_SIGN));

			return result;
		} catch (Exception e) {
			return null;
		}
	}

	protected static String readFile(String file) throws IOException {
		return new String(
				Files.readAllBytes(Paths.get(file)),
				Charset.defaultCharset());
	}
}
