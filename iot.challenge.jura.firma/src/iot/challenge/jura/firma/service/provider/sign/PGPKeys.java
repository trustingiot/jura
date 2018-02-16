package iot.challenge.jura.firma.service.provider.sign;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Arrays;
import java.util.Date;

import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.command.CommandService;

import static java.text.MessageFormat.*;

/**
 * Exposes the PGP keys, creating them if they do not exists
 * 
 * Adapted from
 * https://github.com/bcgit/bc-java/tree/r1rv59/pg/src/main/java/org/bouncycastle/openpgp/examples
 */
public class PGPKeys {

	public static final String FOLDER = "jura/keys";

	protected static final String KEY_TEMPLATE = FOLDER + "/{0}.asc";

	public static final String KEY_SECRET = format(KEY_TEMPLATE, "secret");
	public static final String KEY_PUBLIC = format(KEY_TEMPLATE, "public");

	protected static final String ALGORITHM = "RSA";
	protected static final String PROVIDER = "BC";

	protected static final int TAG_ALGORITHM = PGPPublicKey.RSA_GENERAL;
	protected static final int TAG_HASH_ALGORITHM = HashAlgorithmTags.SHA1;
	protected static final int TAG_KEY_ALGORITHM = PGPEncryptedData.CAST5;

	protected static final int CERTIFICATION_LEVEL = PGPSignature.DEFAULT_CERTIFICATION;

	protected CommandService commandService;
	protected Options options;
	protected PGPSecretKey secretKey;
	protected PGPPublicKey publicKey;

	static {
		Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
		Security.addProvider(new BouncyCastleProvider());
	}

	protected PGPKeys() {
		super();
	}

	public static PGPKeys build(CommandService commandService, Options options) throws KuraException {
		PGPKeys result = new PGPKeys();
		result.commandService = commandService;
		result.options = options;
		result.initializeKeys();
		return result;
	}

	public PGPSecretKey getSecretKey() {
		return secretKey;
	}

	public PGPPublicKey getPublicKey() {
		return publicKey;
	}

	protected void initializeKeys() throws KuraException {
		boolean send = generateKeysIfNecessary(options);

		secretKey = readSecretKey(KEY_SECRET);
		publicKey = readPublicKey(KEY_PUBLIC);

		if (send)
			sendKeys();

	}

	protected static boolean generateKeysIfNecessary(Options options) {
		if (options.isGenerate() && !exists()) {
			generate(options.getIdentity(), options.getPass());
			return true;
		}
		return false;
	}

	protected static boolean exists() {
		return !Arrays.asList(new String[] { KEY_SECRET, KEY_PUBLIC }).stream()
				.map(File::new)
				.filter(f -> !f.exists())
				.findAny()
				.isPresent();
	}

	protected static void generate(String identity, String pass) {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
			kpg.initialize(1024);
			new File(FOLDER).mkdirs();
			exportKeyPair(
					new FileOutputStream(KEY_SECRET), // Secret out
					new FileOutputStream(KEY_PUBLIC), // Public out
					kpg.generateKeyPair(), // Key pair
					identity, // Identity
					pass); // Pass
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void exportKeyPair(OutputStream secretOut, OutputStream publicOut, KeyPair pair, String identity,
			String pass) throws Exception {
		PGPSecretKey secretKey = constructPGPSecretKey(pair, identity, pass.toCharArray());
		PGPPublicKey publicKey = secretKey.getPublicKey();

		secretKey.encode(secretOut = new ArmoredOutputStream(secretOut));
		publicKey.encode(publicOut = new ArmoredOutputStream(publicOut));

		secretOut.close();
		publicOut.close();
	}

	protected static PGPSecretKey constructPGPSecretKey(KeyPair pair, String identity, char[] pass) throws Exception {

		PGPKeyPair keyPair = new JcaPGPKeyPair(
				TAG_ALGORITHM, // algorithm
				pair, // pair
				new Date()); // date

		PGPDigestCalculator digestCalculator = new JcaPGPDigestCalculatorProviderBuilder()
				.build()
				.get(TAG_HASH_ALGORITHM);

		PGPContentSignerBuilder certificationSignerBuilder = new JcaPGPContentSignerBuilder(
				keyPair.getPublicKey().getAlgorithm(),
				TAG_HASH_ALGORITHM);

		PBESecretKeyEncryptor keyEncryptor = new JcePBESecretKeyEncryptorBuilder(TAG_KEY_ALGORITHM, digestCalculator)
				.setProvider(PROVIDER)
				.build(pass);

		return new PGPSecretKey(
				CERTIFICATION_LEVEL, // certification level
				keyPair, // keys
				identity, // id to bind the key
				digestCalculator, // checksum calculator
				null, // hashed packets to be added to the certification
				null, // unhashed packets to be added to the certification
				certificationSignerBuilder, // certification signer builder
				keyEncryptor); // Key encryptor
	}

	protected void sendKeys() throws KuraException {
		sendKeys(
				commandService,
				Paths.get(PGPKeys.KEY_PUBLIC).toFile().getAbsolutePath(),
				publicKey.getKeyID());
	}

	protected static void sendKeys(CommandService commandService, String key, long id) throws KuraException {
		commandService.execute("gpg --import --armor " + key);
		commandService.execute("dpkg -l | grep -qw dirmngr || aptitude install dirmngr");
		commandService.execute("gpg --send-keys " + Long.toHexString(id));
		commandService.execute("gpg --yes --batch --delete-keys " + Long.toHexString(id) + " -y");
	}

	protected static PGPSecretKey readSecretKey(String keyFile) {
		try {
			InputStream inputStream = new BufferedInputStream(new FileInputStream(keyFile));
			PGPSecretKey key = readSecretKey(inputStream);
			inputStream.close();
			return key;
		} catch (Exception e) {
			return null;
		}
	}

	protected static PGPSecretKey readSecretKey(InputStream inputStream) throws Exception {
		PGPSecretKeyRingCollection pgpKR = new PGPSecretKeyRingCollection(
				PGPUtil.getDecoderStream(inputStream),
				new JcaKeyFingerprintCalculator());

		Iterator<PGPSecretKeyRing> ikr = pgpKR.getKeyRings();
		while (ikr.hasNext()) {
			PGPSecretKeyRing kr = ikr.next();

			Iterator<PGPSecretKey> ik = kr.getSecretKeys();
			while (ik.hasNext()) {
				PGPSecretKey k = ik.next();

				if (k.isSigningKey())
					return k;
			}
		}

		return null;
	}

	protected static PGPPublicKey readPublicKey(String keyFile) {
		try {
			InputStream inputStream = new BufferedInputStream(new FileInputStream(keyFile));
			PGPPublicKey key = readPublicKey(inputStream);
			inputStream.close();
			return key;
		} catch (Exception e) {
			return null;
		}
	}

	protected static PGPPublicKey readPublicKey(InputStream inputStream) throws Exception {
		PGPPublicKeyRingCollection pgpKR = new PGPPublicKeyRingCollection(
				PGPUtil.getDecoderStream(inputStream),
				new JcaKeyFingerprintCalculator());

		Iterator<PGPPublicKeyRing> ikr = pgpKR.getKeyRings();
		while (ikr.hasNext()) {
			PGPPublicKeyRing kr = ikr.next();

			Iterator<PGPPublicKey> ik = kr.getPublicKeys();
			while (ik.hasNext()) {
				PGPPublicKey k = ik.next();

				if (k.isEncryptionKey())
					return k;
			}
		}

		return null;
	}

}
