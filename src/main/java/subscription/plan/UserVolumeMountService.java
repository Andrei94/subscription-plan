package subscription.plan;

import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;
import io.micronaut.context.annotation.Value;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class UserVolumeMountService implements VolumeService {
	private final UserService userService;
	private final DeviceList deviceList;
	@Value("${ec2-instance}")
	public String ec2InstanceId;
	@Value("${ec2-region}")
	public String ec2Region;
	@Inject
	public AWSAdapter awsAdapter;
	@Inject
	public TokenStore tokenStore;

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
		makeMountPointAvailableToUser(user, mp);
		userService.put(user, mp);
		deviceList.markAsUsed(mp.getDeviceName());
	}

	private void makeMountPointAvailableToUser(String user, MountPoint mp) {
		awsAdapter.sendCommandAsync(createShellCommandRequest(Stream.of("mkfs -t xfs {mountPoint}",
				"mkdir -p /sftpg/{user}/data",
				"mount {mountPoint} /sftpg/{user}/data",
				"chown root.sftpg /sftpg/{user}/",
				"chown -R {user}.sftpg /sftpg/{user}/data")
				.map(value -> value.replace("{user}", user).replace("{mountPoint}", mp.getDeviceName())).collect(Collectors.toList())));
	}

	@Override
	public String createUser(String user) {
		if(tokenStore.hasToken(user))
			return tokenStore.getToken(user);
		awsAdapter.sendCommand(createShellCommandRequest(Stream.of("useradd -g sftpg {user}",
				"echo \"{user}:$(openssl rand -base64 32 | cut -c1-32 > /tmp/token{user} && cat /tmp/token{user} | openssl passwd -1 -stdin -salt tnGKMjFm)\" | chpasswd -e")
				.map(value -> value.replace("{user}", user)).collect(Collectors.toList())));
		return getUserToken(user);
	}

	@Override
	public String getUserToken(String user) {
		if(tokenStore.hasToken(user))
			return tokenStore.getToken(user);
		File file = new File("/tmp/token" + user);
		try {
			Optional<String> token = Files.lines(file.toPath()).findFirst();
			if(token.isPresent()) {
				tokenStore.putToken(user, token.get());
				Files.delete(file.toPath());
				return token.get();
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public void deleteVolume(String user, MountPoint mountPoint) {
		if(!userService.userExists(user))
			return;
		awsAdapter.sendCommand(createShellCommandRequest("umount -l " + mountPoint.getDeviceName() + " && rm -rf /sftpg/" + user + "/data/"));
		deviceList.markAsFree(mountPoint.getDeviceName());
		awsAdapter.deleteEBSVolume(mountPoint.getVolumeId());
	}

	private SendCommandRequest createShellCommandRequest(List<String> commands) {
		return new SendCommandRequest()
				.withDocumentName("AWS-RunShellScript")
				.withDocumentVersion("1")
				.withParameters(new HashMap<String, List<String>>() {{
					put("commands", commands);
					put("executionTimeout", Collections.singletonList("40"));
				}})
				.withInstanceIds(ec2InstanceId);
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

	void setTokenStore(TokenStore tokenStore) {
		this.tokenStore = tokenStore;
	}
}
