package subscription.plan;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class VolumeService {
	private final UserService userService;
	private final DeviceList deviceList;
	private AmazonEC2 ec2Client = AmazonEC2ClientBuilder.standard().withCredentials(new ProfileCredentialsProvider("default")).withRegion(Regions.EU_CENTRAL_1).build();
	private AWSSimpleSystemsManagement ssm = AWSSimpleSystemsManagementClientBuilder.standard().withCredentials(new ProfileCredentialsProvider("default")).withRegion(Regions.EU_CENTRAL_1).build();
	private final String instanceId = "i-002ed5146c5e2b375";

	public VolumeService(UserService userService, DeviceList deviceList) {
		this.userService = userService;
		this.deviceList = deviceList;
	}

	public String createVolume() {
		String volumeId = ec2Client
				.createVolume(
						new CreateVolumeRequest(500, "eu-central-1a")
								.withEncrypted(true)
								.withVolumeType(VolumeType.Sc1))
				.getVolume().getVolumeId();
		while(true) {
			Volume volumeStatusItem = ec2Client.describeVolumes(new DescribeVolumesRequest(Collections.singletonList(volumeId))).getVolumes().get(0);
			if(volumeStatusItem.getState().equals(VolumeState.Available.toString()))
				break;
		}
		return volumeId;
	}

	public void attachVolume(String user, String volumeId) {
		String device = deviceList.getFreeDevice();
		AttachVolumeResult attachVolumeResult = ec2Client.attachVolume(new AttachVolumeRequest(volumeId, instanceId, device));
		MountPoint mp = new MountPoint(attachVolumeResult.getAttachment().getDevice(), attachVolumeResult.getAttachment().getVolumeId());
		userService.put(user, mp);
		deviceList.markAsUsed(mp.getDeviceName());
		while(true) {
			Volume volumeStatusItem = ec2Client.describeVolumes(new DescribeVolumesRequest(Collections.singletonList(volumeId))).getVolumes().get(0);
			if(volumeStatusItem.getAttachments().get(0).getState().equals(VolumeAttachmentState.Attached.toString()))
				break;
		}
		makeMountPointAvailableToUser(user, mp);
	}

	private void makeMountPointAvailableToUser(String user, MountPoint mp) {
		String template = "mkfs -t xfs {mountPoint} && mkdir /mnt/{username} && mount {mountPoint} /mnt/{username}";
		ssm.sendCommand(new SendCommandRequest()
				.withDocumentName("AWS-RunShellScript")
				.withDocumentVersion("1")
				.withParameters(new HashMap<String, List<String>>() {{
					put("commands", Collections.singletonList(template.replace("{mountPoint}", mp.getDeviceName()).replace("{username}", user)));
					put("executionTimeout", Collections.singletonList("40"));
				}})
				.withInstanceIds(instanceId));
	}
}
