/*
 *  *
 *  Copyright (C) 2009-2015 Dell, Inc.
 *  See annotations for authorship information
 *
 *  ====================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ====================================================================
 *
 */

package org.dasein.cloud.aws.compute;

import org.dasein.cloud.AbstractProviderService;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AwsTestBase;
import org.dasein.cloud.aws.network.ElasticIP;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.SpotPriceHistory;
import org.dasein.cloud.compute.SpotPriceHistoryFilterOptions;
import org.dasein.cloud.compute.SpotVirtualMachineRequest;
import org.dasein.cloud.compute.SpotVirtualMachineRequestFilterOptions;
import org.dasein.cloud.compute.SpotVirtualMachineRequestType;
import org.dasein.cloud.compute.VMFilterOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineLifecycle;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineProductFilterOptions;
import org.dasein.cloud.compute.VirtualMachineStatus;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.VmStatus;
import org.dasein.cloud.compute.VmStatusFilterOptions;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeState;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.method;

/**
 * Created by Jeffrey Yan on 11/19/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AWSCloud.class, EC2Instance.class, ElasticIP.class })
public class EC2InstanceTest extends AwsTestBase {

    private EC2Instance ec2Instance;

    @Before
    public void setUp() {
        super.setUp();
        ec2Instance = new EC2Instance(awsCloudStub);
    }

    @Test
    public void testGetPassword() throws Exception {
        String instanceId = "i-2574e22a";

        EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke()).thenReturn(resource("org/dasein/cloud/aws/compute/instance/get_password.xml"));

        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId", instanceId), hasEntry("Action", "GetPasswordData"))))
                .thenReturn(ec2MethodStub);

        assertEquals("TGludXggdmVyc2lvbiAyLjYuMTYteGVuVSAoYnVpbGRlckBwYXRjaGJhdC5hbWF6b25zYSkgKGdj",
                ec2Instance.getPassword(instanceId));
    }

    @Test
    public void testGetVirtualMachine() throws Exception {
        String instanceId = "i-2574e22a";

        EC2Method listIpMethodStub = mock(EC2Method.class);
        when(listIpMethodStub.invoke()).thenReturn(resource("org/dasein/cloud/aws/network/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(listIpMethodStub);

        EC2Method getVirtualMachineMethodStub = mock(EC2Method.class);
        when(getVirtualMachineMethodStub.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/describe_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId), hasEntry("Action", "DescribeInstances"))))
                .thenReturn(getVirtualMachineMethodStub);

        VirtualMachine virtualMachine = ec2Instance.getVirtualMachine(instanceId);
        assertEquals("eipalloc-08229861", virtualMachine.getProviderAssignedIpAddressId());
        assertEquals(Architecture.I64, virtualMachine.getArchitecture());
        assertEquals(VmState.RUNNING, virtualMachine.getCurrentState());
        assertEquals(Platform.WINDOWS, virtualMachine.getPlatform());
        assertEquals("c1.medium", virtualMachine.getProductId());
        assertEquals("us-west-2a", virtualMachine.getProviderDataCenterId());
        assertEquals("ami-1a2b3c4d", virtualMachine.getProviderMachineImageId());
        assertEquals("subnet-1a2b3c4d", virtualMachine.getProviderSubnetId());
        assertEquals("vpc-1a2b3c4d", virtualMachine.getProviderVlanId());
        assertEquals("my-key-pair", virtualMachine.getProviderKeypairId());
        assertEquals(1, virtualMachine.getProviderFirewallIds().length);
        assertEquals("sg-1a2b3c4d", virtualMachine.getProviderFirewallIds()[0]);
        assertEquals(1, virtualMachine.getPublicIpAddresses().length);
        assertEquals("46.51.219.63", virtualMachine.getPublicIpAddresses()[0]);
        assertEquals(ACCOUNT_NO, virtualMachine.getProviderOwnerId());
        assertEquals(REGION, virtualMachine.getProviderRegionId());

        assertEquals(1, virtualMachine.getTags().size());
        assertEquals("Windows Instance", virtualMachine.getTag("Name"));

        Volume volume = virtualMachine.getVolumes()[0];
        assertEquals("/dev/sda1", volume.getDeviceId());
        assertEquals(VolumeState.PENDING, volume.getCurrentState());
        assertTrue(volume.isDeleteOnVirtualMachineTermination());
    }

    @Test
    public void testAlterVirtualMachineProduct() throws Exception {
        String instanceId = "i-2574e22a";
        String targetProductType = "c1.large";

        EC2Method alterVirtualMachineMethodMock = mock(EC2Method.class);
        when(alterVirtualMachineMethodMock.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/modify_instance_attribute.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq("ec2"), eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId", instanceId), hasEntry("InstanceType.Value", targetProductType),
                        hasEntry("Action", "ModifyInstanceAttribute")))).thenReturn(alterVirtualMachineMethodMock);

        EC2Method getVirtualMachineMethodStub = mock(EC2Method.class);
        when(getVirtualMachineMethodStub.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/describe_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId), hasEntry("Action", "DescribeInstances"))))
                .thenReturn(getVirtualMachineMethodStub);

        ec2Instance.alterVirtualMachineProduct(instanceId, targetProductType);

        verify(alterVirtualMachineMethodMock, times(1)).invoke();
    }

    @Test
    public void testAlterVirtualMachineFirewalls() throws Exception {
        String instanceId = "i-2574e22a";
        String targetFirewall0 = "fwl-1";
        String targetFirewall1 = "fwl-2";

        EC2Method alterVirtualMachineMethodStub = mock(EC2Method.class);
        when(alterVirtualMachineMethodStub.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/modify_instance_attribute.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq("ec2"), eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId", instanceId), hasEntry("GroupId.0", targetFirewall0),
                        hasEntry("GroupId.1", targetFirewall1), hasEntry("Action", "ModifyInstanceAttribute"))))
                .thenReturn(alterVirtualMachineMethodStub);

        EC2Method describeInstanceMethodStub = mock(EC2Method.class);
        when(describeInstanceMethodStub.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/describe_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId), hasEntry("Action", "DescribeInstances"))))
                .thenReturn(describeInstanceMethodStub);

        ec2Instance.alterVirtualMachineFirewalls(instanceId, new String[] { targetFirewall0, targetFirewall1 });

        verify(alterVirtualMachineMethodStub, times(1)).invoke();
    }

    @Test
    public void testGetUserData() throws Exception {
        String instanceId = "i-2574e22a";

        EC2Method getUserDataMethodStub = mock(EC2Method.class);
        when(getUserDataMethodStub.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/get_userdata.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId", instanceId), hasEntry("Attribute", "userData"),
                        hasEntry("Action", "DescribeInstanceAttribute")))).thenReturn(getUserDataMethodStub);

        assertEquals("exmple_user_data", ec2Instance.getUserData(instanceId));
    }

    @Test
    public void testGetConsoleOutput() throws Exception {
        String instanceId = "i-2574e22a";

        EC2Method getUserDataMethodStub = mock(EC2Method.class);
        when(getUserDataMethodStub.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/get_consoleoutput.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId", instanceId), hasEntry("Action", "GetConsoleOutput"))))
                .thenReturn(getUserDataMethodStub);

        String consoleOutput = ec2Instance.getConsoleOutput(instanceId);
        assertTrue(consoleOutput.startsWith("Linux version 2.6.16-xenU"));
    }

    @Test
    public void testListFirewalls() throws Exception {
        String instanceId = "i-2574e22a";

        EC2Method describeInstanceMethodStub = mock(EC2Method.class);
        when(describeInstanceMethodStub.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/describe_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId), hasEntry("Action", "DescribeInstances"))))
                .thenReturn(describeInstanceMethodStub);

        List<String> firewalls = (List<String>) ec2Instance.listFirewalls(instanceId);
        assertEquals(3, firewalls.size());
        assertEquals("sg-1a2b3c4d", firewalls.get(0));
    }

    @Test
    public void testGetVMStatus() throws Exception {
        String instanceId1 = "i-2574e22a";
        String instanceId2 = "i-2a2b3c4d";
        VmStatus status = VmStatus.IMPAIRED;

        EC2Method describeInstanceStatusMethodStub = mock(EC2Method.class);
        when(describeInstanceStatusMethodStub.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/describe_instance_status.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId1), hasEntry("InstanceId.2", instanceId2),
                        hasEntry("Filter.0.Name", "system-status.status"), hasEntry("Filter.0.Value.0", "impaired"),
                        hasEntry("Filter.1.Name", "instance-status.status"), hasEntry("Filter.1.Value.0", "impaired"),
                        hasEntry("Action", "DescribeInstanceStatus")))).thenReturn(describeInstanceStatusMethodStub);

        List<VirtualMachineStatus> vmStatus = (List<VirtualMachineStatus>) ec2Instance.getVMStatus(
                VmStatusFilterOptions.getInstance().withVmIds(instanceId1, instanceId2)
                        .withVmStatuses(Sets.newSet(VmStatus.IMPAIRED)));

        assertEquals(1, vmStatus.size());
        assertEquals(instanceId1, vmStatus.get(0).getProviderVirtualMachineId());
        assertEquals(status, vmStatus.get(0).getProviderHostStatus());
        assertEquals(status, vmStatus.get(0).getProviderVmStatus());
    }

    @Test
    public void testListVirtualMachineStatus() throws Exception {
        String instanceId = "i-2574e22a";

        EC2Method describeInstanceMethodStub = mock(EC2Method.class);
        when(describeInstanceMethodStub.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/describe_instance.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(hasEntry("Action", "DescribeInstances"))))
                .thenReturn(describeInstanceMethodStub);

        List<ResourceStatus> resourceStatuses = (List<ResourceStatus>) ec2Instance.listVirtualMachineStatus();
        assertEquals(1, resourceStatuses.size());
        assertEquals(instanceId, resourceStatuses.get(0).getProviderResourceId());
        assertEquals(VmState.RUNNING, resourceStatuses.get(0).getResourceStatus());
    }

    @Test
    public void testListVirtualMachines() throws Exception {
        EC2Method listIpMethodStub = mock(EC2Method.class);
        when(listIpMethodStub.invoke()).thenReturn(resource("org/dasein/cloud/aws/network/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(listIpMethodStub);

        String spotRequestId = "spot-1a2b3c4d";
        String tagKey = "tk-1a2b3c4d";
        String tagValue = "tv-1a2b3c4d";

        Map<String, String> tags = new HashMap<>();
        tags.put(tagKey, tagValue);

        EC2Method describeInstanceMethodStub = mock(EC2Method.class);
        when(describeInstanceMethodStub.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/describe_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("Filter.0.Name", "tag:" + tagKey), hasEntry("Filter.0.Value.0", tagValue),
                        hasEntry("Filter.1.Name", "instance-state-name"), hasEntry("Filter.1.Value.0", "running"),
                        hasEntry("Filter.2.Name", "instance-lifecycle"), hasEntry("Filter.2.Value.0", "spot"),
                        hasEntry("Filter.3.Name", "spot-instance-request-id"),
                        hasEntry("Filter.3.Value.0", spotRequestId), hasEntry("Action", "DescribeInstances"))))
                .thenReturn(describeInstanceMethodStub);

        List<VirtualMachine> virtualMachines = (List<VirtualMachine>) ec2Instance.listVirtualMachines(
                VMFilterOptions.getInstance().withVmStates(Sets.newSet(VmState.RUNNING))
                        .withLifecycles(VirtualMachineLifecycle.SPOT).withSpotRequestId(spotRequestId).withTags(tags));

        assertEquals(1, virtualMachines.size());
    }

    @Test
    public void testEnableAnalytics() throws Exception {
        String instanceId = "i-2574e22a";

        EC2Method monitorInstanceMock = mock(EC2Method.class);
        when(monitorInstanceMock.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/monitor_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId), hasEntry("Action", "MonitorInstances"))))
                .thenReturn(monitorInstanceMock);

        ec2Instance.enableAnalytics(instanceId);

        verify(monitorInstanceMock, times(1)).invoke();
    }

    @Test
    public void testDisableAnalytics() throws Exception {
        String instanceId = "i-2574e22a";

        EC2Method unmonitorInstanceMock = mock(EC2Method.class);
        when(unmonitorInstanceMock.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/unmonitor_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId), hasEntry("Action", "UnmonitorInstances"))))
                .thenReturn(unmonitorInstanceMock);

        ec2Instance.disableAnalytics(instanceId);

        verify(unmonitorInstanceMock, times(1)).invoke();
    }

    //getVMStatistics not tested, as no sample response data

    @Test
    public void testEnableSpotDataFeedSubscription() throws Exception {
        String bucketName = "bkt-1a2b3c4d";

        EC2Method createSpotDatafeedSubscriptionMock = mock(EC2Method.class);
        when(createSpotDatafeedSubscriptionMock.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/create_spot_datafeed_subscription.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("Bucket", bucketName), hasEntry("Action", "CreateSpotDatafeedSubscription"))))
                .thenReturn(createSpotDatafeedSubscriptionMock);

        ec2Instance.enableSpotDataFeedSubscription(bucketName);

        verify(createSpotDatafeedSubscriptionMock, times(1)).invoke();
    }

    @Test
    public void testCancelSpotDataFeedSubscription() throws Exception {
        EC2Method deleteSpotDatafeedSubscriptionMock = mock(EC2Method.class);
        when(deleteSpotDatafeedSubscriptionMock.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/create_spot_datafeed_subscription.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(hasEntry("Action", "DeleteSpotDatafeedSubscription")))
                .thenReturn(deleteSpotDatafeedSubscriptionMock);

        ec2Instance.cancelSpotDataFeedSubscription();

        verify(deleteSpotDatafeedSubscriptionMock, times(1)).invoke();
    }

    //createSpotVirtualMachineRequest not tested

    @Test
    public void testCancelSpotVirtualMachineRequest() throws Exception {
        String spotInstanceRequestId = "sir-1a2b3c4d";

        EC2Method cancelSpotInstanceRequestMock = mock(EC2Method.class);
        when(cancelSpotInstanceRequestMock.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/cancel_spot_instance_request.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("SpotInstanceRequestId.1", spotInstanceRequestId),
                        hasEntry("Action", "CancelSpotInstanceRequests")))).thenReturn(cancelSpotInstanceRequestMock);

        ec2Instance.cancelSpotVirtualMachineRequest(spotInstanceRequestId);

        verify(cancelSpotInstanceRequestMock, times(1)).invoke();
    }

    @Test
    public void testListSpotVirtualMachineRequests() throws Exception {
        String instanceId = "i-2574e22a";
        String spotInstanceRequestId = "sir-1a2b3c4d";
        String launchGroupId = "lg-1a2b3c4d";
        String launchImageId = "ami-1a2b3c4d";
        String launchProductId = "c1.medium";
        SpotVirtualMachineRequestType launchType = SpotVirtualMachineRequestType.ONE_TIME;
        long validFrom = 1417392000000l; //"2014-12-01T00:00:00.000Z";
        long validUntil = 1420070399000l; //"2014-12-31T23:59:59.000Z";
        float price = 0.10f;

        TimeZone backup = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        EC2Method listIpMethodStub = mock(EC2Method.class);
        when(listIpMethodStub.invoke()).thenReturn(resource("org/dasein/cloud/aws/network/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(listIpMethodStub);

        EC2Method getVirtualMachineMethodStub = mock(EC2Method.class);
        when(getVirtualMachineMethodStub.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/describe_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId), hasEntry("Action", "DescribeInstances"))))
                .thenReturn(getVirtualMachineMethodStub);

        EC2Method cancelSpotInstanceRequestMock = mock(EC2Method.class);
        when(cancelSpotInstanceRequestMock.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/describe_spot_instance_request.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("Filter.1.Name", "launch-group"), hasEntry("Filter.1.Value.0", launchGroupId),
                        hasEntry("Filter.2.Name", "launch.image-id"), hasEntry("Filter.2.Value.0", launchImageId),
                        hasEntry("Filter.3.Name", "spot-price"), hasEntry("Filter.3.Value.0", Float.toString(price)),
                        hasEntry("Filter.4.Name", "valid-from"), hasEntry("Filter.4.Value.0", Long.toString(validFrom)),
                        hasEntry("Filter.5.Name", "valid-until"),
                        hasEntry("Filter.5.Value.0", Long.toString(validUntil)),
                        hasEntry("Filter.6.Name", "launch.instance-type"),
                        hasEntry("Filter.6.Value.0", launchProductId), hasEntry("Filter.7.Name", "type"),
                        hasEntry("Filter.7.Value.0", "one-time"),
                        hasEntry("SpotInstanceRequestId.1", spotInstanceRequestId),
                        hasEntry("Action", "DescribeSpotInstanceRequests")))).thenReturn(cancelSpotInstanceRequestMock);

        List<SpotVirtualMachineRequest> spotVirtualMachineRequests = (List<SpotVirtualMachineRequest>) ec2Instance
                .listSpotVirtualMachineRequests(
                        SpotVirtualMachineRequestFilterOptions.getInstance().withSpotRequestIds(spotInstanceRequestId)
                                .inLaunchGroup(launchGroupId).withMachineImageId(launchImageId)
                                .withStandardProductId(launchProductId).ofType(launchType).validFrom(validFrom)
                                .validUntil(validUntil).withSpotPrice(price));

        assertEquals(1, spotVirtualMachineRequests.size());
        SpotVirtualMachineRequest spotVirtualMachineRequest = spotVirtualMachineRequests.get(0);
        assertEquals(spotInstanceRequestId, spotVirtualMachineRequest.getProviderSpotVmRequestId());
        assertEquals(price, spotVirtualMachineRequest.getSpotPrice(), 2);
        assertEquals(launchType, spotVirtualMachineRequest.getType());
        assertEquals(launchImageId, spotVirtualMachineRequest.getProviderMachineImageId());
        assertEquals(launchProductId, spotVirtualMachineRequest.getProductId());
        assertEquals(validFrom, spotVirtualMachineRequest.getValidFromTimestamp());
        assertEquals(validUntil, spotVirtualMachineRequest.getValidUntilTimestamp());
        assertEquals(launchGroupId, spotVirtualMachineRequest.getLaunchGroup());

        TimeZone.setDefault(backup);
    }

    @Test
    public void testListSpotPriceHistories() throws Exception {
        String instanceType = "m3.medium";
        String startTime = "2014-12-01T00:00:00.000Z";
        String endTime = "2014-12-31T23:59:59.000Z";

        TimeZone backup = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        EC2Method describeSpotPriceHistoryMock = mock(EC2Method.class);
        when(describeSpotPriceHistoryMock.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/describe_spot_price_history.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceType.1", instanceType), hasEntry("StartTime", startTime),
                        hasEntry("EndTime", endTime), hasEntry("Action", "DescribeSpotPriceHistory"))))
                .thenReturn(describeSpotPriceHistoryMock);

        List<SpotPriceHistory> spotPriceHistories = (List<SpotPriceHistory>) ec2Instance.listSpotPriceHistories(
                SpotPriceHistoryFilterOptions.getInstance().matchingProducts(new String[] { instanceType })
                        .matchingInterval(awsCloudStub.parseTime(startTime), awsCloudStub.parseTime(endTime)));

        assertEquals(1, spotPriceHistories.size());
        assertEquals(instanceType, spotPriceHistories.get(0).getProductId());
        assertEquals("us-west-2a", spotPriceHistories.get(0).getProviderDataCenterId());
        assertEquals(2, spotPriceHistories.get(0).getPriceHistory().length);

        TimeZone.setDefault(backup);
    }

    @Test
    public void startStart() throws Exception {
        String instanceId = "i-2574e22a";

        EC2Method listIpMethodStub = mock(EC2Method.class);
        when(listIpMethodStub.invoke()).thenReturn(resource("org/dasein/cloud/aws/network/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(listIpMethodStub);

        EC2Method getVirtualMachineMethodStub = mock(EC2Method.class);
        when(getVirtualMachineMethodStub.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/describe_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId), hasEntry("Action", "DescribeInstances"))))
                .thenReturn(getVirtualMachineMethodStub);

        EC2Method startInstanceMock = mock(EC2Method.class);
        when(startInstanceMock.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/start_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq("ec2"), eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId), hasEntry("Action", "StartInstances"))))
                .thenReturn(startInstanceMock);

        ec2Instance.start(instanceId);

        verify(startInstanceMock, times(1)).invoke();
    }

    @Test
    public void testStop() throws Exception {
        String instanceId = "i-2574e22a";

        EC2Method listIpMethodStub = mock(EC2Method.class);
        when(listIpMethodStub.invoke()).thenReturn(resource("org/dasein/cloud/aws/network/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(listIpMethodStub);

        EC2Method getVirtualMachineMethodStub = mock(EC2Method.class);
        when(getVirtualMachineMethodStub.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/describe_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId), hasEntry("Action", "DescribeInstances"))))
                .thenReturn(getVirtualMachineMethodStub);

        EC2Method startInstanceMock = mock(EC2Method.class);
        when(startInstanceMock.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/stop_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId), hasEntry("Action", "StopInstances"),
                        hasEntry("Force", "true")))).thenReturn(startInstanceMock);

        ec2Instance.stop(instanceId, true);

        verify(startInstanceMock, times(1)).invoke();
    }

    @Test
    public void testReboot() throws Exception {
        String instanceId = "i-2574e22a";

        EC2Method startInstanceMock = mock(EC2Method.class);
        when(startInstanceMock.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/reboot_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId), hasEntry("Action", "RebootInstances"))))
                .thenReturn(startInstanceMock);

        ec2Instance.reboot(instanceId);

        verify(startInstanceMock, times(1)).invoke();
    }

    @Test
    public void testTerminate() throws Exception {
        String instanceId = "i-2574e22a";

        EC2Method startInstanceMock = mock(EC2Method.class);
        when(startInstanceMock.invoke())
                .thenReturn(resource("org/dasein/cloud/aws/compute/instance/terminate_instance.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId.1", instanceId), hasEntry("Action", "TerminateInstances"))))
                .thenReturn(startInstanceMock);

        ec2Instance.terminate(instanceId);

        verify(startInstanceMock, times(1)).invoke();
    }

    /**
     * Tests that the list of *all* products is not empty (FB8437).
     *
     * @throws Exception
     */
    @Test
    public void testListAllProducts() throws Exception {
        EC2Instance ec2InstanceMock = PowerMockito.mock(EC2Instance.class);

        when(ec2InstanceMock.listAllProducts()).thenCallRealMethod();
        when(ec2InstanceMock.listProducts(any(VirtualMachineProductFilterOptions.class), any(Architecture.class)))
                .thenCallRealMethod();
        when(ec2InstanceMock.toProduct(any(JSONObject.class))).thenCallRealMethod();
        PowerMockito.when(ec2InstanceMock, method(AbstractProviderService.class, "getProvider")).withNoArguments()
                .thenReturn(awsCloudStub);
        PowerMockito.when(ec2InstanceMock, method(EC2Instance.class, "getContext")).withNoArguments()
                .thenReturn(providerContextStub);
        Iterable<VirtualMachineProduct> products = ec2InstanceMock.listAllProducts();
        int count = 0;
        for (VirtualMachineProduct p : products) {
            count++;
        }
        assertThat("Product count should be greater than zero", count, greaterThan(0));
    }
}
