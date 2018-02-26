package iot.challenge.jura.firma.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SignatureException;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.examples.ClearSignedFileProcessor;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.bouncycastle.util.Strings;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.command.CommandService;

import iot.challenge.jura.firma.service.provider.sign.Options;

/**
 * PGP functions
 */
public class PGP {

	protected static String PARAMETER_SIGN = "-s";
	protected static String PARAMETER_VERIFY = "-v";

	protected static String BUFFER = "jura/sign";
	protected static String BUFFER_SIGN = BUFFER + ".asc";
	protected static String BUFFER_KEY = "jura/key.asc";

	protected CommandService commandService;
	protected Options options;
	protected PGPKeys keys;

	private PGP() {
		super();
	}

	public PGP(CommandService commandService, Options options) {
		this();
		this.commandService = commandService;
		this.options = options;
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

	/**
	 * Verify a signed message
	 * 
	 * @param message
	 *            Signed message
	 * @param key
	 *            Key ID
	 * 
	 * @return True if the sign is valid, False otherwise
	 */
	public boolean verify(String message, String key) {
		try {
			Files.write(Paths.get(BUFFER_SIGN), message.getBytes());

			String inPath = Paths.get(BUFFER_SIGN).toFile().getAbsolutePath();
			String outPath = Paths.get(BUFFER).toFile().getAbsolutePath();
			InputStream keyIn = obtainKey(key);

			boolean result = verifyFile(new FileInputStream(inPath), keyIn, outPath);

			Files.delete(Paths.get(BUFFER));
			Files.delete(Paths.get(BUFFER_SIGN));
			Files.delete(Paths.get(BUFFER_KEY));

			return result;
		} catch (Exception e) {
			return false;
		}
	}

	private InputStream obtainKey(String id) throws FileNotFoundException, IOException {
		try {
			commandService.execute("gpg --recv-keys 0x" + id);
			String output = Paths.get(BUFFER_KEY).toFile().getAbsolutePath();
			commandService.execute("gpg --output " + output + " --armor --export " + id);
			return PGPUtil.getDecoderStream(new FileInputStream(BUFFER_KEY));

		} catch (KuraException e) {
			return null;
		}
	}

	private static boolean verifyFile(InputStream in, InputStream keyIn, String resultName)
			throws Exception {
		ArmoredInputStream aIn = new ArmoredInputStream(in);
		OutputStream out = new BufferedOutputStream(new FileOutputStream(resultName));

		//
		// write out signed section using the local line separator.
		// note: trailing white space needs to be removed from the end of
		// each line RFC 4880 Section 7.1
		//
		ByteArrayOutputStream lineOut = new ByteArrayOutputStream();
		int lookAhead = readInputLine(lineOut, aIn);
		byte[] lineSep = getLineSeparator();

		if (lookAhead != -1 && aIn.isClearText()) {
			byte[] line = lineOut.toByteArray();
			out.write(line, 0, getLengthWithoutSeparatorOrTrailingWhitespace(line));
			out.write(lineSep);

			while (lookAhead != -1 && aIn.isClearText()) {
				lookAhead = readInputLine(lineOut, lookAhead, aIn);

				line = lineOut.toByteArray();
				out.write(line, 0, getLengthWithoutSeparatorOrTrailingWhitespace(line));
				out.write(lineSep);
			}
		} else {
			// a single line file
			if (lookAhead != -1) {
				byte[] line = lineOut.toByteArray();
				out.write(line, 0, getLengthWithoutSeparatorOrTrailingWhitespace(line));
				out.write(lineSep);
			}
		}

		out.close();

		PGPPublicKeyRingCollection pgpRings = new PGPPublicKeyRingCollection(keyIn, new JcaKeyFingerprintCalculator());

		JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(aIn);
		PGPSignatureList p3 = (PGPSignatureList) pgpFact.nextObject();
		PGPSignature sig = p3.get(0);

		PGPPublicKey publicKey = pgpRings.getPublicKey(sig.getKeyID());
		sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), publicKey);

		//
		// read the input, making sure we ignore the last newline.
		//

		InputStream sigIn = new BufferedInputStream(new FileInputStream(resultName));

		lookAhead = readInputLine(lineOut, sigIn);

		processLine(sig, lineOut.toByteArray());

		if (lookAhead != -1) {
			do {
				lookAhead = readInputLine(lineOut, lookAhead, sigIn);

				sig.update((byte) '\r');
				sig.update((byte) '\n');

				processLine(sig, lineOut.toByteArray());
			} while (lookAhead != -1);
		}

		sigIn.close();

		return sig.verify();
	}

	private static int readInputLine(ByteArrayOutputStream bOut, InputStream fIn)
			throws IOException {
		bOut.reset();

		int lookAhead = -1;
		int ch;

		while ((ch = fIn.read()) >= 0) {
			bOut.write(ch);
			if (ch == '\r' || ch == '\n') {
				lookAhead = readPassedEOL(bOut, ch, fIn);
				break;
			}
		}

		return lookAhead;
	}

	private static int readInputLine(ByteArrayOutputStream bOut, int lookAhead, InputStream fIn)
			throws IOException {
		bOut.reset();

		int ch = lookAhead;

		do {
			bOut.write(ch);
			if (ch == '\r' || ch == '\n') {
				lookAhead = readPassedEOL(bOut, ch, fIn);
				break;
			}
		} while ((ch = fIn.read()) >= 0);

		if (ch < 0) {
			lookAhead = -1;
		}

		return lookAhead;
	}

	private static int readPassedEOL(ByteArrayOutputStream bOut, int lastCh, InputStream fIn)
			throws IOException {
		int lookAhead = fIn.read();

		if (lastCh == '\r' && lookAhead == '\n') {
			bOut.write(lookAhead);
			lookAhead = fIn.read();
		}

		return lookAhead;
	}

	private static byte[] getLineSeparator() {
		String nl = Strings.lineSeparator();
		byte[] nlBytes = new byte[nl.length()];

		for (int i = 0; i != nlBytes.length; i++) {
			nlBytes[i] = (byte) nl.charAt(i);
		}

		return nlBytes;
	}

	private static void processLine(PGPSignature sig, byte[] line)
			throws SignatureException, IOException {
		int length = getLengthWithoutWhiteSpace(line);
		if (length > 0) {
			sig.update(line, 0, length);
		}
	}

	private static int getLengthWithoutSeparatorOrTrailingWhitespace(byte[] line) {
		int end = line.length - 1;

		while (end >= 0 && isWhiteSpace(line[end])) {
			end--;
		}

		return end + 1;
	}

	private static boolean isLineEnding(byte b) {
		return b == '\r' || b == '\n';
	}

	private static int getLengthWithoutWhiteSpace(byte[] line) {
		int end = line.length - 1;

		while (end >= 0 && isWhiteSpace(line[end])) {
			end--;
		}

		return end + 1;
	}

	private static boolean isWhiteSpace(byte b) {
		return isLineEnding(b) || b == '\t' || b == ' ';
	}

}
