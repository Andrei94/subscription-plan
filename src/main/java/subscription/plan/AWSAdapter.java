package subscription.plan;

import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;

import java.util.List;

public interface AWSAdapter {
	CreateVolumeResult createVolume(CreateVolumeRequest req);

	DescribeVolumesResult describeVolumes(DescribeVolumesRequest req);

	AttachVolumeResult attachVolume(AttachVolumeRequest req);

	void sendCommand(SendCommandRequest req);

	void sendCommandAsync(SendCommandRequest shellCommandRequest);

	SendCommandRequest createShellCommandRequest(List<String> commands);

	void deleteEBSVolume(String volumeId);
}
