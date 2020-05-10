package subscription.plan.locking;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class MarkFileStateRequest {
	private String file;

	public MarkFileStateRequest() {
	}

	public MarkFileStateRequest(String file) {
		this.file = file;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
}
