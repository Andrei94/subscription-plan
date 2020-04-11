package subscription.plan;

public interface VolumeService {
	String createVolume();

	void attachVolume(String user, String volumeId);

	String createUser(String user);

	String getUserToken(String user);

	void deleteVolume(String user, MountPoint mp);
}
