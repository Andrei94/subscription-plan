package subscription.plan;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import javax.inject.Inject;

@Controller
public class HomeController {
	private UserSpace spaceForUser = new FileUserSpace();

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

	@Inject
	public void setSpaceForUser(UserSpace spaceForUser) {
		this.spaceForUser = spaceForUser;
	}

	@Get(value = "/showSubscriptionSpace/{username}")
	StorageSubscription showSubscriptionSpace(@PathVariable("username") String user) {
		long totalSpace = spaceForUser.getTotalSpace(user);
		return new StorageSubscription(totalSpace - spaceForUser.getUsableSpace(user), totalSpace);
	}
}
