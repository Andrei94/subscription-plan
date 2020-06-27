package subscription.plan;

import com.amazonaws.services.ec2.model.*;
import com.amazonaws.util.EC2MetadataUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class UserVolumeMountService implements VolumeService {
	private final UserService userService;
	private final DeviceList deviceList;
	public String ec2InstanceId;
	public String ec2AvailabilityZone;
	@Inject
	public AWSAdapter awsAdapter;
	@Inject
	public TokenStore tokenStore;
	@Inject
	public DataSyncher syncher;

	public UserVolumeMountService(UserService userService, DeviceList deviceList) {
		this.userService = userService;
		this.deviceList = deviceList;
		initializeEC2Environment();
	}

	public void initializeEC2Environment() {
		this.ec2InstanceId = EC2MetadataUtils.getInstanceId();
		this.ec2AvailabilityZone = EC2MetadataUtils.getAvailabilityZone();
	}

	@Override
	public String createVolume() {
		String volumeId = awsAdapter
				.createVolume(
						new CreateVolumeRequest(500, ec2AvailabilityZone)
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
		AttachVolumeResult attachVolumeResult = awsAdapter.attachVolume(new AttachVolumeRequest(volumeId, ec2InstanceId, deviceList.getFreeDevice()));
		MountPoint mp = new MountPoint(attachVolumeResult.getAttachment().getDevice(), attachVolumeResult.getAttachment().getVolumeId());
		makeMountPointAvailableToUser(user, mp);
		userService.put(user, mp);
		deviceList.markAsUsed(mp.getDeviceName());
		syncher.downloadUserData(user);
	}

	private void makeMountPointAvailableToUser(String user, MountPoint mp) {
		awsAdapter.sendCommand(awsAdapter.createShellCommandRequest(Stream.of(
				"mkfs -t xfs {mountPoint}",
				"mkdir -p /sftpg/{user}/data",
				"mount {mountPoint} /sftpg/{user}/data",
				"chown root.sftpg /sftpg/{user}/",
				"chown -R {user}.sftpg /sftpg/{user}/data"
		).map(value -> value.replace("{user}", user).replace("{mountPoint}", mp.getDeviceName())).collect(Collectors.toList())));
	}

	@Override
	public String createUser(String user) {
		if(tokenStore.hasToken(user))
			return tokenStore.getToken(user);
		awsAdapter.sendCommand(awsAdapter.createShellCommandRequest(Stream.of(
				"useradd -g sftpg {user}",
				"echo \"{user}:$(openssl rand -base64 32 | cut -c1-32 > /tmp/token{user} && cat /tmp/token{user} | openssl passwd -1 -stdin -salt tnGKMjFm)\" | chpasswd -e"
		).map(value -> value.replace("{user}", user)).collect(Collectors.toList())));
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
		awsAdapter.sendCommand(awsAdapter.createShellCommandRequest(Arrays.asList(
				"umount -l " + mountPoint.getDeviceName(),
				"rm -rf /sftpg/" + user + "/data/"
		)));
		deviceList.markAsFree(mountPoint.getDeviceName());
		awsAdapter.deleteEBSVolume(mountPoint.getVolumeId());
	}

	void setAwsAdapter(AWSAdapter awsAdapter) {
		this.awsAdapter = awsAdapter;
	}

	void setTokenStore(TokenStore tokenStore) {
		this.tokenStore = tokenStore;
	}

	void setDataSyncher(DataSyncher syncher) {
		this.syncher = syncher;
	}

	@Override
	public boolean tokenHashMatch(String user, String token) {
		return tokenStore.getHashedToken(user).equals(token);
	}

	@Override
	public String getTokenHash(String user) {
		return tokenStore.getHashedToken(user);
	}
}
