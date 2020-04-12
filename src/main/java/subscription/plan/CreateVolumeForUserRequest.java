package subscription.plan;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class CreateVolumeForUserRequest {
	private String username;
	private String token;

	public CreateVolumeForUserRequest() {
	}

	public CreateVolumeForUserRequest(String username, String token) {
		this.username = username;
		this.token = token;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
