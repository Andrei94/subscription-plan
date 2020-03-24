package subscription.plan;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;

import javax.inject.Singleton;
import java.util.Collections;

@Singleton
public class AWSClientAdapter implements AWSAdapter {
	private AmazonEC2 ec2Client = AmazonEC2ClientBuilder.standard().withCredentials(new ProfileCredentialsProvider("default")).withRegion(Regions.EU_CENTRAL_1).build();
	private AWSSimpleSystemsManagement ssm = AWSSimpleSystemsManagementClientBuilder.standard().withCredentials(new ProfileCredentialsProvider("default")).withRegion(Regions.EU_CENTRAL_1).build();

	@Override
	public CreateVolumeResult createVolume(CreateVolumeRequest req) {
		return ec2Client.createVolume(req);
	}

	@Override
	public DescribeVolumesResult describeVolumes(DescribeVolumesRequest req) {
		return ec2Client.describeVolumes(req);
	}

	@Override
	public AttachVolumeResult attachVolume(AttachVolumeRequest req) {
		return ec2Client.attachVolume(req);
	}

	@Override
	public void sendCommand(SendCommandRequest req) {
		ssm.sendCommand(req);
	}

	@Override
	public void deleteEBSVolume(String volumeId) {
		ec2Client.detachVolume(new DetachVolumeRequest(volumeId));
		while(true) {
			Volume volumeStatusItem = ec2Client.describeVolumes(new DescribeVolumesRequest(Collections.singletonList(volumeId))).getVolumes().get(0);
			if(volumeStatusItem.getState().equals(VolumeState.Available.toString()))
				break;
		}
		ec2Client.deleteVolume(new DeleteVolumeRequest(volumeId));
	}
}
