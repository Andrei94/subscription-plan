package subscription.plan;

import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserVolumeMountServiceTest {
	private final String user = "user";
	private final String device = "/dev/sdo";
	private UserVolumeMountService userVolumeMountService;
	private UserMountsService userMountsService;
	private DeviceList deviceList;
	private final String volumeId = "vol-07aa2a8cf7b8b15d7";

	@BeforeEach
	void setUp() {
		userMountsService = new UserMountsService();
		deviceList = new DeviceList();
		userVolumeMountService = new UserVolumeMountService(userMountsService, deviceList) {
			@Override
			public void initializeEC2Environment() {
				this.ec2InstanceId = "i-dmkj1892jdiu1";
				this.ec2AvailabilityZone = "eu-central-1a";
			}
		};
	}

	@Test
	void createVolume() {
		userVolumeMountService.setAwsAdapter(new DummyAWSAdapter() {
			@Override
			public CreateVolumeResult createVolume(CreateVolumeRequest req) {
				assertEquals(500, req.getSize());
				assertEquals("eu-central-1a", req.getAvailabilityZone());
				return new CreateVolumeResult().withVolume(new Volume().withVolumeId(volumeId));
			}

			@Override
			public DescribeVolumesResult describeVolumes(DescribeVolumesRequest req) {
				return new DescribeVolumesResult().withVolumes(new Volume().withVolumeId(volumeId).withState(VolumeState.Available));
			}
		});
		assertEquals(volumeId, userVolumeMountService.createVolume());
	}

	@Test
	void attachVolume() {
		userVolumeMountService.setAwsAdapter(new DummyAWSAdapter() {
			@Override
			public DescribeVolumesResult describeVolumes(DescribeVolumesRequest req) {
				return new DescribeVolumesResult()
						.withVolumes(new Volume()
								.withVolumeId(volumeId)
								.withAttachments(new VolumeAttachment().withState(VolumeAttachmentState.Attached)));
			}

			@Override
			public AttachVolumeResult attachVolume(AttachVolumeRequest req) {
				return new AttachVolumeResult()
						.withAttachment(new VolumeAttachment()
								.withVolumeId(req.getVolumeId())
								.withInstanceId(req.getInstanceId())
								.withDevice(req.getDevice()));
			}

			@Override
			public void sendCommand(SendCommandRequest req) {
				assertEquals("AWS-RunShellScript", req.getDocumentName());
				assertEquals("1", req.getDocumentVersion());
				assertEquals(Arrays.asList("mkfs -t xfs /dev/sdo",
						"mkdir -p /sftpg/user/data",
						"mount /dev/sdo /sftpg/user/data",
						"chown root.sftpg /sftpg/user/",
						"chown -R user.sftpg /sftpg/user/data"),
						req.getParameters().get("commands"));
				assertEquals(Collections.singletonList("40"), req.getParameters().get("executionTimeout"));
				assertEquals(userVolumeMountService.ec2InstanceId, req.getInstanceIds().get(0));
			}
		});
		userVolumeMountService.setDataSyncher(new DataSyncher() {
			@Override
			Process startAWSProcess(List<String> args) {
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
		});
		userVolumeMountService.attachVolume(user, volumeId);
		assertEquals(volumeId, userMountsService.getVolumeId(user));
		assertTrue(userMountsService.userExists(user));
		assertFalse(deviceList.getDeviceStatus(device));
	}

	@Test
	void attachVolumeFailed() {
		userVolumeMountService.setAwsAdapter(new DummyAWSAdapter() {
			@Override
			public AttachVolumeResult attachVolume(AttachVolumeRequest req) {
				throw new AmazonEC2Exception("exception");
			}
		});
		assertThrows(AmazonEC2Exception.class, () -> userVolumeMountService.attachVolume(user, volumeId));
		assertFalse(userMountsService.userExists(user));
		assertTrue(deviceList.getDeviceStatus(device));
	}

	@Test
	void createUser() {
		userVolumeMountService.setTokenStore(new TokenStore());
		userVolumeMountService.setAwsAdapter(new DummyAWSAdapter() {
			@Override
			public void sendCommand(SendCommandRequest req) {
				assertEquals("AWS-RunShellScript", req.getDocumentName());
				assertEquals("1", req.getDocumentVersion());
				assertEquals(Arrays.asList("useradd -g sftpg username2",
								"echo \"username2:" +
								"$(openssl rand -base64 32 | cut -c1-32 > /tmp/tokenusername2 && " +
								"cat /tmp/tokenusername2 | openssl passwd -1 -stdin -salt tnGKMjFm)\" | chpasswd -e"),
						req.getParameters().get("commands"));
				assertEquals(Collections.singletonList("40"), req.getParameters().get("executionTimeout"));
				assertEquals(userVolumeMountService.ec2InstanceId, req.getInstanceIds().get(0));
			}
		});
		userVolumeMountService.createUser("username2");
	}

	@Test
	void createUserHasToken() {
		TokenStore tokenStore = new TokenStore();
		tokenStore.putToken(user, "token");
		userVolumeMountService.setTokenStore(tokenStore);

		assertEquals("token", userVolumeMountService.createUser(user));
	}

	@Test
	void collectUsers() {
		List<String> users = new ArrayList<>();
		userMountsService.put("username1", new MountPoint("/dev/sdo", volumeId));
		userMountsService.put("username2", new MountPoint("/dev/sdn", "vol-07aa2a8cf7b8b15d9"));
		userMountsService.forEachUser((user, mountPoint) -> users.add(user));
		assertEquals(2, users.size());
	}

	@Test
	void dontDeleteEBSOfNonExistentUser() {
		userVolumeMountService.setAwsAdapter(new DummyAWSAdapter() {
			@Override
			public void sendCommand(SendCommandRequest req) {
				fail();
			}

			@Override
			public void deleteEBSVolume(String volumeId) {
				fail();
			}
		});
		userVolumeMountService.deleteVolume(user, new MountPoint("/dev/sdo", volumeId));
	}

	@Test
	void deleteEBSVolumeOfExistentUser() {
		userVolumeMountService.setAwsAdapter(new DummyAWSAdapter() {
			@Override
			public void sendCommand(SendCommandRequest req) {
				assertEquals(Arrays.asList("umount -l /dev/sdo", "rm -rf /sftpg/user/data/"), req.getParameters().get("commands"));
			}

			@Override
			public void deleteEBSVolume(String volumeId) {
				assertEquals(volumeId, UserVolumeMountServiceTest.this.volumeId);
			}
		});
		userMountsService.put(user, new MountPoint("/dev/sdo", volumeId));
		deviceList.markAsUsed("/dev/sdo");

		userVolumeMountService.deleteVolume(user, new MountPoint("/dev/sdo", volumeId));
		assertTrue(deviceList.getDeviceStatus("/dev/sdo"));
	}
}
