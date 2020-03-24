package subscription.plan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataSyncher {
	private static final Logger logger = LoggerFactory.getLogger(DataSyncher.class);
	private Map<String, Process> runningProcesses = new ConcurrentHashMap<>();

	public void syncEbsToS3ForUser(String username) {
		logger.info("execution started on " + Thread.currentThread() + " for user " + username);
		if(!s3SyncRunningForUser(username)) {
			try {
				runningProcesses.put(username, startAWSProcess("s3", "sync", "/mnt/" + username, "s3://backedup-storage-2/" + username, "--delete"));
			} catch(IOException e) {
				logger.error("An error occurred running aws sync s3", e);
			}
		}
		logger.info("execution finished on " + Thread.currentThread() + " for user " + username);
	}

	boolean s3SyncRunningForUser(String user) {
		return runningProcesses.containsKey(user) && runningProcesses.get(user).isAlive();
	}

	public void syncS3ToEBSForUser(String username) {
		logger.info("execution started on " + Thread.currentThread() + " for user " + username);
		try {
			startAWSProcess("s3", "sync", "s3://backedup-storage-2/" + username, "/mnt/" + username, "--delete");
		} catch(IOException e) {
			logger.error("An error occurred running aws sync s3", e);
		}
		logger.info("execution finished on " + Thread.currentThread() + " for user " + username);
	}

	Process startAWSProcess(String... args) throws IOException {
		return new ProcessBuilder(new ArrayList<String>() {{
			add("aws");
			addAll(Arrays.asList(args));
		}}).inheritIO().start();
	}
}