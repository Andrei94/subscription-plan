package subscription.plan;

import io.micronaut.context.annotation.Replaces;
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
public class StorageSubscriptionTest {
	@Inject
	@Client("/")
	HttpClient client;

	@Test
	void getStorageSubscriptionForUserUsername() {
		StorageSubscription subscription = getStorageSubscription("username");
		assertEquals((1024L - 256L) * 1024L * 1024L * 1024L, subscription.getUsedSpace());
		assertEquals(1024L * 1024L * 1024L * 1024L, subscription.getTotalSpace());
	}

	@Test
	void getStorageSubscriptionForUserUsername2() {
		StorageSubscription subscription = getStorageSubscription("username2");
		assertEquals((512L - 256L) * 1024L * 1024L * 1024L, subscription.getUsedSpace());
		assertEquals(512L * 1024L * 1024L * 1024L, subscription.getTotalSpace());
	}

	@Test
	void getStorageSubscriptionForUserUsername3() {
		StorageSubscription subscription = getStorageSubscription("username3");
		assertEquals(0, subscription.getUsedSpace());
		assertEquals(256L * 1024L * 1024L * 1024L, subscription.getTotalSpace());
	}

	@Test
	void getStorageSubscriptionForUnknownUser() {
		StorageSubscription subscription = getStorageSubscription("username4");
		assertEquals(0, subscription.getUsedSpace());
		assertEquals(0, subscription.getTotalSpace());
	}

	private StorageSubscription getStorageSubscription(String user) {
		HttpRequest<String> put = HttpRequest.GET("/showSubscriptionSpace/" + user);
		HttpResponse<StorageSubscription> exchange = client.toBlocking().exchange(put, StorageSubscription.class);
		assertEquals(200, exchange.getStatus().getCode());
		return Objects.requireNonNull(exchange.body());
	}

	@MockBean(UserSpace.class)
	@Replaces(FileUserSpace.class)
	UserSpace userSpace() {
		return new FileUserSpace() {
			@Override
			public long getTotalSpace(String username) {
				final long quarterTB = 256L * 1024L * 1024L * 1024L;
				final long halfTB = 512L * 1024L * 1024L * 1024L;
				final long oneTB = 1024L * 1024L * 1024L * 1024L;

				switch(username) {
					case "username":
						return oneTB;
					case "username2":
						return halfTB;
					case "username3":
						return quarterTB;
				}
				return 0;
			}

			@Override
			public long getUsableSpace(String username) {
				final long quarterTB = 256L * 1024L * 1024L * 1024L;
				switch(username) {
					case "username":
					case "username2":
					case "username3":
						return quarterTB;
				}
				return 0;
			}
		};
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
