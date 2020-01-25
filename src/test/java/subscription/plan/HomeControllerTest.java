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
import static org.junit.jupiter.api.Assertions.assertNull;

@MicronautTest
public class HomeControllerTest {
	@Inject
	@Client("/")
	HttpClient client;

	@Test
	void checkStorage() {
		SubscriptionConfirmation subscriptionConfirmation = getSubscriptionConfirmation(new SubscriptionPlan(1, "username"));
		assertEquals(1024L * 1024L * 1024L * 1024L - 1, subscriptionConfirmation.getFreeSize());
		assertEquals("backedup-storage-2", subscriptionConfirmation.getBucketName());
		assertEquals("INTELLIGENT_TIERING", subscriptionConfirmation.getStorageClass());
		assertEquals("username/", subscriptionConfirmation.getUserPath());
	}

	@Test
	void checkStorageForUserUsername2() {
		SubscriptionConfirmation subscriptionConfirmation = getSubscriptionConfirmation(new SubscriptionPlan(1, "username2"));
		assertEquals(512L * 1024L * 1024L * 1024L - 1, subscriptionConfirmation.getFreeSize());
		assertEquals("backedup-storage-2", subscriptionConfirmation.getBucketName());
		assertEquals("INTELLIGENT_TIERING", subscriptionConfirmation.getStorageClass());
		assertEquals("username2/", subscriptionConfirmation.getUserPath());
	}

	@Test
	void checkStorageForUserUsername3() {
		SubscriptionConfirmation subscriptionConfirmation = getSubscriptionConfirmation(new SubscriptionPlan(1, "username3"));
		assertEquals(256L * 1024L * 1024L * 1024L - 1, subscriptionConfirmation.getFreeSize());
		assertEquals("backedup-storage-2", subscriptionConfirmation.getBucketName());
		assertEquals("INTELLIGENT_TIERING", subscriptionConfirmation.getStorageClass());
		assertEquals("username3/", subscriptionConfirmation.getUserPath());
	}

	@Test
	void checkStorageForUnknownUser() {
		SubscriptionConfirmation subscriptionConfirmation = getSubscriptionConfirmation(new SubscriptionPlan(1, "user"));
		assertEquals(0, subscriptionConfirmation.getFreeSize());
		assertNull(subscriptionConfirmation.getBucketName());
		assertNull(subscriptionConfirmation.getStorageClass());
		assertNull(subscriptionConfirmation.getUserPath());
	}

	private SubscriptionConfirmation getSubscriptionConfirmation(SubscriptionPlan user) {
		HttpRequest<SubscriptionPlan> put = HttpRequest.PUT("/checkStorage", user);
		HttpResponse<SubscriptionConfirmation> exchange = client.toBlocking().exchange(put, SubscriptionConfirmation.class);
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
		};
	}
}
