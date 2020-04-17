package subscription.plan;

import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
	public SendCommandRequest createShellCommandRequest(List<String> commands) {
		return new SendCommandRequest()
				.withDocumentName("AWS-RunShellScript")
				.withDocumentVersion("1")
				.withParameters(new HashMap<String, List<String>>() {{
					put("commands", commands);
					put("executionTimeout", Collections.singletonList("40"));
				}})
				.withInstanceIds("i-dmkj1892jdiu1");
	}

	@Override
	public void deleteEBSVolume(String volumeId) {
		throw new UnsupportedOperationException();
	}
}
