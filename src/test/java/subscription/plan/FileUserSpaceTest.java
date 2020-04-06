package subscription.plan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUserSpaceTest {
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
	void getTotalSpaceOfInnexistentDrive() {
		FileUserSpace fileUserSpace = new FileUserSpace() {
			@Override
			String getUserPathname(String username) {
				return "QWE:\\";
			}
		};
		assertEquals(0, fileUserSpace.getTotalSpace("username"));
	}

	@Test
	void getAvailableSpaceOfCDrive() {
		assertTrue(fileUserSpace.getUsableSpace("bogus") < fileUserSpace.getTotalSpace("bogus"));
	}

	@Test
	void getAvailableSpaceOfInnexistentDrive() {
		FileUserSpace fileUserSpace = new FileUserSpace() {
			@Override
			String getUserPathname(String username) {
				return "QWE:\\";
			}
		};
		assertEquals(0, fileUserSpace.getUsableSpace("username"));
	}

	@Test
	void getPathnameOfUserUsername1() {
		assertEquals("/sftpg/username1/data", new FileUserSpace().getUserPathname("username1"));
	}
}
