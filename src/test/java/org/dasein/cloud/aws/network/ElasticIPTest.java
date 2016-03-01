package org.dasein.cloud.aws.network;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.unitils.reflectionassert.ReflectionAssert.*;

import java.util.Arrays;
import java.util.Collections;

import org.apache.http.HttpStatus;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AwsTestBase;
import org.dasein.cloud.aws.compute.EC2ComputeServices;
import org.dasein.cloud.aws.compute.EC2Exception;
import org.dasein.cloud.aws.compute.EC2Instance;
import org.dasein.cloud.aws.compute.EC2Method;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.network.AddressType;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSCloud.class, EC2Instance.class, ElasticIP.class})
public class ElasticIPTest extends AwsTestBase {

	private ElasticIP elasticIP;
	
	private static final String ASSIGN_INSTANCE_ID = "i-2574e22a";
	
	@Rule
    public final TestName name = new TestName();
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		elasticIP = new ElasticIP(awsCloudStub);
		
		try {
			if(name.getMethodName().startsWith("assignShouldPostWithCorrectRequest")) { //assign
			
				EC2Method describeAddressesMethodStub = mock(EC2Method.class);
		        when(describeAddressesMethodStub.invoke())
		        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/describe_addresses.xml"));
		        PowerMockito.whenNew(EC2Method.class)
		            .withArguments(eq(awsCloudStub), argThat(allOf(
		            		hasEntry("Action", "DescribeAddresses"))))
		            .thenReturn(describeAddressesMethodStub);
		        
				VirtualMachine virtualMachineStub = PowerMockito.mock(VirtualMachine.class);
				EC2ComputeServices computeServicesStub = PowerMockito.mock(EC2ComputeServices.class);
				EC2Instance vmSupportStub = PowerMockito.mock(EC2Instance.class);
				PowerMockito.doReturn(computeServicesStub).when(awsCloudStub).getComputeServices();
				PowerMockito.doReturn(vmSupportStub).when(computeServicesStub).getVirtualMachineSupport();
				PowerMockito.doReturn(virtualMachineStub).when(vmSupportStub).getVirtualMachine(ASSIGN_INSTANCE_ID);
				if (name.getMethodName().endsWith("RunningVm")) {
					PowerMockito.doReturn(VmState.RUNNING).when(virtualMachineStub).getCurrentState();
				} else if (name.getMethodName().endsWith("StoppedVm")) {
					PowerMockito.doReturn(VmState.STOPPED).when(virtualMachineStub).getCurrentState();
				} else if (name.getMethodName().endsWith("PausedVm")) {
					PowerMockito.doReturn(VmState.PAUSED).when(virtualMachineStub).getCurrentState();
				} else if (name.getMethodName().endsWith("SuspendedVm")) {
					PowerMockito.doReturn(VmState.SUSPENDED).when(virtualMachineStub).getCurrentState();
				}
				
		        EC2Method associateAddressSuccessMethodStub = mock(EC2Method.class);
		        when(associateAddressSuccessMethodStub.invoke())
			    	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/associate_address_success.xml"));
		        PowerMockito.whenNew(EC2Method.class)
					.withArguments(eq(awsCloudStub), argThat(allOf(
							hasEntry("InstanceId", ASSIGN_INSTANCE_ID), 
							hasEntry("Action", "AssociateAddress"))))
					.thenReturn(associateAddressSuccessMethodStub);
			} else if (name.getMethodName().startsWith("getIpAddress")) {
				EC2Method listIpMethodStub = mock(EC2Method.class);
				if (name.getMethodName().endsWith("InvalidAllocationIDNotFound")) {
					when(listIpMethodStub.invoke())
			        	.thenThrow(EC2Exception.create(
			        			HttpStatus.SC_METHOD_FAILURE, null, 
			        			"InvalidAllocationID.NotFound", "InvalidAllocationID.NotFound"));
				} else if (name.getMethodName().endsWith("InvalidAddressNotFound")) {
					when(listIpMethodStub.invoke())
		        		.thenThrow(EC2Exception.create(
		        				HttpStatus.SC_METHOD_FAILURE, null, 
		        				"InvalidAddress.NotFound", "InvalidAddress.NotFound"));
				} else {
					when(listIpMethodStub.invoke()).thenReturn(
			        		resource("org/dasein/cloud/aws/network/ipaddress/describe_addresses.xml"));
				}
				PowerMockito.whenNew(EC2Method.class)
	                .withArguments(eq(awsCloudStub), argThat(allOf(
	                		hasEntry("Action", "DescribeAddresses"))))
	                .thenReturn(listIpMethodStub);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void getIpAddressForVpcAddressShouldReturnCorrectResult() throws Exception {
		String ipAddressId = "eipalloc-08229861";
        assertReflectionEquals(
        		createIpAddress(ipAddressId, "eipassoc-f0229899", AddressType.PUBLIC, "eni-ef229886", IPVersion.IPV4, "46.51.219.63", "i-64600030", true), 
        		elasticIP.getIpAddress(ipAddressId));
	}
	
	@Test
	public void getIpAddressForVpcAddressShouldReturnNullIfInvalidAllocationIDNotFound() throws Exception {
		assertNull(elasticIP.getIpAddress("eipalloc-08229861"));
	}
	
	@Test
	public void getIpAddressForVpcAddressShouldReturnNullIfInvalidAddressNotFound() throws Exception {
		assertNull(elasticIP.getIpAddress("eipalloc-08229861"));
	}
	
	@Test
	public void getIpAddressForIpAddressShouldReturnCorrectResult() throws Exception {
		String ipAddressId = "46.51.219.64";
		assertReflectionEquals(
				createIpAddress(ipAddressId, "eipassoc-f0229810", AddressType.PUBLIC, "eni-ef229810", IPVersion.IPV4, ipAddressId, "i-64600031", false), 
				elasticIP.getIpAddress(ipAddressId));
	}
	
	@Test
	public void getIpAddressForIpAddressShouldReturnNullIfInvalidAllocationIDNotFound() throws Exception {
		assertNull(elasticIP.getIpAddress("46.51.219.64"));
	}
	
	@Test
	public void getIpAddressForIpAddressShouldReturnNullIfInvalidAddressNotFound() throws Exception {
		assertNull(elasticIP.getIpAddress("46.51.219.64"));
	}
	
	@Test
	public void listAllPublicIpPoolShouldReturnCorrectResult() throws Exception {
		
		EC2Method listIpMethodStub = mock(EC2Method.class);
        when(listIpMethodStub.invoke()).thenReturn(
        		resource("org/dasein/cloud/aws/network/ipaddress/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(listIpMethodStub);
		
		assertReflectionEquals(
				Arrays.asList(
						createIpAddress("eipalloc-08229861", "eipassoc-f0229899", AddressType.PUBLIC, "eni-ef229886", IPVersion.IPV4, "46.51.219.63", "i-64600030", true),
						createIpAddress("46.51.219.64", "eipassoc-f0229810", AddressType.PUBLIC, "eni-ef229810", IPVersion.IPV4, "46.51.219.64", "i-64600031", false),
						createIpAddress("198.51.100.2", null, AddressType.PUBLIC, null, IPVersion.IPV4, "198.51.100.2", null, false)), 
				elasticIP.listPublicIpPool(false));
	}
	
	@Test
	public void listUnassignedPublicIpPoolShouldReturnCorrectResult() throws Exception {
		
		EC2Method listIpMethodStub = mock(EC2Method.class);
        when(listIpMethodStub.invoke()).thenReturn(
        		resource("org/dasein/cloud/aws/network/ipaddress/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(listIpMethodStub);
		
		assertReflectionEquals(
				Arrays.asList(
						createIpAddress("198.51.100.2", null, AddressType.PUBLIC, null, IPVersion.IPV4, "198.51.100.2", null, false)), 
				elasticIP.listPublicIpPool(true));
	}
	
	@Test
	public void listAllPrivateIpPoolShouldReturnCorrectResult() throws InternalException, CloudException {
		assertEquals(Collections.emptyList(), elasticIP.listPrivateIpPool(false));
	}
	
	@Test
	public void listIPV6IpPoolShouldReturnCorrectResult() throws InternalException, CloudException {
		assertEquals(Collections.emptyList(), elasticIP.listIpPool(IPVersion.IPV6, false));
	}
	
	@Test
	public void listUnassignedPrivateIpPoolShouldReturnCorrectResult() throws InternalException, CloudException {
		assertEquals(Collections.emptyList(), elasticIP.listPrivateIpPool(true));
	}
	
	@Test
	public void listIpPoolStatusShouldReturnCorrectResult() throws Exception {
		
		EC2Method listIpMethodStub = mock(EC2Method.class);
        when(listIpMethodStub.invoke()).thenReturn(
        		resource("org/dasein/cloud/aws/network/ipaddress/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(listIpMethodStub);

		assertReflectionEquals(
				Arrays.asList(
						new ResourceStatus("eipalloc-08229861", false),
						new ResourceStatus("46.51.219.64", false),
						new ResourceStatus("198.51.100.2", true)), 
				elasticIP.listIpPoolStatus(IPVersion.IPV4));
	}
	
	@Test
	public void listIPV6IpPoolStatusShouldReturnCorrectResult() throws InternalException, CloudException {
		assertEquals(Collections.emptyList(), elasticIP.listIpPoolStatus(IPVersion.IPV6));
	}
	
	@Test
	public void listRulesShouldReturnCorrectResult() throws InternalException, CloudException {
		assertEquals(Collections.emptyList(), elasticIP.listRules(null));
	}
	
	@Test
	public void assignShouldPostWithCorrectRequestForRunningVm() throws Exception {
		elasticIP.assign("198.51.100.2", ASSIGN_INSTANCE_ID);
	}
	
	@Test
	public void assignShouldPostWithCorrectRequestForStoppedVm() throws Exception {
		elasticIP.assign("198.51.100.2", ASSIGN_INSTANCE_ID);
	}
	
	@Test
	public void assignShouldPostWithCorrectRequestForPausedVm() throws Exception {
		elasticIP.assign("198.51.100.2", ASSIGN_INSTANCE_ID);
	}
	
	@Test
	public void assignShouldPostWithCorrectRequestForSuspendedVm() throws Exception {
		elasticIP.assign("198.51.100.2", ASSIGN_INSTANCE_ID);
	}
	
	@Test(expected = CloudException.class)
	public void assignShouldThrowExceptionWithFalseResult() throws Exception {
		
		String addressId = "198.51.100.2";
		String instanceId = "i-2574e22a";
		
		EC2Method describeAddressesMethodStub = mock(EC2Method.class);
        when(describeAddressesMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeAddresses"))))
	        .thenReturn(describeAddressesMethodStub);
        
        VirtualMachine virtualMachineStub = PowerMockito.mock(VirtualMachine.class);
		EC2ComputeServices computeServicesStub = PowerMockito.mock(EC2ComputeServices.class);
		EC2Instance vmSupportStub = PowerMockito.mock(EC2Instance.class);
		PowerMockito.doReturn(computeServicesStub).when(awsCloudStub).getComputeServices();
		PowerMockito.doReturn(vmSupportStub).when(computeServicesStub).getVirtualMachineSupport();
		PowerMockito.doReturn(virtualMachineStub).when(vmSupportStub).getVirtualMachine(instanceId);
		PowerMockito.doReturn(VmState.RUNNING).when(virtualMachineStub).getCurrentState();
        
        EC2Method associateAddressMethodStub = mock(EC2Method.class);
        when(associateAddressMethodStub.invoke())
    		.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/associate_address_failed.xml"));
        PowerMockito.whenNew(EC2Method.class)
			.withArguments(eq(awsCloudStub), argThat(allOf(
					hasEntry("InstanceId", instanceId), 
					hasEntry("Action", "AssociateAddress"))))
			.thenReturn(associateAddressMethodStub);
		
		elasticIP.assign(addressId, instanceId);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void assignForTerminatedVMShouldThrowException() throws Exception {
		
		String addressId = "198.51.100.2";
		String instanceId = "i-2574e22a";
		
		VirtualMachine virtualMachineStub = PowerMockito.mock(VirtualMachine.class);
		EC2ComputeServices computeServicesStub = PowerMockito.mock(EC2ComputeServices.class);
		EC2Instance vmSupportStub = PowerMockito.mock(EC2Instance.class);
		
		PowerMockito.doReturn(computeServicesStub).when(awsCloudStub).getComputeServices();
		PowerMockito.doReturn(vmSupportStub).when(computeServicesStub).getVirtualMachineSupport();
		PowerMockito.doReturn(virtualMachineStub).when(vmSupportStub).getVirtualMachine(instanceId);
		PowerMockito.doReturn(VmState.TERMINATED).when(virtualMachineStub).getCurrentState();
        
		elasticIP.assign(addressId, instanceId);
	}
	
	@Test
	public void assignToNetworkInterfaceShouldPostWithCorrectRequest() throws Exception {
		
		String allocationId = "eipalloc-08229861";
		String nicId = "eni-attach-1a2b3c4d";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/associate_address_success.xml"));
        PowerMockito.whenNew(EC2Method.class)
        	.withArguments(eq(awsCloudStub), argThat(allOf(
        			hasEntry("AllocationId", allocationId),
        			hasEntry("NetworkInterfaceId", nicId),
        			hasEntry("Action", "AssociateAddress"))))
        	.thenReturn(ec2MethodStub);
		
		elasticIP.assignToNetworkInterface(allocationId, nicId);
	}
	
	@Test(expected = CloudException.class)
	public void assignToNetworkInterfaceShouldThrowExceptionWithFalseResult() throws Exception {
		
		String allocationId = "eipalloc-08229861";
		String nicId = "eni-attach-1a2b3c4d";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/associate_address_failed.xml"));
        PowerMockito.whenNew(EC2Method.class)
        	.withArguments(eq(awsCloudStub), argThat(allOf(
        			hasEntry("AllocationId", allocationId),
        			hasEntry("NetworkInterfaceId", nicId),
        			hasEntry("Action", "AssociateAddress"))))
        	.thenReturn(ec2MethodStub);
		
		elasticIP.assignToNetworkInterface(allocationId, nicId);
	}
	
	@Test(expected = OperationNotSupportedException.class)
	public void forwardShouldPostWithCorrectRequest() throws InternalException, CloudException {
		elasticIP.forward(null, 0, null, 0, null);
	}
	
	@Test
	public void getCapabilitiesShouldReturnCorrectResult() throws CloudException, InternalException {
		assertTrue(elasticIP.getCapabilities() instanceof ElasticIPAddressCapabilities);
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResult() throws CloudException, InternalException {
		assertTrue(elasticIP.isSubscribed());
	}
	
	@Test
	public void releaseByPublicIPFromServerShouldPostWithCorrectRequest() throws Exception {
		
		String publicIP = "198.51.100.2";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/disassociate_address_success.xml"));
        
        PowerMockito.whenNew(EC2Method.class)
        	.withArguments(eq(awsCloudStub), argThat(allOf(
        			hasEntry("PublicIp", publicIP), 
        			hasEntry("Action", "DisassociateAddress"))))
        	.thenReturn(ec2MethodStub);
        
		elasticIP.releaseFromServer(publicIP);
	}
	
	@Test
	public void releaseByAllocationIdFromServerShouldPostWithCorrectRequest() throws Exception {
		
		String allocationId = "eipalloc-08229861";
		String associationId = "eipassoc-f0229899";
		
		EC2Method describeAddressesMethodStub = mock(EC2Method.class);
        when(describeAddressesMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "DescribeAddresses"))))
            .thenReturn(describeAddressesMethodStub);
        
        EC2Method disassociatedAddressMethodStub = mock(EC2Method.class);
        when(disassociatedAddressMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/disassociate_address_success.xml"));
        PowerMockito.whenNew(EC2Method.class)
        	.withArguments(eq(awsCloudStub), argThat(allOf(
        			hasEntry("AssociationId", associationId),
        			hasEntry("Action", "DisassociateAddress"))))
        	.thenReturn(disassociatedAddressMethodStub);
        
		elasticIP.releaseFromServer(allocationId);
	}
	
	@Test(expected = CloudException.class)
	public void releaseFromServerShouldThrowExceptionIfNotAssociatedWithAnyServer() throws Exception {
		
		String allocationId = "eipalloc-08229861";
		
		EC2Method describeAddressesMethodStub = mock(EC2Method.class);
        when(describeAddressesMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/describe_address_without_association.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "DescribeAddresses"))))
            .thenReturn(describeAddressesMethodStub);
        
		elasticIP.releaseFromServer(allocationId);
	}
	
	@Test(expected = CloudException.class)
	public void releaseFromServerShouldThrowExceptionWithFalseResult() throws Exception {
		
		String publicIP = "198.51.100.2";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/disassociate_address_failed.xml"));
        
        PowerMockito.whenNew(EC2Method.class)
        	.withArguments(eq(awsCloudStub), argThat(allOf(
        			hasEntry("PublicIp", publicIP), 
        			hasEntry("Action", "DisassociateAddress"))))
        	.thenReturn(ec2MethodStub);
        
		elasticIP.releaseFromServer(publicIP);
	}
	
	@Test
	public void releaseByPublicIPFromPoolShouldPostWithCorrectRequest() throws Exception {
		
		String publicIP = "198.51.100.2";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/release_address_success.xml"));
        PowerMockito.whenNew(EC2Method.class)
        	.withArguments(eq(awsCloudStub), argThat(allOf(
        		hasEntry("PublicIp", publicIP),
        		hasEntry("Action", "ReleaseAddress"))))
        	.thenReturn(ec2MethodStub);
		
		elasticIP.releaseFromPool(publicIP);
	}
	
	@Test
	public void releaseByAllocationIdFromPoolShouldPostWithCorrectRequest() throws EC2Exception, CloudException, InternalException, Exception {
		
		String allocationId = "eipalloc-08229861";
		
		EC2Method describeAddressesMethodStub = mock(EC2Method.class);
        when(describeAddressesMethodStub.invoke()).thenReturn(
        		resource("org/dasein/cloud/aws/network/ipaddress/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeAddresses"))))
	        .thenReturn(describeAddressesMethodStub);
        
        EC2Method releaseAddressMethodStub = mock(EC2Method.class);
        when(releaseAddressMethodStub.invoke()).thenReturn(
        		resource("org/dasein/cloud/aws/network/ipaddress/release_address_success.xml"));
        PowerMockito.whenNew(EC2Method.class)
        	.withArguments(eq(awsCloudStub), argThat(allOf(
        			hasEntry("AllocationId", allocationId),
        			hasEntry("Action", "ReleaseAddress"))))
        	.thenReturn(releaseAddressMethodStub);
        
		elasticIP.releaseFromPool(allocationId);
	}
	
	@Test(expected = CloudException.class)
	public void releaseFromPoolShouldThrowExceptionWithFalseResult() throws Exception {
		
		String publicIP = "198.51.100.2";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/release_address_failed.xml"));
        PowerMockito.whenNew(EC2Method.class)
        	.withArguments(eq(awsCloudStub), argThat(allOf(
        		hasEntry("PublicIp", publicIP),
        		hasEntry("Action", "ReleaseAddress"))))
        	.thenReturn(ec2MethodStub);
		
		elasticIP.releaseFromPool(publicIP);
	}
	
	@Test
	public void requestShouldPostWithCorrectRequest() throws Exception {
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/allocate_address_standard.xml"));
        PowerMockito.whenNew(EC2Method.class)
        	.withArguments(eq(awsCloudStub), argThat(allOf(
        		hasEntry("Action", "AllocateAddress"))))
        	.thenReturn(ec2MethodStub);
		
		assertEquals("192.0.2.1", elasticIP.request(AddressType.PUBLIC));
	}
	
	@Test(expected = OperationNotSupportedException.class)
	public void requestForNonPublicAddressShouldThrowException() throws InternalException, CloudException {
		elasticIP.request(AddressType.PRIVATE);
	}
	
	@Test(expected = OperationNotSupportedException.class)
	public void requestForIPV6AddressShouldThrowException() throws InternalException, CloudException {
		elasticIP.request(IPVersion.IPV6);
	}
	
	@Test
	public void requestForVLANShoulPostWithCorrectRequest() throws Exception {
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/allocate_address_vpc.xml"));
        PowerMockito.whenNew(EC2Method.class)
        	.withArguments(eq(awsCloudStub), argThat(allOf(
        		hasEntry("Action", "AllocateAddress"))))
        	.thenReturn(ec2MethodStub);
		
		assertEquals("eipalloc-5723d13e", elasticIP.requestForVLAN(IPVersion.IPV4));
	}
	
	@Test(expected = OperationNotSupportedException.class)
	public void requestIPV6AddressForVLANShouldThrowException() throws InternalException, CloudException {
		elasticIP.requestForVLAN(IPVersion.IPV6);
	}
	
	@Test(expected = OperationNotSupportedException.class)
	public void requestForVLANBySpecificVlanIdShouldThrowException() throws CloudException, InternalException {
		elasticIP.requestForVLAN(IPVersion.IPV4, null);
	}
	
	@Test(expected = OperationNotSupportedException.class)
	public void stopForwardShouldThrowException() throws InternalException, CloudException {
		elasticIP.stopForward(null);
	}
	
	private IpAddress createIpAddress(String ipAddressId, String associationId, AddressType addressType, 
			String nicId, IPVersion version, String address, String serverId, boolean isForVlan) {
		IpAddress ipAddress = new IpAddress();
		ipAddress.setIpAddressId(ipAddressId);
		ipAddress.setProviderAssociationId(associationId);
		ipAddress.setAddressType(addressType);
		ipAddress.setProviderNetworkInterfaceId(nicId);
		ipAddress.setVersion(version);
		ipAddress.setAddress(address);
		ipAddress.setRegionId(REGION);
		ipAddress.setServerId(serverId);
		ipAddress.setForVlan(isForVlan);
		return ipAddress;
	}
	
}
