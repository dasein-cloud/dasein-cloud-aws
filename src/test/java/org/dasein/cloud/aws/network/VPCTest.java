package org.dasein.cloud.aws.network;

import static org.unitils.reflectionassert.ReflectionAssert.*;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.*;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AwsTestBase;
import org.dasein.cloud.aws.compute.EC2ComputeServices;
import org.dasein.cloud.aws.compute.EC2Exception;
import org.dasein.cloud.aws.compute.EC2Instance;
import org.dasein.cloud.aws.compute.EC2Method;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.network.AddressType;
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.InternetGateway;
import org.dasein.cloud.network.InternetGatewayAttachmentState;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.NICCreateOptions;
import org.dasein.cloud.network.NICState;
import org.dasein.cloud.network.NetworkInterface;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Permission;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.network.RawAddress;
import org.dasein.cloud.network.Route;
import org.dasein.cloud.network.RoutingTable;
import org.dasein.cloud.network.RuleTarget;
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
@PrepareForTest({AWSCloud.class, VPC.class, ElasticIP.class, SecurityGroup.class})
public class VPCTest extends AwsTestBase {

	private VPC vpc;
	
	@Before
	public void setUp() throws Exception {
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
	
	@Test
	public void getRoutingTableForSubnetShouldReturnCorrectResult() throws Exception {
		
		String subnetId = "subnet-15ad487c";
		
		EC2Method describeRouteTablesMethodStub = mock(EC2Method.class);
        when(describeRouteTablesMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_route_table.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "association.subnet-id"),
            		hasEntry("Filter.1.Value.1", subnetId),
            		hasEntry("Action", "DescribeRouteTables"))))
            .thenReturn(describeRouteTablesMethodStub);
		
        assertReflectionEquals(
        		createRoutingTable("rtb-e4ad488d", "rtb-e4ad488d", "rtb-e4ad488d", "vpc-11ad4878", false, 
        			new String[] {subnetId},
        			new Route[] {
        				createRoute("10.0.0.0/22", "local", null, null, null),
        				createRoute("0.0.0.0/0", "igw-eaad4883", null, null, null)
        			}
        		),
        		vpc.getRoutingTableForSubnet(subnetId));		
	}
	
	@Test
	public void getRoutingTableSupportShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals(
				vpc.getCapabilities().getRoutingTableSupport(),
				vpc.getRoutingTableSupport());
	}
	
	@Test
	public void getRoutingTableForVlanShouldReturnCorrectResult() throws Exception {
		
		String vlanId = "vpc-11ad4878";
		
		EC2Method describeRouteTablesMethodStub = mock(EC2Method.class);
        when(describeRouteTablesMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_route_table.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "association.main"),
            		hasEntry("Filter.1.Value.1", "true"),
            		hasEntry("Filter.2.Name", "vpc-id"),
            		hasEntry("Filter.2.Value.1", vlanId),
            		hasEntry("Action", "DescribeRouteTables"))))
            .thenReturn(describeRouteTablesMethodStub);
        
        assertReflectionEquals(
        		createRoutingTable("rtb-e4ad488d", "rtb-e4ad488d", "rtb-e4ad488d", vlanId, false, 
        			new String[] {"subnet-15ad487c"},
        			new Route[] {
        				createRoute("10.0.0.0/22", "local", null, null, null),
        				createRoute("0.0.0.0/0", "igw-eaad4883", null, null, null)
        			}
        		),
        		vpc.getRoutingTableForVlan(vlanId));
	}
	
	@Test
	public void getSubnetShouldReturnCorrectResult() throws Exception {
		
		String providerSubnetId = "subnet-9d4a7b6c";
		
		EC2Method describeSubnetMethodStub = mock(EC2Method.class);
        when(describeSubnetMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_subnet.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("SubnetId.1", providerSubnetId),
            		hasEntry("Action", "DescribeSubnets"))))
            .thenReturn(describeSubnetMethodStub);
		
		assertReflectionEquals (
				createSubnet(providerSubnetId, "vpc-1a2b3c4d", SubnetState.AVAILABLE, providerSubnetId, providerSubnetId, "10.0.1.0/24", "us-east-1a", 251),
				vpc.getSubnet(providerSubnetId));
	}
	
	@Test
	public void getSubnetSupportShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals(
				vpc.getCapabilities().getSubnetSupport(),
				vpc.getSubnetSupport());
	}
	
	@Test
	public void getVlanShouldReturnCorrectResult() throws Exception {
		
		String providerVlanId = "vpc-1a2b3c4d";
		String dhcpOptionsId = "dopt-7a8b9c2d";
		
		EC2Method describeVlanMethodStub = mock(EC2Method.class);
        when(describeVlanMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_vlan.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("VpcId.1", providerVlanId),
            		hasEntry("Action", "DescribeVpcs"))))
            .thenReturn(describeVlanMethodStub);
		
        EC2Method describeDhcpOptionsMethodStub = mock(EC2Method.class);
        when(describeDhcpOptionsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_dhcp_options.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("DhcpOptionsId.1", dhcpOptionsId),
            		hasEntry("Action", "DescribeDhcpOptions"))))
            .thenReturn(describeDhcpOptionsMethodStub);
        
        assertReflectionEquals(
        		createVLAN(providerVlanId, providerVlanId, providerVlanId, VLANState.AVAILABLE, "10.0.0.0/23", 
        				"example.com", new String[] {"10.2.5.1", "10.2.5.2"}, null),
        		vpc.getVlan(providerVlanId));
	}
	
	@Test
	public void identifySubnetDCRequirementShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals(
				vpc.getCapabilities().identifySubnetDCRequirement(),
				vpc.identifySubnetDCRequirement());
	}
	
	@Test
	public void isConnectedViaInternetGatewayShouldReturnTrueIfConnected() throws Exception {
		
		String vlanId = "vpc-11ad4878";

		EC2Method describeInternetGatewayMethodStub = mock(EC2Method.class);
        when(describeInternetGatewayMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_internet_gateway.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "attachment.vpc-id"),
            		hasEntry("Filter.1.Value.1", vlanId),
            		hasEntry("Action", "DescribeInternetGateways"))))
            .thenReturn(describeInternetGatewayMethodStub);
		
		assertTrue(vpc.isConnectedViaInternetGateway(vlanId));
	}
	
	@Test
	public void isConnectedViaInternetGatewayShouldReturnFalseIfNotConnected() throws Exception {
		String vlanId = "vpc-11ad4878";

		EC2Method describeInternetGatewayMethodStub = mock(EC2Method.class);
        when(describeInternetGatewayMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_internet_gateway_notconnected.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "attachment.vpc-id"),
            		hasEntry("Filter.1.Value.1", vlanId),
            		hasEntry("Action", "DescribeInternetGateways"))))
            .thenReturn(describeInternetGatewayMethodStub);
		
		assertFalse(vpc.isConnectedViaInternetGateway(vlanId));
	}
	
	@Test
	public void isNetworkInterfaceSupportEnabledShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals(
				vpc.getCapabilities().isNetworkInterfaceSupportEnabled(),
				vpc.isNetworkInterfaceSupportEnabled());
	}
	
	@Test
	public void isSubscribedShouldReturnTrue() throws Exception {
		EC2Method describeVlanMethodStub = mock(EC2Method.class);
        when(describeVlanMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_vlan.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "DescribeVpcs"))))
            .thenReturn(describeVlanMethodStub);
		assertTrue(vpc.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnFalseIfUnauthorized() throws Exception {
		EC2Method describeVlanMethodStub = mock(EC2Method.class);
		when(describeVlanMethodStub.invoke())
			.thenThrow(EC2Exception.create(HttpStatus.SC_UNAUTHORIZED));
		PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeVpcs"))))
	        .thenReturn(describeVlanMethodStub);
		assertFalse(vpc.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnFalseIfFrobidden() throws Exception {
		EC2Method describeVlanMethodStub = mock(EC2Method.class);
		when(describeVlanMethodStub.invoke())
			.thenThrow(EC2Exception.create(HttpStatus.SC_FORBIDDEN));
		PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeVpcs"))))
	        .thenReturn(describeVlanMethodStub);
		assertFalse(vpc.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnFalseIfSubscriptionCheckFailed() throws Exception {
		EC2Method describeVlanMethodStub = mock(EC2Method.class);
		when(describeVlanMethodStub.invoke())
			.thenThrow(EC2Exception.create(HttpStatus.SC_METHOD_FAILURE, null, "SubscriptionCheckFailed", "SubscriptionCheckFailed"));
		PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeVpcs"))))
	        .thenReturn(describeVlanMethodStub);
		assertFalse(vpc.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnFalseIfAuthFailure() throws Exception {
		EC2Method describeVlanMethodStub = mock(EC2Method.class);
		when(describeVlanMethodStub.invoke())
			.thenThrow(EC2Exception.create(HttpStatus.SC_METHOD_FAILURE, null, "AuthFailure", "AuthFailure"));
		PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeVpcs"))))
	        .thenReturn(describeVlanMethodStub);
		assertFalse(vpc.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnFalseIfSignatureDoesNotMatch() throws Exception {
		EC2Method describeVlanMethodStub = mock(EC2Method.class);
		when(describeVlanMethodStub.invoke())
			.thenThrow(EC2Exception.create(HttpStatus.SC_METHOD_FAILURE, null, "SignatureDoesNotMatch", "SignatureDoesNotMatch"));
		PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeVpcs"))))
	        .thenReturn(describeVlanMethodStub);
		assertFalse(vpc.isSubscribed());
	}
	
	@Test(expected=CloudException.class)
	public void isSubscribedShouldThrowException() throws Exception {
		EC2Method describeVlanMethodStub = mock(EC2Method.class);
		when(describeVlanMethodStub.invoke())
			.thenThrow(EC2Exception.create(HttpStatus.SC_METHOD_FAILURE, null, "UnknowCloudException", "UnknowCloudException"));
		PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeVpcs"))))
	        .thenReturn(describeVlanMethodStub);
		vpc.isSubscribed();
	}
	
	@Test
	public void isVlanDataCenterConstrainedShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals(
				vpc.getCapabilities().isVlanDataCenterConstrained(),
				vpc.isVlanDataCenterConstrained());
	}
	
	@Test
	public void listFirewallIdsForNICShouldReturnCorrectResult() throws Exception {
		
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
        		Arrays.asList("sg-3f4b5653"),
        		vpc.listFirewallIdsForNIC(nicId));
	}
	
	@Test
	public void listNetworkInterfaceStatusShouldReturnCorrectResult() throws Exception {
		EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_network_interfaces.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "DescribeNetworkInterfaces"))))
            .thenReturn(describeNetworkInterfaceMethodStub);
		
        assertReflectionEquals(
        		Arrays.asList(
        				new ResourceStatus("eni-0f62d866", NICState.IN_USE),
        				new ResourceStatus("eni-a66ed5cf", NICState.IN_USE)),
        		vpc.listNetworkInterfaceStatus());
	}
	
	@Test
	public void listNetworkInterfacesShouldReturnCorrectResult() throws Exception{
		EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_network_interfaces.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "DescribeNetworkInterfaces"))))
            .thenReturn(describeNetworkInterfaceMethodStub);
        
		assertReflectionEquals(
				Arrays.asList(
						createNetworkInterface("eni-0f62d866", "subnet-c53c87ac", "vpc-cc3c87a5", "us-west-1b", "eni-0f62d866", "eni-0f62d866", 
								"10.0.0.146", NICState.IN_USE, "02:81:60:cb:27:37", null, "i-22197876"),
						createNetworkInterface("eni-a66ed5cf", "subnet-cd8a35a4", "vpc-f28a359b", "us-west-1b", "eni-a66ed5cf", "Primary network interface", 
								"10.0.1.233", NICState.IN_USE, "02:78:d7:00:8a:1e", null, "i-886401dc")),
				vpc.listNetworkInterfaces());
	}
	
	@Test
	public void listNetworkInterfacesForVMShouldReturnCorrectResult() throws Exception {
		String instanceId = "i-886401dc";
		
		EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_network_interfaces_for_instance.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "attachment.instance-id"),
            		hasEntry("Filter.1.Value.1", instanceId),
            		hasEntry("Action", "DescribeNetworkInterfaces"))))
            .thenReturn(describeNetworkInterfaceMethodStub);
        
        assertReflectionEquals(
        		Arrays.asList(
        				createNetworkInterface("eni-a66ed5cf", "subnet-cd8a35a4", "vpc-f28a359b", "us-west-1b", "eni-a66ed5cf", "Primary network interface", 
								"10.0.1.233", NICState.IN_USE, "02:78:d7:00:8a:1e", null, "i-886401dc")),
				vpc.listNetworkInterfacesForVM(instanceId));
	}
	
	@Test
	public void listNetworkInterfacesInSubnetShouldReturnCorrectResult() throws Exception {
		String subnetId = "subnet-cd8a35a4";
		
		EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_network_interfaces_for_subnet.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "subnet-id"),
            		hasEntry("Filter.1.Value.1", subnetId),
            		hasEntry("Action", "DescribeNetworkInterfaces"))))
            .thenReturn(describeNetworkInterfaceMethodStub);
        
        assertReflectionEquals(
        		Arrays.asList(
        				createNetworkInterface("eni-a66ed5cf", "subnet-cd8a35a4", "vpc-f28a359b", "us-west-1b", "eni-a66ed5cf", "Primary network interface", 
								"10.0.1.233", NICState.IN_USE, "02:78:d7:00:8a:1e", null, "i-886401dc")),
				vpc.listNetworkInterfacesInSubnet(subnetId));
	}
	
	@Test
	public void listNetworkInterfacesInVLANShouldReturnCorrectResult() throws Exception {
		String vlanId = "vpc-f28a359b";
		
		EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_network_interfaces_for_vpc.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "vpc-id"),
            		hasEntry("Filter.1.Value.1", vlanId),
            		hasEntry("Action", "DescribeNetworkInterfaces"))))
            .thenReturn(describeNetworkInterfaceMethodStub);
        
        assertReflectionEquals(
        		Arrays.asList(
        				createNetworkInterface("eni-a66ed5cf", "subnet-cd8a35a4", "vpc-f28a359b", "us-west-1b", "eni-a66ed5cf", "Primary network interface", 
								"10.0.1.233", NICState.IN_USE, "02:78:d7:00:8a:1e", null, "i-886401dc")),
				vpc.listNetworkInterfacesInVLAN(vlanId));
	}
	
	@Test
	public void listRoutingTablesForSubnetShouldReturnCorrectResult() throws Exception {
		String subnetId = "subnet-15ad487c";
		
		EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_route_tables_for_subnet.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "association.subnet-id"),
            		hasEntry("Filter.1.Value.1", subnetId),
            		hasEntry("Action", "DescribeRouteTables"))))
            .thenReturn(describeNetworkInterfaceMethodStub);
        
        assertReflectionEquals(
        		Arrays.asList(
        				createRoutingTable("rtb-e4ad488d", "rtb-e4ad488d", "rtb-e4ad488d", "vpc-11ad4878", false, 
        						new String[] {"subnet-15ad487c"}, 
        						new Route[] {
        							createRoute("10.0.0.0/22", "local", null, null, null),
        							createRoute("0.0.0.0/0", "igw-eaad4883", null, null, null)
        				})),
        		vpc.listRoutingTablesForSubnet(subnetId));
	}
	
	@Test
	public void listRoutingTablesForVlanShouldReturnCorrectResult() throws Exception {
		String vlanId = "subnet-15ad487c";
		
		EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_route_tables_for_vlan.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "vpc-id"),
            		hasEntry("Filter.1.Value.1", vlanId),
            		hasEntry("Action", "DescribeRouteTables"))))
            .thenReturn(describeNetworkInterfaceMethodStub);
        
        assertReflectionEquals(
        		Arrays.asList(
        				createRoutingTable("rtb-e4ad488d", "rtb-e4ad488d", "rtb-e4ad488d", "vpc-11ad4878", false, 
        						new String[] {"subnet-15ad487c"}, 
        						new Route[] {
        							createRoute("10.0.0.0/22", "local", null, null, null),
        							createRoute("0.0.0.0/0", "igw-eaad4883", null, null, null)
        				})),
        		vpc.listRoutingTablesForVlan(vlanId));
	}
	
	@Test
	public void isSubnetDataCenterConstrainedShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals(
				vpc.getCapabilities().isSubnetDataCenterConstrained(),
				vpc.isSubnetDataCenterConstrained());
	}
	
	@Test
	public void listSubnetsShouldReturnCorrectResult() throws Exception {
		String providerVlanId = "vpc-1a2b3c4d";
		
		EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_subnets.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "vpc-id"),
            		hasEntry("Filter.1.Value.1", providerVlanId),
            		hasEntry("Action", "DescribeSubnets"))))
            .thenReturn(describeNetworkInterfaceMethodStub);
		
        assertReflectionEquals(
        		Arrays.asList(
        				createSubnet("subnet-9d4a7b6c", "vpc-1a2b3c4d", SubnetState.AVAILABLE, "subnet-9d4a7b6c", 
        						"subnet-9d4a7b6c", "10.0.1.0/24", "us-east-1a", 251),
        				createSubnet("subnet-6e7f829e", "vpc-1a2b3c4d", SubnetState.AVAILABLE, "subnet-6e7f829e", 
        						"subnet-6e7f829e", "10.0.0.0/24", "us-east-1a", 251)),
        		vpc.listSubnets(providerVlanId));
	}
	
	@Test
	public void listSupportedIPVersionsShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals(
				vpc.getCapabilities().listSupportedIPVersions(),
				vpc.listSupportedIPVersions());
	}
	
	@Test
	public void listVlanStatusShouldReturnCorrectResult() throws Exception {
		EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_vlans.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "DescribeVpcs"))))
            .thenReturn(describeNetworkInterfaceMethodStub);
        
        assertReflectionEquals(
        		Arrays.asList(
        				new ResourceStatus("vpc-1a2b3c4d", VLANState.AVAILABLE),
        				new ResourceStatus("vpc-1a2b3c88", VLANState.PENDING)),
        		vpc.listVlanStatus());
	}
	
	@Test
	public void listVlansShouldReturnCorrectResult() throws Exception {
		
		EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_vlans.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "DescribeVpcs"))))
            .thenReturn(describeNetworkInterfaceMethodStub);
        
        EC2Method describeDhcpOptionsMethodStub = mock(EC2Method.class);
        when(describeDhcpOptionsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_dhcp_options.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("DhcpOptionsId.1", "dopt-7a8b9c2d"),
            		hasEntry("Action", "DescribeDhcpOptions"))))
            .thenReturn(describeDhcpOptionsMethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("DhcpOptionsId.1", "dopt-7a8b9c66"),
	        		hasEntry("Action", "DescribeDhcpOptions"))))
	        .thenReturn(describeDhcpOptionsMethodStub);
        
        assertReflectionEquals(
        		Arrays.asList(
        				createVLAN("vpc-1a2b3c4d", "vpc-1a2b3c4d", "vpc-1a2b3c4d", VLANState.AVAILABLE, "10.0.0.0/23", 
        						"example.com", new String[] {"10.2.5.1", "10.2.5.2"}, null),
						createVLAN("vpc-1a2b3c88", "vpc-1a2b3c88", "vpc-1a2b3c88", VLANState.PENDING, "10.0.2.0/23", 
        						"example.com", new String[] {"10.2.5.1", "10.2.5.2"}, null)),
        		vpc.listVlans());
	}
	
	@Test
	public void getAttachedInternetGatewayIdShouldReturnCorrectResult() throws Exception {
		String vlanId = "vpc-11ad4878";
		
		EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_internet_gateway.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "attachment.vpc-id"),
            		hasEntry("Filter.1.Value.1", vlanId),
            		hasEntry("Action", "DescribeInternetGateways"))))
            .thenReturn(describeNetworkInterfaceMethodStub);
		
        assertEquals(
        		"igw-eaad4883EXAMPLE",
        		vpc.getAttachedInternetGatewayId(vlanId));
	}
	
	@Test
	public void getInternetGatewayByIdShouldReturnCorrectResult() throws Exception {
		String gatewayId = "igw-eaad4883EXAMPLE";
		
		EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_internet_gateway.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("InternetGatewayId.1", gatewayId),
            		hasEntry("Action", "DescribeInternetGateways"))))
            .thenReturn(describeNetworkInterfaceMethodStub);
		
        assertReflectionEquals(
        		createInternetGateway("igw-eaad4883EXAMPLE", InternetGatewayAttachmentState.AVAILABLE, "vpc-11ad4878"),
        		vpc.getInternetGatewayById(gatewayId));
	}
	
	@Test
	public void listResourcesShouldReturnCorrectResult() throws Exception {
		
		String vlanId = "vpc-11ad4878";
		
		NetworkServices networkServices = PowerMockito.mock(EC2NetworkServices.class);
		FirewallSupport firewallSupport = new SecurityGroup(awsCloudStub);
		IpAddressSupport ipAddressSupport = new ElasticIP(awsCloudStub);
		
		PowerMockito.doReturn(networkServices).when(awsCloudStub).getNetworkServices();
		PowerMockito.doReturn(firewallSupport).when(networkServices).getFirewallSupport();
		PowerMockito.doReturn(ipAddressSupport).when(networkServices).getIpAddressSupport();
		
		//list firewalls
		EC2Method describeSecurityGroupsForVlanMethodStub = mock(EC2Method.class);
        when(describeSecurityGroupsForVlanMethodStub.invoke())
        		.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_security_groups_for_vlan.xml"));
        PowerMockito.whenNew(EC2Method.class)
            	.withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "DescribeSecurityGroups"))))
            	.thenReturn(describeSecurityGroupsForVlanMethodStub);

        //list ip addresses
      	EC2Method describeIpAddressesForVlanMethodStub = mock(EC2Method.class);
        when(describeIpAddressesForVlanMethodStub.invoke())
            	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_ip_addresses_for_vlan.xml"));
        PowerMockito.whenNew(EC2Method.class)
        		.withArguments(eq(awsCloudStub), argThat(allOf(
                	hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(describeIpAddressesForVlanMethodStub);
        
        //list routing tables
        EC2Method describeRoutingTablesForVlanMethodStub = mock(EC2Method.class);
        when(describeRoutingTablesForVlanMethodStub.invoke())
        		.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_route_tables_for_vlan.xml"));
        PowerMockito.whenNew(EC2Method.class)
            	.withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "vpc-id"),
            		hasEntry("Filter.1.Value.1", vlanId),
            		hasEntry("Action", "DescribeRouteTables"))))
            	.thenReturn(describeRoutingTablesForVlanMethodStub);
        
        //list ip addresses
        EC2Method listIpMethodStub = mock(EC2Method.class);
        when(listIpMethodStub.invoke()).thenReturn(resource("org/dasein/cloud/aws/network/describe_addresses.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(
                		allOf(hasEntry("Action", "DescribeAddresses"))))
                .thenReturn(listIpMethodStub);

        //list subnets
        EC2Method describeNetworkInterfaceMethodStub = mock(EC2Method.class);
        when(describeNetworkInterfaceMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/vpc/describe_subnets_for_vlan.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "vpc-id"),
            		hasEntry("Filter.1.Value.1", vlanId),
            		hasEntry("Action", "DescribeSubnets"))))
            .thenReturn(describeNetworkInterfaceMethodStub);
        
        
        //mock vm instances
        ComputeServices computeServices = PowerMockito.mock(EC2ComputeServices.class);
		EC2Instance ec2Instance = PowerMockito.mock(EC2Instance.class);
		PowerMockito.doReturn(computeServices).when(awsCloudStub).getComputeServices();
		PowerMockito.doReturn(ec2Instance).when(computeServices).getVirtualMachineSupport();
		
        VirtualMachine testVM1 = this.createVirtualMachine("r-1a2b3c4d", Architecture.I64, "ami-1a2b3c4d", "subnet-9d4a7b6c", 
        		vlanId, VmState.RUNNING, "10.0.0.12", "46.51.219.63", "c1.medium", Platform.RHEL);
        PowerMockito.doReturn(Arrays.asList(testVM1)).when(ec2Instance).listVirtualMachines();
        
		assertReflectionEquals(
				Arrays.asList(
						createFirewall("sg-1a2b3c4d", vlanId, "WebServers (VPC " + vlanId + ")", "Web Servers", true, true, 
								Arrays.asList(FirewallRule.getInstance(null, "sg-1a2b3c4d", RuleTarget.getCIDR("0.0.0.0/0"), 
										Direction.INGRESS, Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal("sg-1a2b3c4d"), 80, 80))),
						createRoutingTable("rtb-e4ad488d", "rtb-e4ad488d", "rtb-e4ad488d", "vpc-11ad4878", false, 
        						new String[] {"subnet-15ad487c"}, 
        						new Route[] {
        							createRoute("10.0.0.0/22", "local", null, null, null),
        							createRoute("0.0.0.0/0", "igw-eaad4883", null, null, null)
        				}),
						createIpAddress("eipalloc-08229861", "eipassoc-f0229899", AddressType.PUBLIC, "eni-ef229886", IPVersion.IPV4, "46.51.219.63", "i-64600030", true),
						createIpAddress("46.51.219.64", "eipassoc-f0229810", AddressType.PUBLIC, "eni-ef229810", IPVersion.IPV4, "46.51.219.64", "i-64600031", false),
						createIpAddress("198.51.100.2", null, AddressType.PUBLIC, null, IPVersion.IPV4, "198.51.100.2", null, false),
						createSubnet("subnet-9d4a7b6c", vlanId, SubnetState.AVAILABLE, "subnet-9d4a7b6c", "subnet-9d4a7b6c", "10.0.1.0/24", "us-east-1a", 251),
						testVM1,
						createSubnet("subnet-6e7f829e", vlanId, SubnetState.AVAILABLE, "subnet-6e7f829e", "subnet-6e7f829e", "10.0.0.0/24", "us-east-1a", 251)),
				vpc.listResources(vlanId));
	}
	
	private VirtualMachine createVirtualMachine(String instanceId, Architecture architecture, String imageId, String subnetId,
			String vpcId, VmState state, String privateIpAddress, String ipAddress, String instanceType, Platform platform) {
		VirtualMachine instance = new VirtualMachine();
		instance.setProviderVirtualMachineId(instanceId);
		instance.setArchitecture(architecture);
		instance.setProviderMachineImageId(imageId);
		instance.setProviderSubnetId(subnetId);
		instance.setProviderVlanId(vpcId);
		instance.setPrivateAddresses(new RawAddress(privateIpAddress));
		instance.setPublicAddresses(new RawAddress(ipAddress));
		instance.setProductId(instanceType);
		instance.setPlatform(platform);
		instance.setName(instance.getProviderVirtualMachineId());
		instance.setDescription(instance.getName());
		instance.setProviderOwnerId(ACCOUNT_NO);
		instance.setProviderRegionId(REGION);
		instance.setCurrentState(state);
		instance.setPersistent(false);
		return instance;
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
	
	private Firewall createFirewall(String providerFirewallId, String providerVlanId, String name, 
			String description, boolean active, boolean available, List<FirewallRule> rules) {
		Firewall response = new Firewall();
		response.setProviderFirewallId(providerFirewallId);
		response.setProviderVlanId(providerVlanId);
		response.setName(name);
		response.setDescription(description);
		response.setActive(active);
		response.setAvailable(available);
		response.setRules(rules);
		response.setRegionId(REGION);
		return response;
	}
	
	private InternetGateway createInternetGateway(String gwId, InternetGatewayAttachmentState attachmentState, String vlanId) {
		InternetGateway internetGateway = new InternetGateway();
		internetGateway.setProviderInternetGatewayId(gwId);
		internetGateway.setAttachmentState(attachmentState);
		internetGateway.setProviderVlanId(vlanId);
		internetGateway.setProviderOwnerId(ACCOUNT_NO);
		internetGateway.setProviderRegionId(REGION);
		internetGateway.setTags(new HashMap<String, String>());
		return internetGateway;
	}
	
	private RoutingTable createRoutingTable(String routingTableId, String name, String description, 
			String vlanId, boolean isMain, String[] subnets, Route[] routes) {
		RoutingTable routingTable = new RoutingTable();
		routingTable.setProviderRoutingTableId(routingTableId);
		routingTable.setName(name);
		routingTable.setDescription(description);
		routingTable.setProviderVlanId(vlanId);
		routingTable.setRoutes(routes);
		routingTable.setProviderSubnetIds(subnets);
		routingTable.setMain(isMain);
		routingTable.setProviderOwnerId(ACCOUNT_NO);
		routingTable.setProviderRegionId(REGION);
		return routingTable;
	}
	
	private Route createRoute(String destination, String gateway, String instanceId, String ownerId, String nicId) {
		if( gateway != null ) {
            return Route.getRouteToGateway(IPVersion.IPV4, destination, gateway);
        } else {
	        if( instanceId != null && nicId != null ) {
	            return Route.getRouteToVirtualMachineAndNetworkInterface(IPVersion.IPV4, destination, ownerId, instanceId, nicId);
	        } else {
	            if( nicId != null ) {
	                return Route.getRouteToNetworkInterface(IPVersion.IPV4, destination, nicId);
	            } else if( instanceId != null ) {
	                return Route.getRouteToVirtualMachine(IPVersion.IPV4, destination, ownerId, instanceId);
	            }
	        }
        }
		return null;
	}
	
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
