package subscription.plan;

import javax.inject.Singleton;
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
}
