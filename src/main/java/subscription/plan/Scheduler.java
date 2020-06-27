package subscription.plan;

import io.micronaut.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import subscription.plan.locking.FileLockingService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class Scheduler {
	private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);
	private ExecutorService syncExecutors = Executors.newFixedThreadPool(20);
	private ExecutorService shutdownExecutors = Executors.newFixedThreadPool(10);
	@Inject
	private DataSyncher syncher;
	@Inject
	public UserService userService;
	@Inject
	public VolumeService volumeService;
	@Inject
	public AWSAdapter awsAdapter;
	@Inject
	private FileLockingService fileLockingService;

	//run every minute between 05:00 and 00:00 the next day
	@Scheduled(cron = "* 5-23,0 * * *")
	public void syncUserFolderFromEBSToS3() {
		if(syncExecutors.isShutdown())
			return;
		userService.forEachUser((user, mount) -> syncExecutors.execute(() -> {
			syncher.syncEbsToS3ForUser(user, fileLockingService.getLockedFiles());
			syncher.syncS3BackupToEbs(user);
		}));
	}

	// run daily at 00:10
	@Scheduled(cron = "10 0 * * *")
	public void shutdownExecutors() {
		logger.info("Initiating shutdown");
		syncExecutors.shutdown();
		userService.forEachUser((user, mp) ->
				shutdownExecutors.execute(() -> volumeService.deleteVolume(user, mp)));
		logger.info("Shutdown finished");
	}

	// run daily at 04:00
	@Scheduled(cron = "0 4 * * *")
	public void startExecutors() {
		logger.info("Starting scheduler");
		syncExecutors = Executors.newScheduledThreadPool(20);
		logger.info("Scheduler started");
		userService.forEachUser((user, mount) -> syncExecutors.execute(() -> {
			String volumeId = volumeService.createVolume();
			volumeService.attachVolume(user, volumeId);
			syncher.syncS3ToEBSForUser(user);
		}));
	}
}
