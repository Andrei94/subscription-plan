package subscription.plan;

import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Collections;

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
		userVolumeMountService = new UserVolumeMountService(userMountsService, deviceList);
	}

	@Test
	void createVolume() {
		userVolumeMountService.setAwsAdapter(new AWSAdapter() {
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

			@Override
			public AttachVolumeResult attachVolume(AttachVolumeRequest req) {
				throw new NotImplementedException();
			}

			@Override
			public void sendCommand(SendCommandRequest req) {
				throw new NotImplementedException();
			}
		});
		assertEquals(volumeId, userVolumeMountService.createVolume());
	}

	@Test
	void attachVolume() {
		userVolumeMountService.setAwsAdapter(new AWSAdapter() {
			@Override
			public CreateVolumeResult createVolume(CreateVolumeRequest req) {
				throw new NotImplementedException();
			}

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
				assertEquals(Collections.singletonList("mkfs -t xfs /dev/sdo && mkdir /mnt/user && mount /dev/sdo /mnt/user"), req.getParameters().get("commands"));
				assertEquals(Collections.singletonList("40"), req.getParameters().get("executionTimeout"));
				assertNotNull(req.getInstanceIds());
			}
		});
		userVolumeMountService.attachVolume(user, volumeId);
		assertEquals(volumeId, userMountsService.getVolumeId(user));
		assertTrue(userMountsService.userExists(user));
		assertFalse(deviceList.getDeviceStatus(device));
	}

	@Test
	void attachVolumeFailed() {
		userVolumeMountService.setAwsAdapter(new AWSAdapter() {
			@Override
			public CreateVolumeResult createVolume(CreateVolumeRequest req) {
				throw new NotImplementedException();
			}

			@Override
			public DescribeVolumesResult describeVolumes(DescribeVolumesRequest req) {
				throw new NotImplementedException();
			}

			@Override
			public AttachVolumeResult attachVolume(AttachVolumeRequest req) {
				throw new AmazonEC2Exception("exception");
			}

			@Override
			public void sendCommand(SendCommandRequest req) {
				throw new NotImplementedException();
			}
		});
		assertThrows(AmazonEC2Exception.class, () -> userVolumeMountService.attachVolume(user, volumeId));
		assertFalse(userMountsService.userExists(user));
		assertTrue(deviceList.getDeviceStatus(device));
	}
}
