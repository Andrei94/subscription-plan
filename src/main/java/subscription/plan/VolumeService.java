package subscription.plan;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.ec2.AmazonEC2AsyncClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import java.util.Collections;

public class VolumeService {
	private final UserService userService;
	private final DeviceList deviceList;

	public VolumeService(UserService userService, DeviceList deviceList) {
		this.userService = userService;
		this.deviceList = deviceList;
	}

	public boolean existsVolume(String volumeId) {
		try {
			AmazonEC2AsyncClientBuilder.defaultClient().describeVolumes(new DescribeVolumesRequest().withVolumeIds(volumeId)).getVolumes();
			return true;
		} catch(AmazonEC2Exception ex) {
			return false;
		}
	}

	public String createVolume() {
		String volumeId = AmazonEC2ClientBuilder.defaultClient()
				.createVolume(
						new CreateVolumeRequest(500, "eu-central-1a")
								.withEncrypted(true)
								.withVolumeType(VolumeType.Sc1))
				.getVolume().getVolumeId();
		while(true) {
			Volume volumeStatusItem = AmazonEC2AsyncClientBuilder.defaultClient().describeVolumes(new DescribeVolumesRequest(Collections.singletonList(volumeId))).getVolumes().get(0);
			if(volumeStatusItem.getState().equals(VolumeState.Available.toString()))
				break;
		}
		return volumeId;
	}

	public void attachVolume(String user, String volumeId) {
		String device = deviceList.getFreeDevice();
		AmazonEC2AsyncClientBuilder.defaultClient().attachVolumeAsync(new AttachVolumeRequest(volumeId, "i-04e3b2054493c5e7a", device), new AsyncHandler<AttachVolumeRequest, AttachVolumeResult>() {
			@Override
			public void onError(Exception exception) {
				exception.printStackTrace();
			}

			@Override
			public void onSuccess(AttachVolumeRequest request, AttachVolumeResult attachVolumeResult) {
				MountPoint mp = new MountPoint(attachVolumeResult.getAttachment().getDevice(), attachVolumeResult.getAttachment().getVolumeId());
				userService.put(user, mp);
				deviceList.markAsUsed(mp.getDeviceName());
			}
		});
	}
}
