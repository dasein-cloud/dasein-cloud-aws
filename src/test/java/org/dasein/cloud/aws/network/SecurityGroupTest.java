package org.dasein.cloud.aws.network;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.*;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.unitils.reflectionassert.ReflectionAssert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.AssertionFailedError;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AWSResourceNotFoundException;
import org.dasein.cloud.aws.AwsTestBase;
import org.dasein.cloud.aws.EC2Provider;
import org.dasein.cloud.aws.compute.EC2Method;
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallCreateOptions;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.FirewallRuleCreateOptions;
import org.dasein.cloud.network.Permission;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.network.RuleTarget;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSCloud.class, SecurityGroup.class})
public class SecurityGroupTest extends AwsTestBase {

	private SecurityGroup securityGroup;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		securityGroup = new SecurityGroup(awsCloudStub);
	}
	
	@Test
	public void getActiveConstraintsForFirewallShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals(
				Collections.emptySet(), 
				securityGroup.getActiveConstraintsForFirewall(null).keySet());
	}
	
	@Test
	public void getCapabilitiesShouldReturnCorrectResult() throws CloudException, InternalException {
		assertTrue(securityGroup.getCapabilities() instanceof SecurityGroupCapabilities);
	}
	
	@Test
	public void getFirewallShouldReturnCorrectResult() throws Exception {
		
		String securityGroupId = "sg-1a2b3c4d";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("GroupId.1", securityGroupId), 
                		hasEntry("Action", "DescribeSecurityGroups"))))
                .thenReturn(ec2MethodStub);
		
        assertReflectionEquals(
				createFirewall(securityGroupId, null, "WebServers", "Web Servers", true, true, 
						Arrays.asList(FirewallRule.getInstance(null, securityGroupId, RuleTarget.getCIDR("0.0.0.0/0"), 
						Direction.INGRESS, Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal(securityGroupId), 80, 80))),
				securityGroup.getFirewall(securityGroupId));
	}
	
	@Test
	public void getFirewallConstraintsForCloudShouldReturnCorrectResult() throws InternalException, CloudException {
		assertReflectionEquals(
				securityGroup.getCapabilities().getFirewallConstraintsForCloud(),
				securityGroup.getFirewallConstraintsForCloud());
	}
	
	@Test
	public void getProviderTermForFirewallShouldReturnCorrectResult() throws AssertionFailedError, CloudException, InternalException {
		assertReflectionEquals(
				securityGroup.getCapabilities().getProviderTermForFirewall(null),
				securityGroup.getProviderTermForFirewall(null));
	}
	
	@Test
	public void getRulesShouldReturnCorrectResult() throws Exception {
		
		String securityGroupId = "sg-1a2b3c4d";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("GroupId.1", securityGroupId), 
                		hasEntry("Action", "DescribeSecurityGroups"))))
                .thenReturn(ec2MethodStub);
		
        assertReflectionEquals(
				Arrays.asList(
						FirewallRule.getInstance(null, securityGroupId, RuleTarget.getCIDR("0.0.0.0/0"), 
								Direction.INGRESS, Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal(securityGroupId), 80, 80)), 
				securityGroup.getRules(securityGroupId));
	}
	
	@Test
	public void getRulesForEucalyptusProviderShouldReturnCorrectResult() throws Exception {
		String securityGroupName = "WebServers";
		
		PowerMockito.doReturn(EC2Provider.EUCALYPTUS).when(awsCloudStub).getEC2Provider();
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("GroupName.1", securityGroupName), 
                		hasEntry("Action", "DescribeSecurityGroups"))))
                .thenReturn(ec2MethodStub);
		
        assertReflectionEquals(
				Arrays.asList(
						FirewallRule.getInstance(null, securityGroupName, RuleTarget.getCIDR("0.0.0.0/0"), 
								Direction.INGRESS, Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal(securityGroupName), 80, 80)), 
				securityGroup.getRules(securityGroupName));
	}

	@Test
	public void listShouldReturnCorrectResult() throws Exception {
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_groups.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("Action", "DescribeSecurityGroups"))))
                .thenReturn(ec2MethodStub);
		
		assertReflectionEquals(
				Arrays.asList(
						createFirewall("sg-1a2b3c4d", null, "WebServers", "Web Servers", true, true, 
								Arrays.asList(FirewallRule.getInstance(null, "sg-1a2b3c4d", RuleTarget.getCIDR("0.0.0.0/0"), 
										Direction.INGRESS, Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal("sg-1a2b3c4d"), 80, 80))),
						createFirewall("sg-2a2b3c4d", null, "RangedPortsBySource", "Group A", true, true, 
								Arrays.asList(FirewallRule.getInstance(null, "sg-2a2b3c4d", RuleTarget.getGlobal("sg-3a2b3c4d"), 
										Direction.INGRESS, Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal("sg-2a2b3c4d"), 6000, 7000)))),
				securityGroup.list());
	}
	
	@Test
	public void listFirewallStatusShouldReturnCorrectResult() throws Exception {
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_groups.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(hasEntry("Action", "DescribeSecurityGroups"))))
                .thenReturn(ec2MethodStub);
		
        assertReflectionEquals(
				Arrays.asList(
						new ResourceStatus("sg-1a2b3c4d", true), 
						new ResourceStatus("sg-2a2b3c4d", true)), 
				securityGroup.listFirewallStatus());
	}
	
	@Test
	public void authorizeIngressRuleShouldPostWithCorrectRequest() throws Exception {
		
		String securityGroupId = "sg-1a2b3c4d";
		String cidr = "192.168.110.0/100";
		int startPort = 8080;
		int endPort = 8080;
		
		EC2Method describeSecurityGroupMethodStub = mock(EC2Method.class);
        when(describeSecurityGroupMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId.1", securityGroupId), 
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(describeSecurityGroupMethodStub);
        
        EC2Method authorizeSecurityGroupMethodStub = mock(EC2Method.class);
        when(authorizeSecurityGroupMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/authorize_security_group_ingress.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId", securityGroupId),
	        		hasEntry("IpPermissions.1.IpProtocol", Protocol.TCP.name().toLowerCase()),
	        		hasEntry("IpPermissions.1.FromPort", String.valueOf(startPort)),
	        		hasEntry("IpPermissions.1.ToPort", String.valueOf(endPort)),
	        		hasEntry("IpPermissions.1.IpRanges.1.CidrIp", cidr),
	        		hasEntry("Action", "AuthorizeSecurityGroupIngress"))))
	        .thenReturn(authorizeSecurityGroupMethodStub);
        
        assertEquals(
                FirewallRule.getInstance(null, securityGroupId, RuleTarget.getCIDR(cidr), Direction.INGRESS, 
                		Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal(securityGroupId), startPort, endPort).getProviderRuleId(),
                securityGroup.authorize(securityGroupId, Direction.INGRESS, Permission.ALLOW, RuleTarget.getCIDR(cidr), 
                		Protocol.TCP, null, startPort, endPort, 100));
	}
	
	@Test
	public void authorizeEgressRuleShouldPostWithCorrectRequest() throws Exception {
		
		String securityGroupId = "sg-1a2b3c4d";
		
		FirewallRuleCreateOptions options = FirewallRuleCreateOptions.getInstance(
				Direction.EGRESS, Permission.ALLOW, null, Protocol.TCP, RuleTarget.getCIDR("202.108.110.0/100"), 
				8080, 8080, 100);
		
		EC2Method describeVpcSecurityGroupMethodStub = mock(EC2Method.class);
        when(describeVpcSecurityGroupMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_vpc_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId.1", securityGroupId), 
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(describeVpcSecurityGroupMethodStub);
        
        EC2Method authorizeSecurityGroupEgressMethodStub = mock(EC2Method.class);
        when(authorizeSecurityGroupEgressMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/authorize_security_group_egress.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId", securityGroupId),
	        		hasEntry("IpPermissions.1.IpProtocol", Protocol.TCP.name().toLowerCase()),
	        		hasEntry("IpPermissions.1.FromPort", "8080"),
	        		hasEntry("IpPermissions.1.ToPort", "8080"),
	        		hasEntry("IpPermissions.1.IpRanges.1.CidrIp", "202.108.110.0/100"),
	        		hasEntry("Action", "AuthorizeSecurityGroupEgress"))))
	        .thenReturn(authorizeSecurityGroupEgressMethodStub);
		
        assertEquals(
        		FirewallRule.getInstance(null, securityGroupId, RuleTarget.getGlobal(securityGroupId), Direction.EGRESS, 
        				Protocol.TCP, Permission.ALLOW, RuleTarget.getCIDR("202.108.110.0/100"), 8080, 8080).getProviderRuleId(),
        		securityGroup.authorize(securityGroupId, options));
	}
	
	@Test
	public void authorizeRuleForEucalyptusProviderShouldPostWithCorrectRequest() throws Exception {
		
		String securityGroupId = "sg-1a2b3c4d";
		String cidr = "192.168.110.0/100";
		int startPort = 8080;
		int endPort = 8080;
		
		PowerMockito.doReturn(EC2Provider.EUCALYPTUS).when(awsCloudStub).getEC2Provider();
		
		EC2Method describeSecurityGroupMethodStub = mock(EC2Method.class);
        when(describeSecurityGroupMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupName.1", securityGroupId), 
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(describeSecurityGroupMethodStub);
        
        EC2Method authorizeSecurityGroupMethodStub = mock(EC2Method.class);
        when(authorizeSecurityGroupMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/authorize_security_group_ingress.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupName", securityGroupId),
	        		hasEntry("IpProtocol", Protocol.TCP.name().toLowerCase()),
	        		hasEntry("FromPort", String.valueOf(startPort)),
	        		hasEntry("ToPort", String.valueOf(endPort)),
	        		hasEntry("CidrIp", cidr),
	        		hasEntry("Action", "AuthorizeSecurityGroupIngress"))))
	        .thenReturn(authorizeSecurityGroupMethodStub);
        
        assertEquals(
                FirewallRule.getInstance(null, securityGroupId, RuleTarget.getCIDR(cidr), Direction.INGRESS, 
                		Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal(securityGroupId), startPort, endPort).getProviderRuleId(),
                securityGroup.authorize(securityGroupId, Direction.INGRESS, Permission.ALLOW, RuleTarget.getCIDR(cidr), 
                		Protocol.TCP, null, startPort, endPort, 100));
	}
	
	@Test(expected = OperationNotSupportedException.class)
	public void authorizeAnyProtocolIngressRuleForNonVpcShouldThrowException() throws Exception {
		
		String securityGroupId = "sg-1a2b3c4d";
		
		EC2Method describeSecurityGroupMethodStub = mock(EC2Method.class);
        when(describeSecurityGroupMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId.1", securityGroupId), 
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(describeSecurityGroupMethodStub);
        
        securityGroup.authorize(securityGroupId, Direction.INGRESS, Permission.ALLOW, 
        		RuleTarget.getCIDR("192.168.110.0/100"), Protocol.ANY, null, 8080, 8080, 100);
	}
	
	@Test(expected = OperationNotSupportedException.class)
	public void authorizeDenyRuleShouldThrowException() throws CloudException, InternalException {
		String securityGroupId = "sg-1a2b3c4d";
		FirewallRuleCreateOptions options = FirewallRuleCreateOptions.getInstance(
				Direction.INGRESS, Permission.DENY, RuleTarget.getCIDR(securityGroupId), Protocol.TCP, RuleTarget.getGlobal(securityGroupId), 
				8080, 8080, 100);
		securityGroup.authorize(securityGroupId, options);
	}
		
	@Test(expected = CloudException.class)
	public void authorizeShouldThrowExceptionIfFirewallIsNull() throws Exception {
		String securityGroupId = "sg-1invalid";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("GroupId.1", securityGroupId), 
                		hasEntry("Action", "DescribeSecurityGroups"))))
                .thenReturn(ec2MethodStub);
		
        FirewallRuleCreateOptions options = FirewallRuleCreateOptions.getInstance(
				Direction.INGRESS, Permission.ALLOW, null, Protocol.TCP, null, 8080, 8080, 100);
        securityGroup.authorize(securityGroupId, options);
	}
	
	@Test(expected=OperationNotSupportedException.class)
	public void authorizeEgressRuleForInternetFirewallShouldThrowException() throws Exception {
		String securityGroupId = "sg-1a2b3c4d";
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("GroupId.1", securityGroupId), 
                		hasEntry("Action", "DescribeSecurityGroups"))))
                .thenReturn(ec2MethodStub);
        
        FirewallRuleCreateOptions options = FirewallRuleCreateOptions.getInstance(
				Direction.EGRESS, Permission.ALLOW, null, Protocol.TCP, null, 8080, 8080, 100);
		securityGroup.authorize(securityGroupId, options);
	}
	
	@Test
	public void authorizeAnyProtocolIngressRuleForInternetFirewallShouldPostWithCorrectRequest() throws Exception {
		String securityGroupId = "sg-1a2b3c4d";
		String cidr = "0.0.0.0/0";
		int startPort = 80;
		int endPort = 80;
		
		EC2Method describeSecurityGroupMethodStub = mock(EC2Method.class);
        when(describeSecurityGroupMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_vpc_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId.1", securityGroupId), 
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(describeSecurityGroupMethodStub);
        
        EC2Method authorizeSecurityGroupMethodStub = mock(EC2Method.class);
        when(authorizeSecurityGroupMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/authorize_security_group_ingress.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId", securityGroupId),
	        		hasEntry("IpPermissions.1.IpProtocol", "-1"),
	        		hasEntry("IpPermissions.1.FromPort", String.valueOf(startPort)),
	        		hasEntry("IpPermissions.1.ToPort", String.valueOf(endPort)),
	        		hasEntry("IpPermissions.1.IpRanges.1.CidrIp", cidr),
	        		hasEntry("Action", "AuthorizeSecurityGroupIngress"))))
	        .thenReturn(authorizeSecurityGroupMethodStub);
        
        assertEquals(
                FirewallRule.getInstance(null, securityGroupId, RuleTarget.getCIDR(cidr), Direction.INGRESS, 
                		Protocol.ANY, Permission.ALLOW, RuleTarget.getGlobal(securityGroupId), startPort, endPort).getProviderRuleId(),
                securityGroup.authorize(securityGroupId, Direction.INGRESS, Permission.ALLOW, RuleTarget.getCIDR(cidr), 
                		Protocol.ANY, null, startPort, endPort, 100));
	}
	
	@Test
	public void revokeShouldDeleteWithCorrectRequest() throws Exception {
		
		String firewallId = "sg-1a2b3c4d";
		String ruleId = FirewallRule.getRuleId(
				firewallId, RuleTarget.getCIDR("0.0.0.0/0"), Direction.INGRESS, Protocol.TCP, Permission.ALLOW, 
				RuleTarget.getGlobal(firewallId), 80, 80);
		
		EC2Method describeSecurityGroupsMethodStub = mock(EC2Method.class);
        when(describeSecurityGroupsMethodStub.invoke())
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
	    	.withArguments(eq(awsCloudStub), argThat(allOf(
	    			hasEntry("Action", "DescribeSecurityGroups"))))
	    	.thenReturn(describeSecurityGroupsMethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId.1", firewallId), 
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(describeSecurityGroupsMethodStub);
        
        EC2Method revokeSecurityGroupMethodStub = mock(EC2Method.class);
        when(revokeSecurityGroupMethodStub.invoke())
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/revoke_security_group_rule.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId", firewallId), 
	        		hasEntry("IpPermissions.1.IpProtocol", "tcp"),
	        		hasEntry("IpPermissions.1.FromPort", "80"),
	        		hasEntry("IpPermissions.1.ToPort", "80"),
	        		hasEntry("IpPermissions.1.IpRanges.1.CidrIp", "0.0.0.0/0"),
	        		hasEntry("Action", "RevokeSecurityGroupIngress"))))
	        .thenReturn(revokeSecurityGroupMethodStub);
        
		securityGroup.revoke(ruleId);
	}
	
	@Test(expected = OperationNotSupportedException.class)
	public void revokeShouldThrowExceptionFroEgressEc2Classic() throws Exception {
		String firewallId = "sg-1a2b3c4d";
		String ruleId = FirewallRule.getRuleId(
				firewallId, RuleTarget.getGlobal(firewallId), Direction.EGRESS, Protocol.TCP, Permission.ALLOW, 
				RuleTarget.getCIDR("0.0.0.0/0"), 80, 80);
		
		EC2Method describeSecurityGroupsMethodStub = mock(EC2Method.class);
        when(describeSecurityGroupsMethodStub.invoke())
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group_with_egress_rule.xml"));
        PowerMockito.whenNew(EC2Method.class)
	    	.withArguments(eq(awsCloudStub), argThat(allOf(
	    			hasEntry("Action", "DescribeSecurityGroups"))))
	    	.thenReturn(describeSecurityGroupsMethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId.1", firewallId), 
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(describeSecurityGroupsMethodStub);
        
		securityGroup.revoke(ruleId);
	}
	
	@Test(expected = AWSResourceNotFoundException.class)
	public void revokeShouldThrowExceptionIfNoSuchFirewall() throws Exception {
		
		String firewallId = "sg-1a2b3c4d";
		String ruleId = FirewallRule.getRuleId(
				firewallId, RuleTarget.getCIDR("0.0.0.0/0"), Direction.INGRESS, Protocol.TCP, Permission.ALLOW, 
				RuleTarget.getGlobal(firewallId), 80, 80);
		
		EC2Method describeSecurityGroupMethodStub = mock(EC2Method.class);
        when(describeSecurityGroupMethodStub.invoke())
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_groups.xml"));
        PowerMockito.whenNew(EC2Method.class)
	    	.withArguments(eq(awsCloudStub), argThat(allOf(
	    			hasEntry("Action", "DescribeSecurityGroups"))))
	    	.thenReturn(describeSecurityGroupMethodStub);
        
        EC2Method describeEmptyGroupMethodStub = mock(EC2Method.class);
        when(describeEmptyGroupMethodStub.invoke())
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_groups.xml"))
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_empty_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
		        .withArguments(eq(awsCloudStub), argThat(allOf(
		        		hasEntry("GroupId.1", firewallId), 
		        		hasEntry("Action", "DescribeSecurityGroups"))))
		        .thenReturn(describeEmptyGroupMethodStub);
        
		securityGroup.revoke(ruleId);
	}

	@Test
	public void createFirewallWithoutRulesShouldPostWithCorrectRequest() throws Exception {
		
		EC2Method describeSecurityGroupsMethodStub = mock(EC2Method.class);
        when(describeSecurityGroupsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_groups.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupName", "WebServerSG"), 
	        		hasEntry("GroupDescription", "Web Servers"),
	        		hasEntry("Action", "CreateSecurityGroup"))))
	        .thenReturn(describeSecurityGroupsMethodStub);
        
        EC2Method createSecurityGroupMethodStub = mock(EC2Method.class);
        when(createSecurityGroupMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/create_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(createSecurityGroupMethodStub);
		
        FirewallCreateOptions options = FirewallCreateOptions.getInstance(
				"WebServerSG", "Web Servers");
		assertEquals("sg-1a2b3c4d", securityGroup.create(options));
	}
	
	@Test
	public void createEucalyptusProviderFirewallWithoutRulesShouldPostWithCorrectRequest() throws Exception {
		
		PowerMockito.doReturn(EC2Provider.EUCALYPTUS).when(awsCloudStub).getEC2Provider();
		
		EC2Method describeSecurityGroupsMethodStub = mock(EC2Method.class);
        when(describeSecurityGroupsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_groups.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupName", "WebServerSG"), 
	        		hasEntry("GroupDescription", "Web Servers"),
	        		hasEntry("Action", "CreateSecurityGroup"))))
	        .thenReturn(describeSecurityGroupsMethodStub);
        
        EC2Method createSecurityGroupMethodStub = mock(EC2Method.class);
        when(createSecurityGroupMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/create_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(createSecurityGroupMethodStub);
		
        FirewallCreateOptions options = FirewallCreateOptions.getInstance(
				"WebServerSG", "Web Servers");
		assertEquals("WebServerSG", securityGroup.create(options));
	}
	
	@Test
	public void createFirewallWithRulesShouldPostWithCorrectRequest() throws Exception {
		
		EC2Method describeSecurityGroupsMethodStub = mock(EC2Method.class);
        when(describeSecurityGroupsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_groups.xml"));
        PowerMockito.whenNew(EC2Method.class)
        	.withArguments(eq(awsCloudStub), argThat(allOf(
        		hasEntry("Action", "DescribeSecurityGroups"))))
        	.thenReturn(describeSecurityGroupsMethodStub);
        		
        EC2Method createSecurityGroupMethodStub = mock(EC2Method.class);
        when(createSecurityGroupMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/create_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("GroupName", "WebServerSG"), 
            		hasEntry("GroupDescription", "Web Servers"),
            		hasEntry("Action", "CreateSecurityGroup"))))
            .thenReturn(createSecurityGroupMethodStub);
        
        EC2Method describeSecurityGroupMethodStub = mock(EC2Method.class);
        when(describeSecurityGroupMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId.1", "sg-1a2b3c4d"), 
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(describeSecurityGroupMethodStub);
        
        EC2Method authorizeSecurityGroupIngressMethodStub = mock(EC2Method.class);
        when(authorizeSecurityGroupIngressMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/authorize_security_group_ingress.xml"));
	    PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId", "sg-1a2b3c4d"),
	        		hasEntry("IpPermissions.1.IpProtocol", Protocol.TCP.name().toLowerCase()),
	        		hasEntry("IpPermissions.1.FromPort", "80"),
	        		hasEntry("IpPermissions.1.ToPort", "80"),
	        		hasEntry("IpPermissions.1.IpRanges.1.CidrIp", "192.0.2.0/24"),
	        		hasEntry("Action", "AuthorizeSecurityGroupIngress"))))
	        .thenReturn(authorizeSecurityGroupIngressMethodStub);
		
		FirewallRuleCreateOptions ruleOptions = FirewallRuleCreateOptions.getInstance(
				Direction.INGRESS, Permission.ALLOW, RuleTarget.getCIDR("192.0.2.0/24"), Protocol.TCP, null, 80, 80, 10);
		FirewallCreateOptions options = FirewallCreateOptions.getInstance(
				"WebServerSG", "Web Servers", ruleOptions);
		assertEquals("sg-1a2b3c4d", securityGroup.create(options));
	}
	
	@Test
	public void createVpcFirewallShouldPostWithCorrectRequest() throws Exception {
		
		EC2Method createSecurityGroupMethodStub = mock(EC2Method.class);
        when(createSecurityGroupMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/create_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("GroupName", "WebServerSG"), 
            		hasEntry("GroupDescription", "Web Servers"),
            		hasEntry("VpcId", "vpc-3325caf2"),
            		hasEntry("Action", "CreateSecurityGroup"))))
            .thenReturn(createSecurityGroupMethodStub);
        
        EC2Method describeSecurityGroupsMethodStub = mock(EC2Method.class);
        when(describeSecurityGroupsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_groups.xml"));
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(describeSecurityGroupsMethodStub);
		
        FirewallCreateOptions options = FirewallCreateOptions.getInstance(
        		"vpc-3325caf2", "WebServerSG", "Web Servers");
		assertEquals("sg-1a2b3c4d", securityGroup.create(options));
	}
	
	@Test
	public void deleteShouldDeleteWithCorrectRequest() throws Exception {
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/delete_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
        	.withArguments(eq(awsCloudStub), argThat(allOf(
        		hasEntry("GroupId", "sg-1a2b3c4d"),
        		hasEntry("Action", "DeleteSecurityGroup"))))
        .thenReturn(ec2MethodStub);
		securityGroup.delete("sg-1a2b3c4d");
	}
	
	@Test
	public void deleteForEucalyptusProviderShouldDeleteWithCorrectRequest() throws Exception {
		String firewallName = "WebServers";
		
		PowerMockito.doReturn(EC2Provider.EUCALYPTUS).when(awsCloudStub).getEC2Provider();
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/delete_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
        		.withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupName", firewallName),
	        		hasEntry("Action", "DeleteSecurityGroup"))))
        		.thenReturn(ec2MethodStub);
        
		securityGroup.delete(firewallName);
	}
	
	@Test(expected = CloudException.class)
	public void deleteShouldThrowExceptionIfFailed() throws Exception {
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/delete_security_group_failed.xml"));
        PowerMockito.whenNew(EC2Method.class)
        	.withArguments(eq(awsCloudStub), argThat(allOf(
        		hasEntry("GroupId", "sg-1a2b3c4d"),
        		hasEntry("Action", "DeleteSecurityGroup"))))
        	.thenReturn(ec2MethodStub);
		securityGroup.delete("sg-1a2b3c4d");
	}
	
	private Firewall createFirewall(String providerFirewallId, String providerVlanId, String name, 
			String description, boolean active, boolean available, List<FirewallRule> rules) {
		Firewall response = new Firewall();
		response.setProviderFirewallId(providerFirewallId);
		response.getProviderVlanId();
		response.setName(name);
		response.setDescription(description);
		response.setActive(active);
		response.setAvailable(available);
		response.setRules(rules);
		response.setRegionId(REGION);
		return response;
	}
}
