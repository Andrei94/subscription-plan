package subscription.plan;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

@Controller
public class HomeController {
	@Put(value = "/checkStorage", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
	SubscriptionConfirmation checkStorage(@Body SubscriptionPlan plan) {
		long userPlanSize = getUserPlanSize(plan.getUser());
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

	long getUserPlanSize(String user) {
		final long quarterTB = 256L * 1024L * 1024L * 1024L;
		final long halfTB = 512L * 1024L * 1024L * 1024L;
		final long oneTB = 1024L * 1024L * 1024L * 1024L;

		switch(user) {
			case "username":
				return oneTB;
			case "username2":
				return halfTB;
			case "username3":
				return quarterTB;
		}
		return 0;
	}
}
