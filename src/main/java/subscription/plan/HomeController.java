package subscription.plan;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import javax.inject.Inject;

@Controller
public class HomeController {
	@Inject
	public UserSpace spaceForUser;
	@Inject
	public UserService userService;
	@Inject
	public VolumeService volumeService;
	@Inject
	public Scheduler scheduler;
	@Inject
	public DeviceList deviceList;

	@Put(value = "/checkStorage", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
	SubscriptionConfirmation checkStorage(@Body SubscriptionPlan plan) {
		long userPlanSize = spaceForUser.getUsableSpace(plan.getUser());
		if(assertPlanSize(userPlanSize, plan.getUsedSize()))
			return new SubscriptionConfirmation()
					.withBucketName("backedup-storage-2")
					.withStorageClass("INTELLIGENT_TIERING")
					.withUserPath(plan.getUser() + "/")
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

	@Put(value = "/volume/{username}")
	String createVolume(@PathVariable("username") String user) {
		if(userService.userExists(user)) {
			return userService.getVolumeId(user) + " " + volumeService.getUserToken(user);
		}
		String volumeId = volumeService.createVolume();
		volumeService.attachVolume(user, volumeId);
		return volumeId + " " + volumeService.getUserToken(user);
	}

	@Get(value = "/scheduler/toS3")
	void syncToS3() {
		scheduler.syncUserFolderFromEBSToS3();
	}

	@Get(value = "/scheduler/shutdown")
	void shutdown() {
		scheduler.shutdownExecutors();
	}

	@Get(value = "/scheduler/restart")
	void restart() {
		scheduler.startExecutors();
	}

	@Get(value = "/freeDevicesCount")
	long getFreeDevicesCount() {
		return deviceList.getFreeDeviceCount();
	}
}
