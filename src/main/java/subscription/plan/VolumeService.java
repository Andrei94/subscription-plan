package subscription.plan;

public interface VolumeService {
	String createVolume();

	void attachVolume(String user, String volumeId);

	void deleteVolume(String user, MountPoint mp);
}
