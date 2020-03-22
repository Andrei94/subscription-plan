package subscription.plan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DeviceListTest {
	private final DeviceList deviceList = new DeviceList();

	@Test
	void markDeviceAsUsed() {
		deviceList.markAsUsed("/dev/sdp");
		deviceNotAvailable("/dev/sdp");
	}

	@Test
	void getNewFreeDevice() {
		deviceList.markAsUsed(deviceList.getFreeDevice());
		deviceNotAvailable("/dev/sdo");
		assertEquals("/dev/sdn", deviceList.getFreeDevice());
	}

	private void deviceNotAvailable(String device) {
		assertFalse(deviceList.getDeviceStatus(device));
	}

	@Test
	void getFreeDevice() {
		assertEquals("/dev/sdo", deviceList.getFreeDevice());
	}
}
