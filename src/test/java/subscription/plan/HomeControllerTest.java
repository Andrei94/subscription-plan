package subscription.plan;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class HomeControllerTest {
	@Inject
	@Client("/")
	HttpClient client;

	@Test
	void checkStorage() {
		HttpRequest<SubscriptionPlan> put = HttpRequest.PUT("/checkStorage", new SubscriptionPlan(1, "username"));
		HttpResponse<SubscriptionConfirmation> exchange = client.toBlocking().exchange(put, SubscriptionConfirmation.class);
		assertEquals(200, exchange.getStatus().getCode());
		SubscriptionConfirmation subscriptionConfirmation = Objects.requireNonNull(exchange.body());
		assertEquals(1024L * 1024L * 1024L * 1024L - 1, subscriptionConfirmation.getFreeSize());
		assertEquals("backedup-storage-2", subscriptionConfirmation.getBucketName());
		assertEquals("INTELLIGENT_TIERING", subscriptionConfirmation.getStorageClass());
		assertEquals("username/", subscriptionConfirmation.getUserPath());
	}
}
