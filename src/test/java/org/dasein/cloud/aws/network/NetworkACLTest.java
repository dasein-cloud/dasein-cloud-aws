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
import org.dasein.cloud.aws.compute.EC2Exception;
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
	public void setUp() throws Exception {
		super.setUp();
		networkACL = new NetworkACL(awsCloudStub);
	}
	
	@Test
	public void associateWithSubnetShouldPostWithCorrectRequest() throws Exception {
		String firewallId = "acl-5d659634";
		String withSubnetId = "subnet-ff669596";
		
		EC2Method describeNetworkAclsMethodStub = mock(EC2Method.class);
        when(describeNetworkAclsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acls.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "association.subnet-id"),
            		hasEntry("Filter.1.Value.1", withSubnetId),
            		hasEntry("Action", "DescribeNetworkAcls"))))
            .thenReturn(describeNetworkAclsMethodStub);
        
        EC2Method replaceNetworkAclAssociationMethodStub = mock(EC2Method.class);
        when(replaceNetworkAclAssociationMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/replace_network_acl_association.xml"));
        PowerMockito.whenNew(EC2Method.class)
        .withArguments(eq(awsCloudStub), argThat(allOf(
        		hasEntry("AssociationId", "aclassoc-5c659635"),
        		hasEntry("NetworkAclId", firewallId),
        		hasEntry("Action", "ReplaceNetworkAclAssociation"))))
        .thenReturn(replaceNetworkAclAssociationMethodStub);
        
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
		
		EC2Method describeNetworkAclsMethodStub = mock(EC2Method.class);
        when(describeNetworkAclsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acls.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Filter.1.Name", "association.subnet-id"),
            		hasEntry("Filter.1.Value.1", withSubnetId),
            		hasEntry("Action", "DescribeNetworkAcls"))))
            .thenReturn(describeNetworkAclsMethodStub);
        
        EC2Method replaceNetworkAckAssociationMethodStub = mock(EC2Method.class);
        when(replaceNetworkAckAssociationMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/replace_network_acl_association_failed.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("AssociationId", "aclassoc-5c659635"),
	        		hasEntry("NetworkAclId", firewallId),
	        		hasEntry("Action", "ReplaceNetworkAclAssociation"))))
	        .thenReturn(replaceNetworkAckAssociationMethodStub);
        
		networkACL.associateWithSubnet(firewallId, withSubnetId);
	}
	
	@Test
	public void authorizeShouldPostWithCorrectRequest() throws Exception {
		String firewallId = "acl-5566953c";
		
		EC2Method describeNetworkAclsMethodStub = mock(EC2Method.class);
        when(describeNetworkAclsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acls.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "DescribeNetworkAcls"))))
            .thenReturn(describeNetworkAclsMethodStub);
        
        EC2Method createNetworkAclEntryMethodStub = mock(EC2Method.class);
        when(createNetworkAclEntryMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/create_network_acl_entry.xml"));
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
	        .thenReturn(createNetworkAclEntryMethodStub);
        
		assertEquals(
				firewallId + ":" + Direction.INGRESS.name() + ":110",
				networkACL.authorize(firewallId, Direction.INGRESS, Permission.ALLOW, RuleTarget.getCIDR("0.0.0.0/0"), Protocol.TCP, null, 80, 80, 110));
	}
	
	@Test
	public void authorizeIcmpRuleShouldPostWithCorrectRequest() throws Exception {
		String firewallId = "acl-5566953c";
		
		EC2Method describeNetworkAclsMethodStub = mock(EC2Method.class);
        when(describeNetworkAclsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acls.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("Action", "DescribeNetworkAcls"))))
            .thenReturn(describeNetworkAclsMethodStub);
		
        EC2Method createNetworkAclEntryMethodStub = mock(EC2Method.class);
        when(createNetworkAclEntryMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/create_network_acl_entry.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("NetworkAclId", firewallId),
	        		hasEntry("Egress", "false"),
	        		hasEntry("RuleNumber", "110"),
	        		hasEntry("Protocol", "1"),
	        		hasEntry("RuleAction", "allow"),
	        		hasEntry("CidrBlock", "0.0.0.0/0"),
	        		hasEntry("Icmp.Code", "-1"),
	        		hasEntry("Icmp.Type", "-1"),
	        		hasEntry("Action", "CreateNetworkAclEntry"))))
	        .thenReturn(createNetworkAclEntryMethodStub);
        
        assertEquals(
				firewallId + ":" + Direction.INGRESS.name() + ":110",
				networkACL.authorize(firewallId, Direction.INGRESS, Permission.ALLOW, RuleTarget.getCIDR("0.0.0.0/0"), Protocol.ICMP, null, 0, 0, 110));
	}
	
	@Test
	public void createFirewallShouldPostWithCorrectRequest () throws Exception {
		String inVlanId = "vpc-11ad4878";
		
		PowerMockito.doReturn(true).when(awsCloudStub).createTags(
				Mockito.anyString(), Mockito.anyString(), (Tag[]) Mockito.any());
		
		FirewallCreateOptions options = FirewallCreateOptions.getInstance(inVlanId, inVlanId, inVlanId);
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/create_network_acl.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("VpcId", inVlanId),
            		hasEntry("Action", "CreateNetworkAcl"))))
            .thenReturn(ec2MethodStub);
		
		assertEquals("acl-5fb85d36",
				networkACL.createFirewall(options));
	}
	
	@Test(expected = CloudException.class)
	public void createFirewallWithoutVlanShouldThrowException() throws Exception {
		networkACL.createFirewall(FirewallCreateOptions.getInstance(null, null));
	}
	
	@Test
	public void createFirewallWithRulesShouldPostWithCorrectRequest() throws Exception {
		
		FirewallCreateOptions options = FirewallCreateOptions.getInstance("vpc-11ad4878", "acl-5fb85d36", "acl-5fb85d36", 
				FirewallRuleCreateOptions.getInstance(Direction.EGRESS, Permission.DENY, null, Protocol.ANY, RuleTarget.getCIDR("0.0.0.0/0"), 80, 80, 32767));
		
		PowerMockito.doReturn(true).when(awsCloudStub).createTags(
				Mockito.anyString(), Mockito.anyString(), (Tag[]) Mockito.any());
		
        EC2Method createNetworkAclMethodStub = mock(EC2Method.class);
        when(createNetworkAclMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/create_network_acl.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("VpcId", "vpc-11ad4878"),
            		hasEntry("Action", "CreateNetworkAcl"))))
            .thenReturn(createNetworkAclMethodStub);
        
        EC2Method describeNetworkAclsMethodStub = mock(EC2Method.class);
        when(describeNetworkAclsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acls.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeNetworkAcls"))))
	        .thenReturn(describeNetworkAclsMethodStub);
        
        EC2Method replaceNetworkAclEntryMethodStub = mock(EC2Method.class);
        when(replaceNetworkAclEntryMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/create_network_acl_entry.xml"));
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
	        .thenReturn(replaceNetworkAclEntryMethodStub);
		
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
        
		assertReflectionEquals(
				createFirewall(firewallId, "vpc-5266953b"), 
				networkACL.getFirewall(firewallId));
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
    
        assertReflectionEquals(
        		Arrays.asList(
        				createFirewall("acl-5566953c", "vpc-5266953b"), 
        				createFirewall("acl-5d659634", "vpc-5266953b", "subnet-f0669599", "subnet-ff669596")),
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
        
		assertReflectionEquals (
				Arrays.asList(
						createFirewallRule(firewallId, Direction.EGRESS, 110, Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal(firewallId), RuleTarget.getCIDR("0.0.0.0/0"), 49152, 65535), 
						createFirewallRule(firewallId, Direction.EGRESS, 32767, Protocol.ANY, Permission.DENY, RuleTarget.getGlobal(firewallId), RuleTarget.getCIDR("0.0.0.0/0"), -1, -1)),
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
		
		EC2Method describeNetworkAclMethodStub = mock(EC2Method.class);
        when(describeNetworkAclMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acl.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("NetworkAclId.1", firewallId), 
	        		hasEntry("Action", "DescribeNetworkAcls"))))
	        .thenReturn(describeNetworkAclMethodStub);
        
        EC2Method describeNetworkAclsMethodStub = mock(EC2Method.class);
        when(describeNetworkAclsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/describe_network_acls.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeNetworkAcls"))))
	        .thenReturn(describeNetworkAclsMethodStub);
      
        EC2Method deleteNetworkAclMethodStub = mock(EC2Method.class);
        when(deleteNetworkAclMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/network_acl/delete_network_acl.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("NetworkAclId", firewallId),
            		hasEntry("Action", "DeleteNetworkAcl"))))
            .thenReturn(deleteNetworkAclMethodStub);
		
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
	
	private Firewall createFirewall(String id, String vlanId, String ... subnets) {
		Firewall firewall = new Firewall();
		firewall.setProviderFirewallId(id);
		firewall.setDescription(firewall.getProviderFirewallId());
		firewall.setName(firewall.getProviderFirewallId());
		firewall.setActive(true);
		firewall.setAvailable(true);
		firewall.setRegionId(REGION);
		firewall.setProviderVlanId(vlanId);
		if (subnets.length == 0) {
			firewall.setSubnetAssociations(null);
		} else {
			firewall.setSubnetAssociations(subnets);
		}
        return firewall;
	}
	
	private FirewallRule createFirewallRule(String firewallId, Direction direction, int precedence, Protocol protocol, 
			Permission permission, RuleTarget sourceEndpoint, RuleTarget destinationEndpoint, int startPort, int endPort) {
		FirewallRule rule = FirewallRule.getInstance(firewallId + ":" + direction.name() + ":" + String.valueOf(precedence), 
				firewallId, 
				sourceEndpoint,
				direction, 
				protocol,
				permission, 
				destinationEndpoint,
				startPort,
				endPort);
		rule.withPrecedence(precedence);
		return rule;
	}
	
}
