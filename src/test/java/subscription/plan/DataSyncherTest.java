package subscription.plan;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DataSyncherTest {
	private DataSyncher dataSyncher;

	@Test
	void syncEbsToS3FinishedWork() {
		dataSyncher = new DataSyncher() {
			@Override
			Process startAWSProcess(String... args) {
				assertArrayEquals(new String[] {"s3", "sync", "/sftpg/username/data", "s3://backedup-storage-2/username", "--delete"}, args);
				return new Process() {
					@Override
					public OutputStream getOutputStream() {
						return null;
					}

					@Override
					public InputStream getInputStream() {
						return null;
					}

					@Override
					public InputStream getErrorStream() {
						return null;
					}

					@Override
					public int waitFor() {
						return 0;
					}

					@Override
					public int exitValue() {
						return 0;
					}

					@Override
					public void destroy() {
					}

					@Override
					public boolean isAlive() {
						return false;
					}
				};
			}
		};
		dataSyncher.syncEbsToS3ForUser("username");
		assertFalse(dataSyncher.s3SyncRunningForUser("username"));
	}

	@Test
	void syncEbsToS3ForUserCatchesIOException() {
		dataSyncher = new DataSyncher() {
			@Override
			Process startAWSProcess(String... args) throws IOException {
				throw new IOException();
			}
		};
		assertDoesNotThrow(() -> dataSyncher.syncEbsToS3ForUser("username"));
	}

	@Test
	void syncEbsToS3ForUserDontStartNewWorkWhenNotFinished() {
		boolean[] alive = {true};
		dataSyncher = new DataSyncher() {
			@Override
			Process startAWSProcess(String... args) {
				assertArrayEquals(new String[] {"s3", "sync", "/sftpg/username/data", "s3://backedup-storage-2/username", "--delete"}, args);
				return new Process() {
					@Override
					public OutputStream getOutputStream() {
						return null;
					}

					@Override
					public InputStream getInputStream() {
						return null;
					}

					@Override
					public InputStream getErrorStream() {
						return null;
					}

					@Override
					public int waitFor() {
						return 0;
					}

					@Override
					public int exitValue() {
						return 0;
					}

					@Override
					public void destroy() {
					}

					@Override
					public boolean isAlive() {
						return alive[0];
					}
				};
			}
		};
		dataSyncher.syncEbsToS3ForUser("username");
		assertTrue(dataSyncher.s3SyncRunningForUser("username"));
		alive[0] = false;
		dataSyncher.syncEbsToS3ForUser("username");
		assertFalse(dataSyncher.s3SyncRunningForUser("username"));
	}

	@Test
	void syncS3ToEBSForUser() {
		dataSyncher = new DataSyncher() {
			@Override
			Process startAWSProcess(String... args) {
				assertArrayEquals(new String[] {"s3", "sync", "s3://backedup-storage-2/username", "/sftpg/username/data", "--delete"}, args);
				return new Process() {
					@Override
					public OutputStream getOutputStream() {
						return null;
					}

					@Override
					public InputStream getInputStream() {
						return null;
					}

					@Override
					public InputStream getErrorStream() {
						return null;
					}

					@Override
					public int waitFor() {
						return 0;
					}

					@Override
					public int exitValue() {
						return 0;
					}

					@Override
					public void destroy() {
					}
				};
			}
		};
		dataSyncher.syncS3ToEBSForUser("username");
	}

	@Test
	void syncS3ToEBSForUserProcessThrowsException() {
		dataSyncher = new DataSyncher() {
			@Override
			Process startAWSProcess(String... args) throws IOException {
				throw new IOException();
			}
		};
		assertDoesNotThrow(() -> dataSyncher.syncS3ToEBSForUser("username"));
	}
}