package subscription.plan;

import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Singleton
public class UserMountsService implements UserService {
	private Map<String, MountPoint> userMountPoints = new ConcurrentHashMap<>();

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
}