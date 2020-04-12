package subscription.plan;

import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;

public class DummyAWSAdapter implements AWSAdapter {
	@Override
	public CreateVolumeResult createVolume(CreateVolumeRequest req) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DescribeVolumesResult describeVolumes(DescribeVolumesRequest req) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AttachVolumeResult attachVolume(AttachVolumeRequest req) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendCommand(SendCommandRequest req) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendCommandAsync(SendCommandRequest shellCommandRequest) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteEBSVolume(String volumeId) {
		throw new UnsupportedOperationException();
	}
}
