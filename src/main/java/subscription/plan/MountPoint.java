package subscription.plan;

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
