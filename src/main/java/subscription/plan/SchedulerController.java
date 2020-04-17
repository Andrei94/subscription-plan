package subscription.plan;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import javax.inject.Inject;

@Controller
public class SchedulerController {
	@Inject
	public Scheduler scheduler;

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
}
