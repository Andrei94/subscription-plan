package subscription.plan;

import io.micronaut.runtime.context.scope.Refreshable;

import java.io.File;

@Refreshable
public class FileUserSpace implements UserSpace {
	@Override
	public long getTotalSpace(String username) {
		try {
			return new File(getPathname(username)).getTotalSpace();
		} catch(SecurityException ex) {
			return 0;
		}
	}

	String getPathname(String username) {
		return "/data/" + username + "/";
	}
}
