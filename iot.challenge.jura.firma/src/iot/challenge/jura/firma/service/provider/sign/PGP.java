package iot.challenge.jura.firma.service.provider.sign;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.bouncycastle.openpgp.examples.ClearSignedFileProcessor;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.command.CommandService;

/**
 * PGP functions
 */
public class PGP {

	protected static String PARAMETER_SIGN = "-s";
	protected static String PARAMETER_VERIFY = "-v";

	protected static String BUFFER = "jura/sign";
	protected static String BUFFER_SIGN = BUFFER + ".asc";

	protected Options options;
	protected PGPKeys keys;

	private PGP() {
		super();
	}

	private PGP(Options options) {
		this();
		this.options = options;
	}

	public PGP(CommandService commandService, Options options) {
		this(options);
		try {
			keys = PGPKeys.build(commandService, options);
		} catch (KuraException e) {
			keys = null;
		}
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
		return sign(message, true);
	}

	private String sign(String message, boolean retry) {
		try {
			if (!message.endsWith("\n"))
				message += "\n";

			Files.write(Paths.get(BUFFER), message.getBytes());

			ClearSignedFileProcessor.main(new String[] {
					PARAMETER_SIGN,
					Paths.get(BUFFER).toFile().getAbsolutePath(),
					Paths.get(PGPKeys.KEY_SECRET).toFile().getAbsolutePath(),
					options.getPass()
			});

			String result = readFile(BUFFER_SIGN);

			Files.delete(Paths.get(BUFFER));
			Files.delete(Paths.get(BUFFER_SIGN));

			return result;
		} catch (Exception e) {
			// The first call to ClearSignedFileProcessor fails sometimes when the service
			// is installed (this code is invoked exceptionally...)
			return retry ? sign(message, false) : null;
		}
	}

	protected static String readFile(String file) throws IOException {
		return new String(
				Files.readAllBytes(Paths.get(file)),
				Charset.defaultCharset());
	}
}
