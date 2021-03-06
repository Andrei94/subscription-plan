package subscription.plan;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;

@Controller
public class HomeController {
	@Inject
	public UserSpace spaceForUser;
	@Inject
	public UserService userService;
	@Inject
	public VolumeService volumeService;
	@Inject
	public DeviceList deviceList;

	@Put(value = "/checkStorage", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
	SubscriptionConfirmation checkStorage(@Body SubscriptionPlan plan) {
		long userPlanSize = spaceForUser.getUsableSpace(plan.getUser());
		String backupFolder = "My Local PC";
		if(assertPlanSize(userPlanSize, plan.getUsedSize()))
			return new SubscriptionConfirmation()
					.withBucketName("backedup-storage-2")
					.withStorageClass("INTELLIGENT_TIERING")
					.withUserPath(plan.getUser() + "/" + backupFolder + "/")
					.withFreeSize(userPlanSize - plan.getUsedSize());
		return new SubscriptionConfirmation().withFreeSize(0);
	}

	boolean assertPlanSize(long maxSize, long usedSize) {
		return usedSize <= maxSize;
	}

	@Get(value = "/showSubscriptionSpace/{username}")
	StorageSubscription showSubscriptionSpace(@PathVariable("username") String user) {
		long totalSpace = spaceForUser.getTotalSpace(user);
		return new StorageSubscription(totalSpace - spaceForUser.getUsableSpace(user), totalSpace);
	}

	@Put(value = "/volume/createVolume/")
	String createVolume(@Body CreateVolumeForUserRequest request) {
		if(!volumeService.tokenHashMatch(request.getUsername(), request.getToken()))
			return "invalid token";
		if(userService.userExists(request.getUsername())) {
			return userService.getVolumeId(request.getUsername()) + " " + volumeService.getTokenHash(request.getUsername());
		}
		String volumeId = volumeService.createVolume();
		volumeService.attachVolume(request.getUsername(), volumeId);
		return volumeId + " " + volumeService.getTokenHash(request.getUsername());
	}

	@Get(value = "/freeDevicesCount")
	long getFreeDevicesCount() {
		return deviceList.getFreeDeviceCount();
	}

	@Put(value = "/volume/createUser/{username}")
	String createUser(@PathVariable("username") String user) {
		return volumeService.createUser(user);
	}

	@Post(value = "/volume/deleteUser/{username}")
	boolean deleteUser(@PathVariable("username") String username) {
		AtomicBoolean found = new AtomicBoolean(false);
		userService.forEachUser((user, mountPoint) -> {
			if(username.equals(user)) {
				volumeService.deleteVolume(user, mountPoint);
				userService.deleteUser(username);
				found.set(true);
			}
		});
		return found.get();
	}
}
