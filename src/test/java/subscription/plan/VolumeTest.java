package subscription.plan;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class VolumeTest {
	@Inject
	@Client("/")
	HttpClient client;

	@Test
	void volumeExistsForUser() {
		String token = "2265daba0872fc3aef169d079365e590f0cbc8ed46c2a7984c8a642803cfd96cb47804a63cf22a79f6ca469268c29ee9e72a5059b62d0a598fe42dfc8dcc51bc";
		String volume = createVolume(new CreateVolumeForUserRequest("username", token));
		assertEquals("vol-07aa2a8cf7b8b15d7 " + token, volume);
	}

	@Test
	void createVolumeForNewUser() {
		String token = "2c2e43dcb393097a1221465812a4e9b1e1e80f16e92b313fd4ce8c5ee5b8272a17cd8cdc1ce63578494eaba739c6f7abba7890506ef6bf8d607538778f2a849";
		String volume = createVolume(new CreateVolumeForUserRequest("username2",
				token));
		assertEquals("vol-07aa2a8cf7b8b15d9 " + token, volume);
	}

	@Test
	void invalidTokenForUser() {
		String volume = createVolume(new CreateVolumeForUserRequest("username3", "token3"));
		assertEquals("invalid token", volume);
	}

	@Test
	void createUser() {
		String user = createUser("user");
		assertEquals("user", user);
	}

	private String createVolume(CreateVolumeForUserRequest request) {
		HttpRequest<CreateVolumeForUserRequest> put = HttpRequest.PUT("/volume/createVolume/", request);
		HttpResponse<String> exchange = client.toBlocking().exchange(put, String.class);
		assertEquals(200, exchange.getStatus().getCode());
		return exchange.body();
	}

	private String createUser(String username) {
		HttpRequest<String> put = HttpRequest.PUT("/volume/createUser/" + username, "");
		HttpResponse<String> exchange = client.toBlocking().exchange(put, String.class);
		assertEquals(200, exchange.getStatus().getCode());
		return Objects.requireNonNull(exchange.body());
	}

	@MockBean(UserMountsService.class)
	UserService userMountsService() {
		UserMountsService userMountsService = new UserMountsService();
		userMountsService.put("username", new MountPoint("/dev/sdo", "vol-07aa2a8cf7b8b15d7"));
		return userMountsService;
	}

	@MockBean(UserVolumeMountService.class)
	VolumeService userVolumeMountService() {
		TokenStore tokenStore = new TokenStore();
		tokenStore.putToken("username", "token");
		tokenStore.putToken("username2", "token2");
		UserVolumeMountService volumeService = new UserVolumeMountService(userMountsService(), new DeviceList()) {
			@Override
			public void initializeEC2Environment() {
				this.ec2InstanceId = "i-dmkj1892jdiu1";
				this.ec2AvailabilityZone = "eu-central-1a";
			}

			@Override
			public String createVolume() {
				return "vol-07aa2a8cf7b8b15d9";
			}

			@Override
			public void attachVolume(String user, String volumeId) {
			}

			@Override
			public String createUser(String user) {
				return user;
			}

			@Override
			public String getUserToken(String user) {
				return tokenStore.getToken(user);
			}

			@Override
			public void deleteVolume(String user, MountPoint mp) {
			}

			@Override
			public boolean tokenHashMatch(String user, String token) {
				return super.tokenHashMatch(user, token);
			}
		};
		volumeService.setTokenStore(tokenStore);
		return volumeService;
	}
}
