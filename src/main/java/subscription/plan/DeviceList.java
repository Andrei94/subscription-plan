package subscription.plan;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceList {
	private Map<String, Boolean> instanceDeviceList = new ConcurrentHashMap<String, Boolean>() {{
		put("/dev/sdb", true);
		put("/dev/sdc", true);
		put("/dev/sdd", true);
		put("/dev/sde", true);
		put("/dev/sdf", true);
		put("/dev/sdg", true);
		put("/dev/sdh", true);
		put("/dev/sdi", true);
		put("/dev/sdj", true);
		put("/dev/sdk", true);
		put("/dev/sdl", true);
		put("/dev/sdm", true);
		put("/dev/sdn", true);
		put("/dev/sdo", true);
		put("/dev/sdp", true);
		put("/dev/sdq", true);
		put("/dev/sdr", true);
		put("/dev/sds", true);
	}};

	public String getFreeDevice() {
		Optional<Map.Entry<String, Boolean>> freeDevice = instanceDeviceList.entrySet().stream().filter(Map.Entry::getValue).findFirst();
		if(freeDevice.isPresent())
			return freeDevice.get().getKey();
		return "";
	}

	public void markAsUsed(String device) {
		instanceDeviceList.put(device, false);
	}
}
