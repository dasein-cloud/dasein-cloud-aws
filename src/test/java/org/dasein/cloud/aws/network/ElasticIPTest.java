package org.dasein.cloud.aws.network;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.unitils.reflectionassert.ReflectionAssert.*;
import java.util.Collections;
import java.util.Iterator;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSCloud.class, EC2Instance.class, ElasticIP.class})
public class ElasticIPTest extends AwsTestBase {

	private ElasticIP elasticIP;
	
	@Before
	public void setUp() {
		super.setUp();
		elasticIP = new ElasticIP(awsCloudStub);
	}
	
	@Test
	public void getIpAddressShouldReturnVpcAddress() throws Exception {
		
		EC2Method listIpMethodStub = mock(EC2Method.class);
        when(listIpMethodStub.invoke()).thenReturn(
        		resource("org/dasein/cloud/aws/network/ipaddress/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(listIpMethodStub);
		
        IpAddress expectedResult = new IpAddress();
        expectedResult.setIpAddressId("eipalloc-08229861");
        expectedResult.setProviderAssociationId("eipassoc-f0229899");
        expectedResult.setAddressType(AddressType.PUBLIC);
        expectedResult.setProviderNetworkInterfaceId("eni-ef229886");
        expectedResult.setVersion(IPVersion.IPV4);
        expectedResult.setAddress("46.51.219.63");
        expectedResult.setRegionId(REGION);
        expectedResult.setServerId("i-64600030");
        expectedResult.setForVlan(true);
        
        assertReflectionEquals(expectedResult, elasticIP.getIpAddress("eipalloc-08229861"));
	}
	
	@Test
	public void getIpAddressShouldReturnIpAddress() throws EC2Exception, CloudException, InternalException, Exception {
		
		EC2Method listIpMethodStub = mock(EC2Method.class);
        when(listIpMethodStub.invoke()).thenReturn(
        		resource("org/dasein/cloud/aws/network/ipaddress/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(listIpMethodStub);
		
        IpAddress expectedResult = new IpAddress();
        expectedResult.setIpAddressId("46.51.219.64");
        expectedResult.setProviderAssociationId("eipassoc-f0229810");
        expectedResult.setAddressType(AddressType.PUBLIC);
        expectedResult.setProviderNetworkInterfaceId("eni-ef229810");
        expectedResult.setVersion(IPVersion.IPV4);
        expectedResult.setAddress("46.51.219.64");
        expectedResult.setRegionId(REGION);
        expectedResult.setServerId("i-64600031");
        expectedResult.setForVlan(false);
        
		assertReflectionEquals(expectedResult, elasticIP.getIpAddress("46.51.219.64"));
	}
	
	@Test
	public void listAllPublicIpPoolShouldReturnCorrectResult() throws EC2Exception, CloudException, InternalException, Exception {
		
		EC2Method listIpMethodStub = mock(EC2Method.class);
        when(listIpMethodStub.invoke()).thenReturn(
        		resource("org/dasein/cloud/aws/network/ipaddress/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(listIpMethodStub);
		
		Iterable<IpAddress> ipAddresses = elasticIP.listPublicIpPool(false);
		Iterator<IpAddress> iter = ipAddresses.iterator();
		int count = 0;
		while (iter.hasNext()) {
			count++;
			IpAddress expectedResult = new IpAddress();
			if (count == 1) {
				expectedResult.setIpAddressId("eipalloc-08229861");
		        expectedResult.setProviderAssociationId("eipassoc-f0229899");
		        expectedResult.setProviderNetworkInterfaceId("eni-ef229886");
		        expectedResult.setAddress("46.51.219.63");
		        expectedResult.setServerId("i-64600030");
		        expectedResult.setForVlan(true);
			} else if (count == 2) {
				expectedResult.setIpAddressId("46.51.219.64");
		        expectedResult.setProviderAssociationId("eipassoc-f0229810");
		        expectedResult.setProviderNetworkInterfaceId("eni-ef229810");
		        expectedResult.setAddress("46.51.219.64");
		        expectedResult.setServerId("i-64600031");
		        expectedResult.setForVlan(false);
			} else if (count == 3) {
				expectedResult.setIpAddressId("198.51.100.2");
				expectedResult.setAddress("198.51.100.2");
				expectedResult.setForVlan(false);
			}
			expectedResult.setAddressType(AddressType.PUBLIC);
			expectedResult.setVersion(IPVersion.IPV4);
			expectedResult.setRegionId(REGION);
			
			assertReflectionEquals(expectedResult, iter.next());
		}
		assertEquals(3, count);
	}
	
	@Test
	public void listUnassignedPublicIpPoolShouldReturnCorrectResult() throws EC2Exception, CloudException, InternalException, Exception {
		
		EC2Method listIpMethodStub = mock(EC2Method.class);
        when(listIpMethodStub.invoke()).thenReturn(
        		resource("org/dasein/cloud/aws/network/ipaddress/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(listIpMethodStub);
		
		Iterable<IpAddress> ipAddresses = elasticIP.listPublicIpPool(true);
		Iterator<IpAddress> iter = ipAddresses.iterator();
		int count = 0;
		while (iter.hasNext()) {
			count++;
			IpAddress expectedResult = new IpAddress();
			if (count == 1) {
				expectedResult.setIpAddressId("198.51.100.2");
				expectedResult.setAddress("198.51.100.2");
				expectedResult.setForVlan(false);
			} 
			expectedResult.setAddressType(AddressType.PUBLIC);
			expectedResult.setVersion(IPVersion.IPV4);
			expectedResult.setRegionId(REGION);
			
			assertReflectionEquals(expectedResult, iter.next());
		}
		assertEquals(1, count);
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
		
		Iterable<ResourceStatus> resourceStatuses = elasticIP.listIpPoolStatus(IPVersion.IPV4);
		Iterator<ResourceStatus> iter = resourceStatuses.iterator();
		int count = 0;
		while (iter.hasNext()) {
			count++;
			ResourceStatus expectedResult = null;
			if (count == 1) {
				expectedResult =  new ResourceStatus("eipalloc-08229861", false);
			} else if (count == 2) {
				expectedResult =  new ResourceStatus("46.51.219.64", false);
			} else if (count == 3) {
				expectedResult =  new ResourceStatus("198.51.100.2", true);
			}
			assertReflectionEquals(expectedResult, iter.next());
		}
		assertEquals(3, count);
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
	public void assignShouldPostWithCorrectRequest() throws Exception {
		
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
        
        EC2Method associateAddressSuccessMethodStub = mock(EC2Method.class);
        when(associateAddressSuccessMethodStub.invoke())
	    	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/associate_address_success.xml"));
        PowerMockito.whenNew(EC2Method.class)
			.withArguments(eq(awsCloudStub), argThat(allOf(
					hasEntry("InstanceId", instanceId), 
					hasEntry("Action", "AssociateAddress"))))
			.thenReturn(associateAddressSuccessMethodStub);
		
		elasticIP.assign(addressId, instanceId);
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
	
}
