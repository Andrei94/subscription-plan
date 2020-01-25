package subscription.plan;

public interface UserSpace {
	long getTotalSpace(String username);

	long getUsableSpace(String username);
}
