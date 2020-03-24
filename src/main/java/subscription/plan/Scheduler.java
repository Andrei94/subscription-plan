package subscription.plan;

import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;
import io.micronaut.context.annotation.Value;
import io.micronaut.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class Scheduler {
	private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);
	private ExecutorService syncExecutors = Executors.newFixedThreadPool(20);
	private ExecutorService shutdownExecutors = Executors.newFixedThreadPool(10);
	private DataSyncher syncher = new DataSyncher();
	@Inject
	public UserService userService;
	@Inject
	public AWSAdapter awsAdapter;
	@Value("${ec2-instance}")
	public String ec2InstanceId;

	//run every minute between 05:00 and 00:00 the next day
	@Scheduled(cron = "* 5-23,0 * * *")
	public void syncUserFolderFromEBSToS3() {
		if(syncExecutors.isShutdown())
			return;
		userService.forEachUser((user, mount) -> syncExecutors.execute(() -> syncher.syncEbsToS3ForUser(user)));
	}

	// run daily at 00:10
	@Scheduled(cron = "10 0 * * *")
	public void shutdownExecutors() {
		logger.info("Initiating shutdown");
		syncExecutors.shutdown();
		userService.forEachUser((user, mp) ->
				shutdownExecutors.execute(() -> {
					awsAdapter.sendCommand(createShellCommandRequest("umount -l " + mp.getDeviceName()));
					awsAdapter.deleteEBSVolume(userService.getVolumeId(mp.getVolumeId()));
				}));
		logger.info("Shutdown finished");
	}

	private SendCommandRequest createShellCommandRequest(String command) {
		return new SendCommandRequest()
				.withDocumentName("AWS-RunShellScript")
				.withDocumentVersion("1")
				.withParameters(new HashMap<String, List<String>>() {{
					put("commands", Collections.singletonList(command));
					put("executionTimeout", Collections.singletonList("40"));
				}})
				.withInstanceIds(ec2InstanceId);
	}

	// run daily at 04:00
	@Scheduled(cron = "0 4 * * *")
	public void startExecutors() {
		logger.info("Starting scheduler");
		syncExecutors = Executors.newScheduledThreadPool(20);
		logger.info("Scheduler started");
		userService.forEachUser((user, mount) -> syncExecutors.execute(() -> syncher.syncS3ToEBSForUser(user)));
	}
}
