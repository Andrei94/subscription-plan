package subscription.plan;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class SubscriptionPlan {
	private long usedSize;
	private String user;

	public SubscriptionPlan() {
	}

	public SubscriptionPlan(long usedSize, String user) {
		this.usedSize = usedSize;
		this.user = user;
	}

	public long getUsedSize() {
		return usedSize;
	}

	public void setUsedSize(long usedSize) {
		this.usedSize = usedSize;
	}

	public String getUser() {
		return user;
	}
}
