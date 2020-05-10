package subscription.plan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataSyncher {
	private static final Logger logger = LoggerFactory.getLogger(DataSyncher.class);
	private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();

	public void syncEbsToS3ForUser(String username, List<String> lockedFiles) {
		logger.info("execution started on " + Thread.currentThread() + " for user " + username);
		List<String> commandWithArgs = new ArrayList<>(Arrays.asList("s3", "sync", "/sftpg/" + username + "/data", "s3://backedup-storage-2/" + username, "--delete", "--exclude", "*My Local PC/*"));
		lockedFiles.forEach(file -> {
			commandWithArgs.add("--exclude");
			commandWithArgs.add(file);
		});
		if(!s3SyncRunningForUser(username)) {
			try {
				runningProcesses.put(username, startAWSProcess(commandWithArgs));
			} catch(IOException e) {
				logger.error("An error occurred running aws sync s3", e);
			}
		}
		logger.info("execution finished on " + Thread.currentThread() + " for user " + username);
	}

	boolean s3SyncRunningForUser(String user) {
		return runningProcesses.containsKey(user) && runningProcesses.get(user).isAlive();
	}

	public void syncS3BackupToEbs(String username) {
		logger.info("execution started on " + Thread.currentThread() + " for user " + username);
		try {
			startAWSProcess(Arrays.asList("s3", "sync", "s3://backedup-storage-2/" + username + "/My Local PC/", "/sftpg/" + username + "/data/My Local PC/", "--delete"));
		} catch(IOException e) {
			logger.error("An error occurred running aws sync s3", e);
		}
		logger.info("execution finished on " + Thread.currentThread() + " for user " + username);
	}

	public void syncS3ToEBSForUser(String username) {
		logger.info("execution started on " + Thread.currentThread() + " for user " + username);
		try {
			startAWSProcess(Arrays.asList("s3", "sync", "s3://backedup-storage-2/" + username, "/sftpg/" + username + "/data", "--delete"));
		} catch(IOException e) {
			logger.error("An error occurred running aws sync s3", e);
		}
		logger.info("execution finished on " + Thread.currentThread() + " for user " + username);
	}

	Process startAWSProcess(List<String> args) throws IOException {
		return new ProcessBuilder(new ArrayList<String>() {{
			add("aws");
			addAll(args);
		}}).inheritIO().start();
	}
}
