package subscription.plan;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

@Controller
public class HomeController {
	@Put(value = "/checkStorage", consumes = MediaType.APPLICATION_JSON)
	SubscriptionConfirmation checkStorage(@Body SubscriptionPlan plan) {
		long oneTB = 1024L * 1024L * 1024L * 1024L;
		if(assertPlanSize(oneTB, plan.getUsedSize()))
			return new SubscriptionConfirmation()
					.withBucketName("backedup-storage-2")
					.withStorageClass("INTELLIGENT_TIERING")
					.withUserPath(plan.getUser() + "/")
					.withFreeSize(oneTB - plan.getUsedSize());
		return new SubscriptionConfirmation().withFreeSize(0);
	}

	boolean assertPlanSize(long maxSize, long usedSize) {
		return usedSize <= maxSize;
	}
}
