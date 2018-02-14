package iot.challenge.jura.firma.service;

/**
 * Service to sign messages using PGP
 */
public interface SignService {

	/**
	 * Sign 'message' using PGP
	 * 
	 * @param message
	 *            Message to be signed
	 * @return Signed message
	 */
	public String sign(String message);
}
