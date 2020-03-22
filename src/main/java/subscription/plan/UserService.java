package subscription.plan;

public interface UserService {
	void put(String user, MountPoint mount);

	boolean userExists(String user);

	String getVolumeId(String user);
}
