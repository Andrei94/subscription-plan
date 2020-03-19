package subscription.plan;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {
	private Map<String, MountPoint> userMountPoints = new ConcurrentHashMap<>();

	public void put(String user, MountPoint mount) {
		userMountPoints.put(user, mount);
	}

	public boolean userExists(String user) {
		return userMountPoints.containsKey(user);
	}

	public String getVolumeId(String user) {
		return userMountPoints.get(user).getVolumeId();
	}
}

class MountPoint {
	private String deviceName;
	private String volumeId;

	public MountPoint(String device, String volumeId) {
		this.deviceName = device;
		this.volumeId = volumeId;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public String getVolumeId() {
		return volumeId;
	}
}