package subscription.plan.locking;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Put;

import javax.inject.Inject;

@Controller
public class FileLockingController {
	@Inject
	private FileLockingService lockingService;

	@Put(value = "/fileState/lock")
	void lockFile(@Body MarkFileStateRequest request) {
		lockingService.lock(request.getFile());
	}

	@Put(value = "/fileState/unlock", processes = "application/json")
	void unlockFile(@Body MarkFileStateRequest request) {
		lockingService.unlock(request.getFile());
	}
}
