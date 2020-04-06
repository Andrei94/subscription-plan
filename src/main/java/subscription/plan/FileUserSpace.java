package subscription.plan;

import io.micronaut.runtime.context.scope.Refreshable;

import java.io.File;

@Refreshable
public class FileUserSpace implements UserSpace {
	@Override
	public long getTotalSpace(String username) {
		try {
			return new File(getUserPathname(username)).getTotalSpace();
		} catch(SecurityException ex) {
			return 0;
		}
	}

	@Override
	public long getUsableSpace(String username) {
		try {
			return new File(getUserPathname(username)).getUsableSpace();
		} catch(SecurityException ex) {
			return 0;
		}
	}

	String getUserPathname(String username) {
		return "/sftpg/" + username + "/data";
	}
}
