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
		String volume = createVolume("username");
		assertEquals("vol-07aa2a8cf7b8b15d7", volume);
	}

	@Test
	void createVolumeForNewUser() {
		String volume = createVolume("username2");
		assertEquals("vol-07aa2a8cf7b8b15d9", volume);
	}

	private String createVolume(String username) {
		HttpRequest<String> put = HttpRequest.PUT("/volume/" + username, "");
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
		return new VolumeService() {
			@Override
			public String createVolume() {
				return "vol-07aa2a8cf7b8b15d9";
			}

			@Override
			public void attachVolume(String user, String volumeId) {
			}
		};
	}
}
