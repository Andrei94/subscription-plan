package subscription.plan;

import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;
import io.micronaut.context.annotation.Value;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class UserMountsService implements UserService {
	private Map<String, MountPoint> userMountPoints = new ConcurrentHashMap<>();
	@Inject
	public AWSAdapter awsAdapter;
	@Inject
	public TokenStore tokenStore;
	@Value("${ec2-instance}")
	public String ec2InstanceId;

	@Override
	public void put(String user, MountPoint mount) {
		userMountPoints.put(user, mount);
	}

	@Override
	public boolean userExists(String user) {
		return userMountPoints.containsKey(user);
	}

	@Override
	public String getVolumeId(String user) {
		return userMountPoints.get(user).getVolumeId();
	}

	@Override
	public void forEachUser(BiConsumer<String, MountPoint> action) {
		userMountPoints.forEach(action);
	}

	@Override
	public void deleteUser(String user) {
		awsAdapter.sendCommandAsync(createShellCommandRequest((Stream.of(
				"userdel -r {user}",
				"rm -rf /sftpg/{user}",
				"rm -rf /home/{user}"
		).map(value -> value.replace("{user}", user)).collect(Collectors.toList()))));
		tokenStore.delete(user);
		userMountPoints.remove(user);
	}

	private SendCommandRequest createShellCommandRequest(List<String> commands) {
		return new SendCommandRequest()
				.withDocumentName("AWS-RunShellScript")
				.withDocumentVersion("1")
				.withParameters(new HashMap<String, List<String>>() {{
					put("commands", commands);
					put("executionTimeout", Collections.singletonList("40"));
				}})
				.withInstanceIds(ec2InstanceId);
	}
}