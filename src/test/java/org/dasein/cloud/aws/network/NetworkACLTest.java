package org.dasein.cloud.aws.network;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.*;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.AssertionFailedError;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Tag;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AwsTestBase;
import org.dasein.cloud.aws.compute.EC2ComputeServices;
import org.dasein.cloud.aws.compute.EC2Instance;
import org.dasein.cloud.aws.compute.EC2Method;
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallCreateOptions;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.FirewallRuleCreateOptions;
import org.dasein.cloud.network.Permission;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.network.RuleTarget;
import org.dasein.cloud.network.VLANSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.unitils.reflectionassert.ReflectionAssert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSCloud.class, EC2Instance.class, EC2ComputeServices.class, NetworkACL.class, VPC.class, VLANSupport.class})
public class NetworkACLTest extends AwsTestBase {

	private NetworkACL networkACL;
	
	@Before
	public void setUp() {
		super.setUp();
		networkACL = new NetworkACL(awsCloudStub);
	}
	
	@Test
	public void associateWithSubnetShouldPostWithCorrectRequest() throws Exception {
		String firewallId = "acl-5d659634";
		String withSubnetId = "subnet-ff669596";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acls.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/replace_network_acl_association.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "association.subnet-id"),
            		hasEntry("Filter.1.Value.1", withSubnetId),
            		hasEntry("Action", "DescribeNetworkAcls"))))
            .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
        .withArguments(eq(awsCloudStub), argThat(allOf(
        		hasEntry("AssociationId", "aclassoc-5c659635"),
        		hasEntry("NetworkAclId", firewallId),
        		hasEntry("Action", "ReplaceNetworkAclAssociation"))))
        .thenReturn(ec2MethodStub);
        
		networkACL.associateWithSubnet(firewallId, withSubnetId);
	}
	
	@Test(expected = CloudException.class)
	public void associateWithSubnetShouldThrowExceptionIfNoSuchAssociation() throws Exception {
		
		String firewallId = "acl-5d659634";
		String withSubnetId = "subnet-ff669597";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acls.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "association.subnet-id"),
            		hasEntry("Filter.1.Value.1", withSubnetId),
            		hasEntry("Action", "DescribeNetworkAcls"))))
            .thenReturn(ec2MethodStub);
		
		networkACL.associateWithSubnet(firewallId, withSubnetId);
	}
	
	@Test(expected = CloudException.class)
	public void associateWithSubnetShouldThrowExceptionIfNoAssociationIdFromResponse() throws Exception {
		
		String firewallId = "acl-5d659634";
		String withSubnetId = "subnet-ff669596";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acls.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/replace_network_acl_association_failed.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "association.subnet-id"),
            		hasEntry("Filter.1.Value.1", withSubnetId),
            		hasEntry("Action", "DescribeNetworkAcls"))))
            .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("AssociationId", "aclassoc-5c659635"),
	        		hasEntry("NetworkAclId", firewallId),
	        		hasEntry("Action", "ReplaceNetworkAclAssociation"))))
	        .thenReturn(ec2MethodStub);
        
		networkACL.associateWithSubnet(firewallId, withSubnetId);
	}
	
	@Test
	public void authorizeShouldPostWithCorrectRequest() throws Exception {
		String firewallId = "acl-5566953c";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acls.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/create_network_acl_entry.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "DescribeNetworkAcls"))))
            .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("NetworkAclId", firewallId),
	        		hasEntry("Egress", "false"),
	        		hasEntry("RuleNumber", "110"),
	        		hasEntry("Protocol", "6"),
	        		hasEntry("RuleAction", "allow"),
	        		hasEntry("CidrBlock", "0.0.0.0/0"),
	        		hasEntry("PortRange.From", "80"),
	        		hasEntry("PortRange.To", "80"),
	        		hasEntry("Action", "CreateNetworkAclEntry"))))
	        .thenReturn(ec2MethodStub);
        
		assertEquals(
				firewallId + ":" + Direction.INGRESS.name() + ":110",
				networkACL.authorize(firewallId, Direction.INGRESS, Permission.ALLOW, RuleTarget.getCIDR("0.0.0.0/0"), Protocol.TCP, null, 80, 80, 110));
	}
	
	@Test
	public void createFirewallShouldPostWithCorrectRequest() throws Exception {
		
		String name = null;
		String description = null;
		
		PowerMockito.doReturn(true).when(awsCloudStub).createTags(
				Mockito.anyString(), Mockito.anyString(), (Tag[]) Mockito.any());
		
		FirewallCreateOptions options = FirewallCreateOptions.getInstance("vpc-11ad4878", name, description);
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/create_network_acl.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("VpcId", "vpc-11ad4878"),
            		hasEntry("Action", "CreateNetworkAcl"))))
            .thenReturn(ec2MethodStub);
		
		assertEquals(
				"acl-5fb85d36",
				networkACL.createFirewall(options));
	}
	
	@Test
	public void createFirewallWithRulesShouldPostWithCorrectRequest() throws Exception {
		
		FirewallCreateOptions options = FirewallCreateOptions.getInstance("vpc-11ad4878", "acl-5fb85d36", "acl-5fb85d36", 
				FirewallRuleCreateOptions.getInstance(Direction.EGRESS, Permission.DENY, null, Protocol.ANY, RuleTarget.getCIDR("0.0.0.0/0"), 80, 80, 32767));
		
		PowerMockito.doReturn(true).when(awsCloudStub).createTags(
				Mockito.anyString(), Mockito.anyString(), (Tag[]) Mockito.any());
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/create_network_acl.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acls.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/create_network_acl_entry.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("VpcId", "vpc-11ad4878"),
            		hasEntry("Action", "CreateNetworkAcl"))))
            .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
        .withArguments(eq(awsCloudStub), argThat(allOf(
        		hasEntry("Action", "DescribeNetworkAcls"))))
        .thenReturn(ec2MethodStub);
	    PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("NetworkAclId", "acl-5fb85d36"),
	        		hasEntry("Egress", "true"),
	        		hasEntry("RuleNumber", "32767"),
	        		hasEntry("Protocol", "-1"),
	        		hasEntry("RuleAction", "deny"),
	        		hasEntry("CidrBlock", "0.0.0.0/0"),
	        		hasEntry("PortRange.From", "80"),
	        		hasEntry("PortRange.To", "80"),
	        		hasEntry("Action", "ReplaceNetworkAclEntry"))))
	        .thenReturn(ec2MethodStub);
		
	    assertEquals(
				"acl-5fb85d36",
				networkACL.createFirewall(options));
	}
	
	@Test
	public void getActiveConstraintsForFirewallShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals(
				Collections.emptySet(), 
				networkACL.getActiveConstraintsForFirewall(null).keySet());
	}
	
	@Test
	public void getCapabilitiesShouldReturnCorrectResult() {
		assertTrue(networkACL.getCapabilities() instanceof NetworkACLCapabilities);
	}
	
	@Test
	public void getFirewallShouldReturnCorrectResult() throws Exception {
		String firewallId = "acl-5566953c";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acl.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("NetworkAclId.1", firewallId), 
            		hasEntry("Action", "DescribeNetworkAcls"))))
            .thenReturn(ec2MethodStub);
        
		Firewall result = networkACL.getFirewall(firewallId);
		assertEquals(firewallId, result.getProviderFirewallId());
		assertEquals(firewallId, result.getDescription());
		assertEquals(firewallId, result.getName());
		assertTrue(result.isActive());
		assertTrue(result.isAvailable());
		assertEquals(REGION, result.getRegionId());
		assertEquals("vpc-5266953b", result.getProviderVlanId());
	}
	
	@Test
	public void getFirewallConstraintsForCloudShouldReturnCorrectResult() throws AssertionFailedError, InternalException, CloudException {
		assertReflectionEquals(
				networkACL.getCapabilities().getFirewallConstraintsForCloud(),
				networkACL.getFirewallConstraintsForCloud());
	}
	
	@Test
	public void getProviderTermForNetworkFirewallShouldReturnCorrectResult() {
		assertEquals(
				networkACL.getCapabilities().getProviderTermForNetworkFirewall(null),
				networkACL.getProviderTermForNetworkFirewall(null));
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfVlanSubscribed() throws CloudException, InternalException {
		EC2NetworkServices services = PowerMockito.spy(new EC2NetworkServices(awsCloudStub));
		VPC support = PowerMockito.spy(new VPC(awsCloudStub));
		PowerMockito.doReturn(services).when(awsCloudStub).getNetworkServices();
		PowerMockito.doReturn(support).when(services).getVlanSupport();
		PowerMockito.doReturn(true).when(support).isSubscribed();
		assertTrue(networkACL.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfVlanNotSubscribed() throws CloudException, InternalException {
		EC2NetworkServices services = PowerMockito.spy(new EC2NetworkServices(awsCloudStub));
		VPC support = PowerMockito.spy(new VPC(awsCloudStub));
		PowerMockito.doReturn(services).when(awsCloudStub).getNetworkServices();
		PowerMockito.doReturn(support).when(services).getVlanSupport();
		PowerMockito.doReturn(false).when(support).isSubscribed();
		assertFalse(networkACL.isSubscribed());
	}
	
	@Test
	public void listFirewallsShouldReturnCorrectResult() throws Exception {
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acls.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "DescribeNetworkAcls"))))
            .thenReturn(ec2MethodStub);
    
        Firewall firewall1 = new Firewall();
        firewall1.setActive(true);
        firewall1.setAvailable(true);
        firewall1.setProviderFirewallId("acl-5566953c");
        firewall1.setDescription("acl-5566953c");
        firewall1.setName("acl-5566953c");
        firewall1.setProviderVlanId("vpc-5266953b");
        firewall1.setRegionId(REGION);
        Firewall firewall2 = new Firewall();
        firewall2.setActive(true);
        firewall2.setAvailable(true);
        firewall2.setProviderFirewallId("acl-5d659634");
        firewall2.setDescription("acl-5d659634");
        firewall2.setName("acl-5d659634");
        firewall2.setProviderVlanId("vpc-5266953b");
        firewall2.setRegionId(REGION);
        firewall2.setSubnetAssociations(Arrays.asList("subnet-f0669599", "subnet-ff669596").toArray(new String[2]));
        assertReflectionEquals(
        		Arrays.asList(firewall1, firewall2),
        		networkACL.listFirewalls());
	}
	
	@Test
	public void listRulesShouldReturnCorrectResult() throws Exception {
		
		String firewallId = "acl-5566953c";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acl.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "DescribeNetworkAcls"))))
            .thenReturn(ec2MethodStub);
        
		FirewallRule rule1 = FirewallRule.getInstance(
				firewallId + ":" + Direction.EGRESS.name() + ":" + String.valueOf(110), 
				firewallId, RuleTarget.getGlobal(firewallId), Direction.EGRESS, Protocol.TCP, Permission.ALLOW, RuleTarget.getCIDR("0.0.0.0/0"), 49152, 65535);
		rule1.withPrecedence(110);
		FirewallRule rule2 = FirewallRule.getInstance(
				firewallId + ":" + Direction.EGRESS.name() + ":" + String.valueOf(32767), 
				firewallId, RuleTarget.getGlobal(firewallId), Direction.EGRESS, Protocol.ANY, Permission.DENY, RuleTarget.getCIDR("0.0.0.0/0"), -1, -1);
		rule2.withPrecedence(32767);
		
		assertReflectionEquals (
				Arrays.asList(rule1, rule2),
				networkACL.listRules(firewallId));
	}
	
	@Test
	public void listSupportedDestinationTypesShouldReturnCorrectResult() throws InternalException, CloudException {
		assertEquals(
				networkACL.getCapabilities().listSupportedDestinationTypes(),
				networkACL.listSupportedDestinationTypes());
	}
	
	@Test
	public void listSupportedDirectionsShouldReturnCorrectResult() throws InternalException, CloudException {
		assertEquals(
				networkACL.getCapabilities().listSupportedDirections(),
				networkACL.listSupportedDirections());
	}
	
	@Test
	public void listSupportedPermissionsShouldReturnCorrectResult() throws InternalException, CloudException {
		assertEquals(
				networkACL.getCapabilities().listSupportedPermissions(),
				networkACL.listSupportedPermissions());
	}
	
	@Test
	public void listSupportedSourceTypesShouldReturnCorrectResult() throws InternalException, CloudException {
		assertEquals(
				networkACL.getCapabilities().listSupportedSourceTypes(),
				networkACL.listSupportedSourceTypes());
	}
	
	@Test
	public void removeFirewallShouldDeleteWithCorrectRequest() throws Exception {
		
		String firewallId = "acl-5566953c";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acl.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acls.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/delete_network_acl.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("NetworkAclId.1", firewallId), 
	        		hasEntry("Action", "DescribeNetworkAcls"))))
	        .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeNetworkAcls"))))
	        .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("NetworkAclId", firewallId),
            		hasEntry("Action", "DeleteNetworkAcl"))))
            .thenReturn(ec2MethodStub);
		
		networkACL.removeFirewall(firewallId);
	}
	
	@Test
	public void revokeShouldDeleteWithCorrectRequest() throws Exception {
		String providerFirewallRuleId = "acl-5566953c:EGRESS:110";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acl.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("NetworkAclId", "acl-5566953c"),
	        		hasEntry("Egress", "true"),
	        		hasEntry("RuleNumber", "110"),
	        		hasEntry("Action", "DeleteNetworkAclEntry"))))
	        .thenReturn(ec2MethodStub);
		
		networkACL.revoke(providerFirewallRuleId);
	}
	
	@Test(expected = CloudException.class)
	public void revokeShouldThrowExceptionIfIdIsInvalid() throws InternalException, CloudException {
		String providerFirewallRuleId = "jfijfd:fdifjdi";
		networkACL.revoke(providerFirewallRuleId);
	}
	
}
