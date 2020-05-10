package subscription.plan.locking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class FileLockingService {
	private final ConcurrentHashMap<String, Boolean> lockedFiles = new ConcurrentHashMap<>();
	private final Logger logger = LoggerFactory.getLogger(FileLockingService.class);

	public void lock(String file) {
		logger.info("Locking file {}", file);
		lockedFiles.put(file, true);
	}

	public void unlock(String file) {
		logger.info("Unlocking file {}", file);
		lockedFiles.remove(file);
	}

	public List<String> getLockedFiles() {
		return new ArrayList<>(lockedFiles.keySet());
	}
}
