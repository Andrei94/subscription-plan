package subscription.plan;

import java.util.function.BiConsumer;

public interface UserService {
	void put(String user, MountPoint mount);

	boolean userExists(String user);

	String getVolumeId(String user);

	void forEachUser(BiConsumer<String, MountPoint> action);
}
