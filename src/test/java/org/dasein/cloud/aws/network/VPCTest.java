package org.dasein.cloud.aws.network;

import static org.unitils.reflectionassert.ReflectionAssert.*;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.*;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

























import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Tag;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AwsTestBase;
import org.dasein.cloud.aws.compute.EC2Exception;
import org.dasein.cloud.aws.compute.EC2Method;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.NICCreateOptions;
import org.dasein.cloud.network.NICState;
import org.dasein.cloud.network.NetworkInterface;
import org.dasein.cloud.network.RawAddress;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.SubnetCreateOptions;
import org.dasein.cloud.network.SubnetState;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANCapabilities;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.network.VlanCreateOptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSCloud.class, VPC.class})
public class VPCTest extends AwsTestBase {

	private VPC vpc;
	
	@Before
	public void setUp() {
		super.setUp();
		vpc = new VPC(awsCloudStub);
	}
	
	@Test
	public void assignRoutingTableToSubnetShouldPostWithCorrectRequest() throws Exception {
		
		String subnetId = "subnet-15ad487c";
		String routingTableId = "rtb-e4ad488d";
		
		EC2Method associateRouteTableMethodStub = mock(EC2Method.class);
        when(associateRouteTableMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/associate_route_table.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("SubnetId", subnetId),
            		hasEntry("RouteTableId", routingTableId),
            		hasEntry("Action", "AssociateRouteTable"))))
            .thenReturn(associateRouteTableMethodStub);
		
		vpc.assignRoutingTableToSubnet(subnetId, routingTableId);
	}
	
	@Test
	public void disassociateRoutingTableFromSubnetShouldPostWithCorrectRequest() throws Exception {
		
		String subnetId = "subnet-15ad487c";
		String routingTableId = "rtb-e4ad488d";
		String associationId = "rtbassoc-faad4893";
		
		EC2Method describeRouteTableMethodStub = mock(EC2Method.class);
        when(describeRouteTableMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_route_table.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "association.route-table-id"),
            		hasEntry("Filter.1.Value.1", routingTableId),
            		hasEntry("Filter.2.Name", "association.subnet-id"),
            		hasEntry("Filter.2.Value.1", subnetId),
            		hasEntry("Action", "DescribeRouteTables"))))
            .thenReturn(describeRouteTableMethodStub);
		
        EC2Method disassociateRouteTableMethodStub = mock(EC2Method.class);
        when(disassociateRouteTableMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/disassociate_route_table.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("AssociationId", associationId),
            		hasEntry("Action", "DisassociateRouteTable"))))
            .thenReturn(disassociateRouteTableMethodStub);
        
		vpc.disassociateRoutingTableFromSubnet(subnetId, routingTableId);
	}
	
	@Test
	public void assignRoutingTableToVlanShouldPostWithCorrectRequest() throws Exception {
		
		String vlanId = "vpc-11ad4878";
		String routingTableId = "rtb-13ad487a";
		String associationId = "rtbassoc-12ad487b";
		
		EC2Method describeMainRouteTableMethodStub = mock(EC2Method.class);
        when(describeMainRouteTableMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_main_route_table.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "association.main"),
            		hasEntry("Filter.1.Value.1", "true"),
            		hasEntry("Filter.2.Name", "vpc-id"),
            		hasEntry("Filter.2.Value.1", vlanId),
            		hasEntry("Action", "DescribeRouteTables"))))
            .thenReturn(describeMainRouteTableMethodStub);
		
		EC2Method replaceRouteTableAssociationMethodStub = mock(EC2Method.class);
        when(replaceRouteTableAssociationMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/replace_route_table_association.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("AssociationId", associationId),
            		hasEntry("RouteTableId", routingTableId),
            		hasEntry("Action", "ReplaceRouteTableAssociation"))))
            .thenReturn(replaceRouteTableAssociationMethodStub);
		
		vpc.assignRoutingTableToVlan(vlanId, routingTableId);
	}
	
	@Test
	public void attachNetworkInterfaceShouldPostWithCorrectRequest() throws Exception {
		
		String nicId = "eni-0f62d866";
		String vmId = "i-22197876";
		int index = 2;
		
		EC2Method describeNetworkInterfacesMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfacesMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_vm_network_interfaces.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "attachment.instance-id"),
            		hasEntry("Filter.1.Value.1", vmId),
            		hasEntry("Action", "DescribeNetworkInterfaces"))))
            .thenReturn(describeNetworkInterfacesMethodStub);
		
		EC2Method attachNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(attachNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/attach_network_interface.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("NetworkInterfaceId", nicId),
            		hasEntry("InstanceId", vmId),
            		hasEntry("DeviceIndex", String.valueOf(index)),
            		hasEntry("Action", "AttachNetworkInterface"))))
            .thenReturn(attachNetworkInterfaceMethodStub);
		
		vpc.attachNetworkInterface(nicId, vmId, index);
	}
	
	@Test
	public void createInternetGatewayShouldPostWithCorrectRequest() throws Exception {
		
		String internetGatewayId = "igw-eaad4883";
		String vlanId = "vpc-11ad4878";
		
		EC2Method attachNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(attachNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/create_internet_gateway.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "CreateInternetGateway"))))
            .thenReturn(attachNetworkInterfaceMethodStub);
        
        EC2Method attachInternetGateway = mock(EC2Method.class);
        when(attachInternetGateway.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/attach_internet_gateway.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("InternetGatewayId", internetGatewayId),
            		hasEntry("VpcId", vlanId),
            		hasEntry("Action", "AttachInternetGateway"))))
            .thenReturn(attachInternetGateway);
		
		assertEquals(
				internetGatewayId,
				vpc.createInternetGateway(vlanId));
	}
	
	@Test
	public void createRoutingTableShouldPostWithCorrectRequest() throws Exception {
		
		String vlanId = "vpc-11ad4878";
		String name = vlanId;
		String description = name;
		String routeTableId = "rtb-f9ad4890";
		
		EC2Method createRouteTableMethodStub = mock(EC2Method.class);
        when(createRouteTableMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/create_route_table.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("VpcId", vlanId),
            		hasEntry("Action", "CreateRouteTable"))))
            .thenReturn(createRouteTableMethodStub);
        
        PowerMockito.doReturn(true).when(awsCloudStub).createTags(
        		EC2Method.SERVICE_ID, 
        		routeTableId, 
        		new Tag[]{new Tag("Name", name), new Tag("Description", description)});
		
		assertEquals(
				routeTableId,
				vpc.createRoutingTable(vlanId, name, description));
	}
	
	@Test
	public void createNetworkInterfaceWithAutoAddressAndSecurityGroupShouldPostWithCorrectRequest() throws Exception {
	
		//TODO find bug for adding Security groups (SecurityGroupId.N) as parameter
		
		String subnetId = "subnet-b2a249da";
		String name = subnetId;
		String description = subnetId;
		String nicId = "eni-cfca76a6";
		String privateIpAddress = "10.0.2.157";
		String firewall1Id = "sg-188d9f74";
		String firewall2Id = "sg-188d9f88";
		
		NICCreateOptions options = NICCreateOptions.getInstanceForSubnet(subnetId, name, description);
		options.withIpAddress(privateIpAddress);
		options.behindFirewalls(firewall1Id, firewall2Id);
		
		EC2Method createNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(createNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/create_network_interface.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("SubnetId", subnetId),
            		hasEntry("Description", description),
            		hasEntry("PrivateIpAddress", privateIpAddress),
            		hasEntry("SecurityGroupId.1", firewall1Id),
            		hasEntry("SecurityGroupId.2", firewall2Id),
            		hasEntry("Action", "CreateNetworkInterface"))))
            .thenReturn(createNetworkInterfaceMethodStub);
        
        PowerMockito.doReturn(true).when(awsCloudStub).createTags(
        		EC2Method.SERVICE_ID, 
        		nicId,
        		new Tag[]{new Tag("Name", name), new Tag("Description", description)});
		
        assertReflectionEquals(
        		this.createNetworkInterface(nicId, subnetId, "vpc-c31dafaa", "ap-southeast-1b", name, description, privateIpAddress, NICState.AVAILABLE, "02:74:b0:72:79:61", null, null),
        		vpc.createNetworkInterface(options));
	}
	
	@Test(expected = OperationNotSupportedException.class)
	public void addRouteToAddressShouldPostWithCorrectRequest() throws CloudException, InternalException {
		vpc.addRouteToAddress(null, null, null, null);
	}
	
	@Test
	public void addRouteToGatewayShouldPostWithCorrectRequest() throws Exception {
		
		String routingTableId = "rtb-e4ad488d";
		String destinationCidr = "0.0.0.0/0";
		String gatewayId = "igw-eaad4883";
		
		EC2Method createRouteMethodStub = mock(EC2Method.class);
        when(createRouteMethodStub.invoke()).thenReturn(null);
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("GatewayId", gatewayId),
            		hasEntry("RouteTableId", routingTableId),
            		hasEntry("DestinationCidrBlock", destinationCidr),
            		hasEntry("Action", "CreateRoute"))))
            .thenReturn(createRouteMethodStub);
		
		vpc.addRouteToGateway(routingTableId, IPVersion.IPV4, destinationCidr, gatewayId);
	}
	
	@Test
	public void addRouteToNetworkInterfaceShouldPostWithCorrectRequest() throws Exception {
		
		String routingTableId = "rtb-e4ad488d";
		String destinationCidr = "0.0.0.0/0";
		String nicId = "eni-cfca76a6";
		
		EC2Method createRouteMethodStub = mock(EC2Method.class);
        when(createRouteMethodStub.invoke()).thenReturn(null);
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("NetworkInterfaceId", nicId),
            		hasEntry("RouteTableId", routingTableId),
            		hasEntry("DestinationCidrBlock", destinationCidr),
            		hasEntry("Action", "CreateRoute"))))
            .thenReturn(createRouteMethodStub);
		
		vpc.addRouteToNetworkInterface(routingTableId, IPVersion.IPV4, destinationCidr, nicId);
	}
	
	@Test
	public void addRouteToVirtualMachineShouldPostWithCorrectRequest() throws Exception {
		
		String routingTableId = "rtb-e4ad488d";
		String destinationCidr = "0.0.0.0/0";
		String vmId = "i-1a2b3c4d";
		
		EC2Method createRouteMethodStub = mock(EC2Method.class);
        when(createRouteMethodStub.invoke()).thenReturn(null);
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("InstanceId", vmId),
            		hasEntry("RouteTableId", routingTableId),
            		hasEntry("DestinationCidrBlock", destinationCidr),
            		hasEntry("Action", "CreateRoute"))))
            .thenReturn(createRouteMethodStub);
		
		vpc.addRouteToVirtualMachine(routingTableId, IPVersion.IPV4, destinationCidr, vmId);
	}
	
	@Test
	public void createSubnetShouldPostWithCorrectRequest() throws Exception {
		
		String inVlanId = "vpc-1a2b3c4d";
		String inDcId = "us-east-1a";
		String cidr = "10.0.1.0/24";
		String subnetId = "subnet-9d4a7b6c";
		String name = subnetId;
		String description = name;
		
		SubnetCreateOptions options = SubnetCreateOptions.getInstance(inVlanId, inDcId, cidr, name, description);
		
		EC2Method createRouteMethodStub = mock(EC2Method.class);
        when(createRouteMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/create_subnet.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("CidrBlock", cidr),
            		hasEntry("VpcId", inVlanId),
            		hasEntry("AvailabilityZone", inDcId),
            		hasEntry("Action", "CreateSubnet"))))
            .thenReturn(createRouteMethodStub);
		
        PowerMockito.doReturn(true).when(awsCloudStub).createTags(
        		EC2Method.SERVICE_ID, 
        		subnetId,
        		new Tag[]{new Tag("Name", name), new Tag("Description", description)});
        
		assertReflectionEquals(
				createSubnet(subnetId, inVlanId, SubnetState.PENDING, name, description, cidr, inDcId, 251),
				vpc.createSubnet(options));
	}
	
	@Test
	public void createVlanShouldPostWithCorrectRequest() throws Exception {
		
		//TODO BUG: toVlan set dnsServers error, only set the last one
		
		String cidr = "10.0.0.0/16";
		String vlanId = "vpc-1a2b3c4d";
		String dhcpOptionsId = "dopt-1a2b3c4d2";
		String domainName = "example.com";
		String[] dnsServers = new String[]{"10.2.5.1", "10.2.5.2"};
		String name = vlanId;
		String description = name;
		
		VlanCreateOptions options = VlanCreateOptions.getInstance(name, description, cidr, domainName, dnsServers, null);
		
		EC2Method createRouteMethodStub = mock(EC2Method.class);
        when(createRouteMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/create_vlan.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("CidrBlock", cidr),
            		hasEntry("InstanceTenancy", "default"),
            		hasEntry("Action", "CreateVpc"))))
            .thenReturn(createRouteMethodStub);
        
        EC2Method describeDhcpOptionsMethodStub = mock(EC2Method.class);
        when(describeDhcpOptionsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_dhcp_options.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("DhcpOptionsId.1", dhcpOptionsId),
            		hasEntry("Action", "DescribeDhcpOptions"))))
            .thenReturn(describeDhcpOptionsMethodStub);
        
        EC2Method createDhcpOptionsMethodStub = mock(EC2Method.class);
        when(createDhcpOptionsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/create_dhcp_options.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("DhcpConfiguration.1.Key", "domain-name"),
            		hasEntry("DhcpConfiguration.1.Value.1", domainName),
            		hasEntry("DhcpConfiguration.2.Key", "domain-name-servers"),
            		hasEntry("DhcpConfiguration.2.Value.1", dnsServers[0]),
            		hasEntry("DhcpConfiguration.2.Value.2", dnsServers[1]),
            		hasEntry("Action", "CreateDhcpOptions"))))
            .thenReturn(createDhcpOptionsMethodStub);
        
        EC2Method associateDhcpOptionMethodStub = mock(EC2Method.class);
        when(associateDhcpOptionMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/associate_dhcp_options.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("DhcpOptionsId", dhcpOptionsId),
            		hasEntry("VpcId", vlanId),
            		hasEntry("Action", "AssociateDhcpOptions"))))
            .thenReturn(associateDhcpOptionMethodStub);
		
        PowerMockito.doReturn(true).when(awsCloudStub).createTags(
        		EC2Method.SERVICE_ID, 
        		vlanId,
        		new Tag[]{new Tag("Name", name), new Tag("Description", description)});
		
		assertReflectionEquals(
				createVLAN(vlanId, name, description, VLANState.PENDING, cidr, domainName, dnsServers, null),
				vpc.createVlan(options));
	}
	
	@Test
	public void detachNetworkInterfaceShouldPostWithCorrectRequest() throws Exception {
		
		String nicId = "eni-0f62d866";
		String attachmentId = "eni-attach-6537fc0c";
		
		EC2Method describeNetworkInterfacesMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfacesMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_network_interface.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("NetworkInterfaceId.1", nicId),
            		hasEntry("Action", "DescribeNetworkInterfaces"))))
            .thenReturn(describeNetworkInterfacesMethodStub);
		
		EC2Method detachNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(detachNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/detach_network_interface.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("AttachmentId", attachmentId),
            		hasEntry("Action", "DetachNetworkInterface"))))
            .thenReturn(detachNetworkInterfaceMethodStub);
		
		vpc.detachNetworkInterface(nicId);
	}
	
	@Test
	public void getCapabilitiesShouldReturnCorrectResult() {
		assertTrue(vpc.getCapabilities() instanceof VLANCapabilities);
	}
	
	@Test
	public void getProviderTermForNetworkInterfaceShouldReturnCorrectResult() {
		assertEquals(
				vpc.getCapabilities().getProviderTermForNetworkInterface(null),
				vpc.getProviderTermForNetworkInterface(null));
	}
	
	@Test
	public void getProviderTermForSubnetShouldReturnCorrectResult() {
		assertEquals(
				vpc.getCapabilities().getProviderTermForSubnet(null),
				vpc.getProviderTermForSubnet(null));
	}
	
	@Test
	public void getProviderTermForVlanShouldReturnCorrectResult() {
		assertEquals(
				vpc.getCapabilities().getProviderTermForVlan(null),
				vpc.getProviderTermForVlan(null));
	}
	
	@Test
	public void getNetworkInterfaceShouldReturnCorrectResult() throws Exception {
		
		String nicId = "eni-0f62d866";
		
		EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_network_interface.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("NetworkInterfaceId.1", nicId),
            		hasEntry("Action", "DescribeNetworkInterfaces"))))
            .thenReturn(describeNetworkInterfaceMethodStub);

        assertReflectionEquals(
        		createNetworkInterface(nicId, "subnet-c53c87ac", "vpc-cc3c87a5", "us-west-1b", null, null, "10.0.0.146", NICState.IN_USE, "02:81:60:cb:27:37", null, "i-22197876"),
        		vpc.getNetworkInterface(nicId));
	}
	
	//TODO continue getRoutingTableForSubnet
	
	private NetworkInterface createNetworkInterface(String nicId, String subnetId, String vpcId, String zone, String name, String desc, 
			String privateIpAddress, NICState status, String macAddress, String privateDnsName, String instanceId) {
		NetworkInterface nic = new NetworkInterface();
		nic.setProviderNetworkInterfaceId(nicId);
		nic.setProviderSubnetId(subnetId);
		nic.setProviderVlanId(vpcId);
		nic.setProviderRegionId(REGION);
		nic.setProviderDataCenterId(zone);
		if (StringUtils.isEmpty(name)) {
			nic.setName(nic.getProviderNetworkInterfaceId());
		} else {
			nic.setName(name);
		}
		if (StringUtils.isEmpty(desc)) {
			nic.setDescription(nic.getName());
		} else {
			nic.setDescription(desc);
		}
		nic.setIpAddresses(new RawAddress(privateIpAddress));
		nic.setCurrentState(status);
		nic.setMacAddress(macAddress);
		nic.setDnsName(privateDnsName);
		nic.setProviderVirtualMachineId(instanceId);
		nic.setProviderOwnerId(ACCOUNT_NO);
		return nic;
	}
	
	private Subnet createSubnet(String subnetId, String vlanId, SubnetState currentState, String name, String description, String cidr, String dataCenterId, int availableIpAddresses) {
		Subnet subnet = Subnet.getInstance(ACCOUNT_NO, REGION, vlanId, subnetId, currentState, name, description, cidr);
		subnet.withAvailableIpAddresses(availableIpAddresses);
		subnet.supportingTraffic(null);								//TODO check toSubnet not set the IPVersion
		subnet.constrainedToDataCenter(dataCenterId);
		subnet.setTags(new HashMap<String, String>());
		return subnet;
	}
	
	private VLAN createVLAN(String vlanId, String name, String description, VLANState currentState, String cidr,
			String domainName, String[] dnsServers, String[] ntpServers) {
		VLAN vlan = new VLAN();
		vlan.setProviderVlanId(vlanId);
		vlan.setCurrentState(currentState);
		vlan.setCidr(cidr);
		vlan.setName(name);
		vlan.setDescription(description);
		vlan.setDomainName(domainName);
		vlan.setDnsServers(dnsServers);
		vlan.setNtpServers(ntpServers);
		vlan.setTags(new HashMap<String, String>());
		vlan.setSupportedTraffic(IPVersion.IPV4);
		vlan.setProviderOwnerId(ACCOUNT_NO);
		vlan.setProviderRegionId(REGION);
		return vlan;
	}
}
