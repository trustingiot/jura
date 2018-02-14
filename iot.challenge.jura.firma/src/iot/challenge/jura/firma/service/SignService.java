package iot.challenge.jura.firma.service;

import com.eclipsesource.json.JsonObject;

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

	/**
	 * Extract signature from PGP signed message
	 * 
	 * @param message
	 *            Signed message
	 * @return Signature
	 */
	public String extractSignature(String message);

	/**
	 * Extract hash from PGP signed message
	 * 
	 * @param message
	 *            Signed message
	 * @return Hash
	 */
	public String extractHash(String message);

	/**
	 * Sign a JSON object -> sign({x}) => {body:{x},sign:{hash:., value:.}}
	 * 
	 * @param body
	 *            Object to be signed
	 * @return Signed JSON
	 */
	public JsonObject sign(JsonObject body);
}
