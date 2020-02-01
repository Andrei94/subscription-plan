package subscription.plan;

import io.micronaut.context.annotation.Primary;
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

	@Primary
	@MockBean(UserSpace.class)
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
}
