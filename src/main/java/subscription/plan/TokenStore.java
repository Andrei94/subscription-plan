package subscription.plan;

import javax.inject.Singleton;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class TokenStore {
	private ConcurrentHashMap<String, String> tokenStore = new ConcurrentHashMap<>();

	public void putToken(String user, String token) {
		tokenStore.put(user, token);
	}

	public String getToken(String user) {
		return tokenStore.get(user);
	}

	public boolean hasToken(String user) {
		return tokenStore.containsKey(user);
	}

	public String getHashedToken(String user) {
		if(hasToken(user))
			return sha512(tokenStore.get(user));
		return "";
	}

	private String sha512(String text) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-512");
			byte[] messageDigest = digest.digest(text.getBytes());
			BigInteger no = new BigInteger(1, messageDigest);
			StringBuilder hashText = new StringBuilder(no.toString(16));
			while (hashText.length() < 32) {
				hashText.insert(0, "0");
			}
			return hashText.toString();
		} catch(NoSuchAlgorithmException ignored) {
		}
		return "";
	}

	public void delete(String user) {
		tokenStore.put(user, "");
		tokenStore.remove(user);
	}
}
