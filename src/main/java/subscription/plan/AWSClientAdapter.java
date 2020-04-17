package subscription.plan;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Async;
import com.amazonaws.services.ec2.AmazonEC2AsyncClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementAsync;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementAsyncClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.*;
import com.amazonaws.util.EC2MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class AWSClientAdapter implements AWSAdapter {
	private Logger logger = LoggerFactory.getLogger(AWSClientAdapter.class);
	private AmazonEC2Async ec2Client = AmazonEC2AsyncClientBuilder.standard().withCredentials(new ProfileCredentialsProvider("default")).withRegion(Regions.EU_CENTRAL_1).build();
	private AWSSimpleSystemsManagementAsync ssm = AWSSimpleSystemsManagementAsyncClientBuilder.standard().withCredentials(new ProfileCredentialsProvider("default")).withRegion(Regions.EU_CENTRAL_1).build();
	private ExecutorService commandExecutors = Executors.newFixedThreadPool(3);

	@Override
	public CreateVolumeResult createVolume(CreateVolumeRequest req) {
		logger.info("Creating volume");
		CreateVolumeResult volume = ec2Client.createVolume(req);
		logger.info("Volume {} created", volume.getVolume().getVolumeId());
		return volume;
	}

	@Override
	public DescribeVolumesResult describeVolumes(DescribeVolumesRequest req) {
		return ec2Client.describeVolumes(req);
	}

	@Override
	public AttachVolumeResult attachVolume(AttachVolumeRequest req) {
		logger.info("Attaching volume {}", req.getVolumeId());
		AttachVolumeResult result = ec2Client.attachVolume(req);
		waitForAttached(req.getVolumeId());
		logger.info("Volume {} attached", result.getAttachment().getVolumeId());
		return result;
	}

	@Override
	public void sendCommand(SendCommandRequest req) {
		try {
			SendCommandResult sendCommandResult = ssm.sendCommandAsync(req).get();
			GetCommandInvocationResult result;
			logger.info("CommandId: {}, InstanceId: {}", sendCommandResult.getCommand().getCommandId(), req.getInstanceIds().get(0));
			do {
				sleep(200);
				result = ssm.getCommandInvocation(new GetCommandInvocationRequest()
						.withCommandId(sendCommandResult.getCommand().getCommandId())
						.withInstanceId(req.getInstanceIds().get(0)));
			} while(result.getStatusDetails().equals(CommandInvocationStatus.Pending.toString()) || result.getStatusDetails().equals(CommandInvocationStatus.InProgress.toString()));
			logger.info("Command {} finished with state {}", result.getCommandId(), result.getStatusDetails());
		} catch(InterruptedException | ExecutionException | InvocationDoesNotExistException e) {
			logger.error("Who dares not to obey my commands?", e);
		}
	}

	@Override
	public void sendCommandAsync(SendCommandRequest shellCommandRequest) {
		commandExecutors.execute(() -> sendCommand(shellCommandRequest));
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
				.withInstanceIds(EC2MetadataUtils.getInstanceId());
	}

	@Override
	public void deleteEBSVolume(String volumeId) {
		try {
			ec2Client.detachVolumeAsync(new DetachVolumeRequest(volumeId), new AsyncHandler<DetachVolumeRequest, DetachVolumeResult>() {
				@Override
				public void onError(Exception exception) {
					logger.error("Who dares to keep this volume attached?", exception);
				}

				@Override
				public void onSuccess(DetachVolumeRequest request, DetachVolumeResult detachVolumeResult) {
					waitForAvailable(volumeId);
					logger.info("Deleting volume {}", volumeId);
					ec2Client.deleteVolume(new DeleteVolumeRequest(volumeId));
					logger.info("Volume {} deleted", volumeId);
				}
			}).get();
		} catch(InterruptedException | ExecutionException e) {
			logger.error("Who dares to take my money?", e);
		}
	}

	private void waitForAvailable(String volumeId) {
		logger.info("Waiting for volume {} to be available", volumeId);
		while(true) {
			Volume volumeStatusItem = describeVolumes(new DescribeVolumesRequest(Collections.singletonList(volumeId))).getVolumes().get(0);
			if(volumeStatusItem.getState().equals(VolumeState.Available.toString()))
				break;
			sleep(100);
		}
		logger.info("Volume {} available", volumeId);
	}

	private void waitForAttached(String volumeId) {
		while(true) {
			Volume volumeStatusItem = ec2Client.describeVolumes(new DescribeVolumesRequest(Collections.singletonList(volumeId))).getVolumes().get(0);
			if(volumeStatusItem.getAttachments().get(0).getState().equals(VolumeAttachmentState.Attached.toString()))
				break;
			sleep(100);
		}
		sleep(1000);
	}

	private void sleep(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch(InterruptedException e) {
			logger.error("Who dares to interrupt me?", e);
		}
	}
}
