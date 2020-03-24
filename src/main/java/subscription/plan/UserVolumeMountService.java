package subscription.plan;

import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;
import io.micronaut.context.annotation.Value;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Singleton
public class UserVolumeMountService implements VolumeService {
	private final UserService userService;
	private final DeviceList deviceList;
	@Value("${ec2-instance}")
	public String ec2InstanceId;
	@Value("${ec2-region}")
	public String ec2Region;
	@Inject
	private AWSAdapter awsAdapter;

	public UserVolumeMountService(UserService userService, DeviceList deviceList) {
		this.userService = userService;
		this.deviceList = deviceList;
	}

	@Override
	public String createVolume() {
		String volumeId = awsAdapter
				.createVolume(
						new CreateVolumeRequest(500, ec2Region)
								.withEncrypted(true)
								.withVolumeType(VolumeType.Sc1))
				.getVolume().getVolumeId();
		while(true) {
			Volume volumeStatusItem = awsAdapter.describeVolumes(new DescribeVolumesRequest(Collections.singletonList(volumeId))).getVolumes().get(0);
			if(volumeStatusItem.getState().equals(VolumeState.Available.toString()))
				break;
		}
		return volumeId;
	}

	@Override
	public void attachVolume(String user, String volumeId) {
		String device = deviceList.getFreeDevice();
		AttachVolumeResult attachVolumeResult = awsAdapter.attachVolume(new AttachVolumeRequest(volumeId, ec2InstanceId, device));
		MountPoint mp = new MountPoint(attachVolumeResult.getAttachment().getDevice(), attachVolumeResult.getAttachment().getVolumeId());
		userService.put(user, mp);
		deviceList.markAsUsed(mp.getDeviceName());
		while(true) {
			Volume volumeStatusItem = awsAdapter.describeVolumes(new DescribeVolumesRequest(Collections.singletonList(volumeId))).getVolumes().get(0);
			if(volumeStatusItem.getAttachments().get(0).getState().equals(VolumeAttachmentState.Attached.toString()))
				break;
		}
		makeMountPointAvailableToUser(user, mp);
	}

	private void makeMountPointAvailableToUser(String user, MountPoint mp) {
		awsAdapter.sendCommand(
				createShellCommandRequest(
						"mkfs -t xfs {mountPoint} && mkdir /mnt/{username} && mount {mountPoint} /mnt/{username}"
								.replace("{mountPoint}", mp.getDeviceName())
								.replace("{username}", user)
				)
		);
	}

	private SendCommandRequest createShellCommandRequest(String command) {
		return new SendCommandRequest()
				.withDocumentName("AWS-RunShellScript")
				.withDocumentVersion("1")
				.withParameters(new HashMap<String, List<String>>() {{
					put("commands", Collections.singletonList(command));
					put("executionTimeout", Collections.singletonList("40"));
				}})
				.withInstanceIds(ec2InstanceId);
	}

	void setAwsAdapter(AWSAdapter awsAdapter) {
		this.awsAdapter = awsAdapter;
	}
}
