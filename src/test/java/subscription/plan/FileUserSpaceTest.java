package subscription.plan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileUserSpaceTest {
	private final FileUserSpace fileUserSpace = new FileUserSpace() {
		@Override
		String getUserPathname(String username) {
			return "C:\\";
		}
	};

	@Test
	void getTotalSpaceOfCDrive() {
		assertEquals(137458384896L, fileUserSpace.getTotalSpace("bogus"));
	}

	@Test
	void getAvailableSpaceOfCDrive() {
		assertTrue(fileUserSpace.getUsableSpace("bogus") < fileUserSpace.getTotalSpace("bogus"));
	}

	@Test
	void getPathnameOfUserUsername1() {
		assertEquals("/data/username1/data", new FileUserSpace().getUserPathname("username1"));
	}
}
