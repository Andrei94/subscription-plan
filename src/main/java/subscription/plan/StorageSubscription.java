package subscription.plan;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class StorageSubscription {
	private long usedSpace;
	private long totalSpace;

	public StorageSubscription() {
	}

	public StorageSubscription(long usedSpace, long totalSpace) {
		this.usedSpace = usedSpace;
		this.totalSpace = totalSpace;
	}

	public long getUsedSpace() {
		return usedSpace;
	}

	public void setUsedSpace(long usedSpace) {
		this.usedSpace = usedSpace;
	}

	public long getTotalSpace() {
		return totalSpace;
	}

	public void setTotalSpace(long totalSpace) {
		this.totalSpace = totalSpace;
	}
}
