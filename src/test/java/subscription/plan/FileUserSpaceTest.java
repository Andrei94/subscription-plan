package subscription.plan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileUserSpaceTest {
	@Test
	void getTotalSpaceOfCDrive() {
		assertEquals(137458384896L, new FileUserSpace() {
			@Override
			String getPathname(String username) {
				return "C:\\";
			}
		}.getTotalSpace("bogus"));
	}

	@Test
	void getPathnameOfUserUsername1() {
		assertEquals("/data/username1/data", new FileUserSpace().getPathname("username1"));
	}
}
