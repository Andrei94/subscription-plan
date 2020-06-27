package subscription.plan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class DataSyncher {
	private static final Logger logger = LoggerFactory.getLogger(DataSyncher.class);
	private final ConcurrentHashMap<String, Process> runningProcesses = new ConcurrentHashMap<>();

	public void syncEbsToS3ForUser(String username, List<String> lockedFiles) {
		logger.info("execution started on " + Thread.currentThread() + " for user " + username);
		List<String> commandWithArgs = new ArrayList<>(Arrays.asList("s3", "sync", "/sftpg/" + username + "/data", "s3://backedup-storage-2/" + username, "--delete", "--exclude", "*My Local PC/*", "--endpoint-url=https://s3.eu-central-1.amazonaws.com"));
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
		} else
			runningProcesses.remove(username);
		logger.info("execution finished on " + Thread.currentThread() + " for user " + username);
	}

	Process startAWSProcess(List<String> args) throws IOException {
		return new ProcessBuilder(new ArrayList<String>() {{
			add("aws");
			addAll(args);
		}}).inheritIO().start();
	}

	public void downloadUserData(String username) {
		logger.info("execution started on " + Thread.currentThread() + " for user " + username);
		if(!s3SyncRunningForUser(username)) {
			try {
				runningProcesses.put(username, startShellScript("/tmp/downloadUserData.sh", username));
			} catch(IOException e) {
				logger.error("An error occurred running aws sync s3", e);
			}
		} else
			runningProcesses.remove(username);
		logger.info("execution finished on " + Thread.currentThread() + " for user " + username);
	}

	public void syncS3BackupToEbs(String username) {
		logger.info("execution started on " + Thread.currentThread() + " for user " + username);
		if(!s3SyncRunningForUser(username)) {
			try {
				runningProcesses.put(username, startShellScript("/tmp/syncS3BackupToEBS.sh", username));
			} catch(IOException e) {
				logger.error("An error occurred running aws sync s3", e);
			}
		} else
			runningProcesses.remove(username);
		logger.info("execution finished on " + Thread.currentThread() + " for user " + username);
	}

	public void syncS3ToEBSForUser(String username) {
		logger.info("execution started on " + Thread.currentThread() + " for user " + username);
		if(!s3SyncRunningForUser(username)) {
			try {
				runningProcesses.put(username, startShellScript("/tmp/syncS3ToEBS.sh", username));
			} catch(IOException e) {
				logger.error("An error occurred running aws sync s3", e);
			}
		} else
			runningProcesses.remove(username);
		logger.info("execution finished on " + Thread.currentThread() + " for user " + username);
	}

	boolean s3SyncRunningForUser(String user) {
		return runningProcesses.containsKey(user) && runningProcesses.get(user).isAlive();
	}

	Process startShellScript(String script, String username) throws IOException {
		return new ProcessBuilder(script, username).inheritIO().start();
	}
}
