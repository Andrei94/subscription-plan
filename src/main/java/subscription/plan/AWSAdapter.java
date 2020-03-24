package subscription.plan;

import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;

public interface AWSAdapter {
	CreateVolumeResult createVolume(CreateVolumeRequest req);

	DescribeVolumesResult describeVolumes(DescribeVolumesRequest req);

	AttachVolumeResult attachVolume(AttachVolumeRequest req);

	void sendCommand(SendCommandRequest req);

	void deleteEBSVolume(String volumeId);
}
