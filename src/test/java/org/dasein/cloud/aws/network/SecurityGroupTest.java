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
import java.util.Iterator;

import junit.framework.AssertionFailedError;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AWSResourceNotFoundException;
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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSCloud.class, EC2Instance.class, EC2ComputeServices.class, SecurityGroup.class})
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
		
		Firewall response = securityGroup.getFirewall(securityGroupId);
		assertEquals(REGION, response.getRegionId());
		assertEquals("Web Servers", response.getDescription());
		assertEquals("WebServers", response.getName());
		assertEquals(securityGroupId, response.getProviderFirewallId());
		assertNull(response.getProviderVlanId());
		assertTrue(response.isActive());
		assertTrue(response.isAvailable());
		assertEquals(
				Arrays.asList(
						FirewallRule.getInstance(null, securityGroupId, RuleTarget.getCIDR("0.0.0.0/0"), 
								Direction.INGRESS, Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal(securityGroupId), 80, 80)), 
				response.getRules());
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
		
        assertEquals(
				Arrays.asList(
						FirewallRule.getInstance(null, securityGroupId, RuleTarget.getCIDR("0.0.0.0/0"), 
								Direction.INGRESS, Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal(securityGroupId), 80, 80)), 
				securityGroup.getRules(securityGroupId));
	}
	
	@Ignore
	@Test
	public void isSubscribedShouldReturnCorrectResult() throws CloudException, InternalException {
		EC2ComputeServices ec2ComputeServicesMock = PowerMockito.spy(new EC2ComputeServices(awsCloudStub));
		EC2Instance ec2Instance = Mockito.mock(EC2Instance.class);
		Mockito.doReturn(ec2Instance).when(ec2ComputeServicesMock).getVirtualMachineSupport();
		assertTrue(securityGroup.isSubscribed());
	}
	
	@Test
	public void listShouldReturnCorrectResult() throws Exception {
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_groups.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(hasEntry("Action", "DescribeSecurityGroups"))))
                .thenReturn(ec2MethodStub);
		
		Iterator<Firewall> firewalls = securityGroup.list().iterator();
		Firewall firewall1 = firewalls.next();
		assertEquals(REGION, firewall1.getRegionId());
		assertEquals("Web Servers", firewall1.getDescription());
		assertEquals("WebServers", firewall1.getName());
		assertEquals("sg-1a2b3c4d", firewall1.getProviderFirewallId());
		assertNull(firewall1.getProviderVlanId());
		assertTrue(firewall1.isActive());
		assertTrue(firewall1.isAvailable());
		assertEquals(
				Arrays.asList(
						FirewallRule.getInstance(null, "sg-1a2b3c4d", RuleTarget.getCIDR("0.0.0.0/0"), 
								Direction.INGRESS, Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal("sg-1a2b3c4d"), 80, 80)), 
				firewall1.getRules());
		
		Firewall firewall2 = firewalls.next();
		assertEquals(REGION, firewall2.getRegionId());
		assertEquals("Group A", firewall2.getDescription());
		assertEquals("RangedPortsBySource", firewall2.getName());
		assertEquals("sg-2a2b3c4d", firewall2.getProviderFirewallId());
		assertNull(firewall2.getProviderVlanId());
		assertTrue(firewall2.isActive());
		assertTrue(firewall2.isAvailable());
		assertEquals(
				Arrays.asList(
						FirewallRule.getInstance(null, "sg-2a2b3c4d", RuleTarget.getGlobal("sg-3a2b3c4d"), 
								Direction.INGRESS, Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal("sg-2a2b3c4d"), 6000, 7000)), 
				firewall2.getRules());
		
		assertFalse(firewalls.hasNext());
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
		
		FirewallRuleCreateOptions options = FirewallRuleCreateOptions.getInstance(
				Direction.INGRESS, Permission.ALLOW, RuleTarget.getCIDR("192.168.110.0/100"), Protocol.TCP, null, 
				8080, 8080, 100);
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/authorize_security_group_ingress.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("GroupId.1", securityGroupId), 
            		hasEntry("Action", "DescribeSecurityGroups"))))
            .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId", securityGroupId),
	        		hasEntry("IpPermissions.1.IpProtocol", Protocol.TCP.name().toLowerCase()),
	        		hasEntry("IpPermissions.1.FromPort", "8080"),
	        		hasEntry("IpPermissions.1.ToPort", "8080"),
	        		hasEntry("IpPermissions.1.IpRanges.1.CidrIp", "192.168.110.0/100"),
	        		hasEntry("Action", "AuthorizeSecurityGroupIngress"))))
	        .thenReturn(ec2MethodStub);
		
        assertEquals(
        		FirewallRule.getInstance(null, securityGroupId, RuleTarget.getCIDR("192.168.110.0/100"), Direction.INGRESS, 
        				Protocol.TCP, Permission.ALLOW, RuleTarget.getGlobal(securityGroupId), 8080, 8080).getProviderRuleId(),
        		securityGroup.authorize(securityGroupId, options));
	}
	
	@Test
	public void authorizeEgressRuleShouldPostWithCorrectRequest() throws Exception {
		
		String securityGroupId = "sg-1a2b3c4d";
		
		FirewallRuleCreateOptions options = FirewallRuleCreateOptions.getInstance(
				Direction.EGRESS, Permission.ALLOW, null, Protocol.TCP, RuleTarget.getCIDR("202.108.110.0/100"), 
				8080, 8080, 100);
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_vpc_security_group.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/authorize_security_group_egress.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("GroupId.1", securityGroupId), 
            		hasEntry("Action", "DescribeSecurityGroups"))))
            .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId", securityGroupId),
	        		hasEntry("IpPermissions.1.IpProtocol", Protocol.TCP.name().toLowerCase()),
	        		hasEntry("IpPermissions.1.FromPort", "8080"),
	        		hasEntry("IpPermissions.1.ToPort", "8080"),
	        		hasEntry("IpPermissions.1.IpRanges.1.CidrIp", "202.108.110.0/100"),
	        		hasEntry("Action", "AuthorizeSecurityGroupEgress"))))
	        .thenReturn(ec2MethodStub);
		
        assertEquals(
        		FirewallRule.getInstance(null, securityGroupId, RuleTarget.getGlobal(securityGroupId), Direction.EGRESS, 
        				Protocol.TCP, Permission.ALLOW, RuleTarget.getCIDR("202.108.110.0/100"), 8080, 8080).getProviderRuleId(),
        		securityGroup.authorize(securityGroupId, options));
	}
	
	@Test(expected = OperationNotSupportedException.class)
	public void authorizeDenyRuleShouldThrowException() throws CloudException, InternalException {
		String securityGroupId = "sg-1a2b3c4d";
		FirewallRuleCreateOptions options = FirewallRuleCreateOptions.getInstance(
				Direction.INGRESS, Permission.DENY, RuleTarget.getCIDR(""), Protocol.TCP, RuleTarget.getGlobal(""), 
				8080, 8080, 100);
		securityGroup.authorize(securityGroupId, options);
	}
	
	@Test(expected = CloudException.class)
	public void authorizeShouldThrowExceptionIfFirewallIsNull() throws Exception {
		String securityGroupId = "sg-1invalid";
		FirewallRuleCreateOptions options = FirewallRuleCreateOptions.getInstance(
				Direction.INGRESS, Permission.ALLOW, null, Protocol.TCP, null, 8080, 8080, 100);
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("GroupId.1", securityGroupId), 
                		hasEntry("Action", "DescribeSecurityGroups"))))
                .thenReturn(ec2MethodStub);
		securityGroup.authorize(securityGroupId, options);
	}
	
	@Test(expected=OperationNotSupportedException.class)
	public void authorizeEgressRuleForInternetFirewallShouldThrowException() throws Exception {
		String securityGroupId = "sg-1a2b3c4d";
		FirewallRuleCreateOptions options = FirewallRuleCreateOptions.getInstance(
				Direction.EGRESS, Permission.ALLOW, null, Protocol.TCP, null, 8080, 8080, 100);
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(
                		hasEntry("GroupId.1", securityGroupId), 
                		hasEntry("Action", "DescribeSecurityGroups"))))
                .thenReturn(ec2MethodStub);
		securityGroup.authorize(securityGroupId, options);
	}
	
	@Test
	public void revokeShouldDeleteWithCorrectRequest() throws Exception {
		String ruleId = FirewallRule.getRuleId(
				"sg-1a2b3c4d", RuleTarget.getCIDR("0.0.0.0/0"), Direction.INGRESS, Protocol.TCP, Permission.ALLOW, 
				RuleTarget.getGlobal("sg-1a2b3c4d"), 80, 80);
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"))
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"))
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"))
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/revoke_security_group_rule.xml"));
        PowerMockito.whenNew(EC2Method.class)
            	.withArguments(eq(awsCloudStub), argThat(allOf(
            			hasEntry("Action", "DescribeSecurityGroups"))))
            	.thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId.1", "sg-1a2b3c4d"), 
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId", "sg-1a2b3c4d"), 
	        		hasEntry("IpPermissions.1.IpProtocol", "tcp"),
	        		hasEntry("IpPermissions.1.FromPort", "80"),
	        		hasEntry("IpPermissions.1.ToPort", "80"),
	        		hasEntry("IpPermissions.1.IpRanges.1.CidrIp", "0.0.0.0/0"),
	        		hasEntry("Action", "RevokeSecurityGroupIngress"))))
	        .thenReturn(ec2MethodStub);
		securityGroup.revoke(ruleId);
	}
	
	@Test(expected = AWSResourceNotFoundException.class)
	public void revokeShouldThrowExceptionIfNoSuchFirewall() throws Exception {
		String ruleId = FirewallRule.getRuleId(
				"sg-1a2b3c4d", RuleTarget.getCIDR("0.0.0.0/0"), Direction.INGRESS, Protocol.TCP, Permission.ALLOW, 
				RuleTarget.getGlobal("sg-1a2b3c4d"), 80, 80);
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"))
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"))
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_empty_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
            	.withArguments(eq(awsCloudStub), argThat(allOf(
            			hasEntry("Action", "DescribeSecurityGroups"))))
            	.thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
		        .withArguments(eq(awsCloudStub), argThat(allOf(
		        		hasEntry("GroupId.1", "sg-1a2b3c4d"), 
		        		hasEntry("Action", "DescribeSecurityGroups"))))
		        .thenReturn(ec2MethodStub);
		securityGroup.revoke(ruleId);
	}
	
	@Test(expected = CloudException.class)
	public void revokeShouldThrowExceptionIfFailed() throws Exception {
		String ruleId = FirewallRule.getRuleId(
				"sg-1a2b3c4d", RuleTarget.getCIDR("0.0.0.0/0"), Direction.INGRESS, Protocol.TCP, Permission.ALLOW, 
				RuleTarget.getGlobal("sg-1a2b3c4d"), 80, 80);
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"))
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"))
        		.thenReturn(resource("org/dasein/cloud/aws/network/security_group/revoke_security_group_rule_failed.xml"));
        PowerMockito.whenNew(EC2Method.class)
            	.withArguments(eq(awsCloudStub), argThat(allOf(
            			hasEntry("Action", "DescribeSecurityGroups"))))
            	.thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId.1", "sg-1a2b3c4d"), 
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId", "sg-1a2b3c4d"), 
	        		hasEntry("IpPermissions.1.IpProtocol", "tcp"),
	        		hasEntry("IpPermissions.1.FromPort", "80"),
	        		hasEntry("IpPermissions.1.ToPort", "80"),
	        		hasEntry("IpPermissions.1.IpRanges.1.CidrIp", "0.0.0.0/0"),
	        		hasEntry("Action", "RevokeSecurityGroupIngress"))))
	        .thenReturn(ec2MethodStub);
		securityGroup.revoke(ruleId);
	}

	@Test
	public void createFirewallWithoutRulesShouldPostWithCorrectRequest() throws Exception {
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_groups.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/create_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("GroupName", "WebServerSG"), 
            		hasEntry("GroupDescription", "Web Servers"),
            		hasEntry("Action", "CreateSecurityGroup"))))
            .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(ec2MethodStub);
		
        FirewallCreateOptions options = FirewallCreateOptions.getInstance(
				"WebServerSG", "Web Servers");
		assertEquals("sg-1a2b3c4d", securityGroup.create(options));
	}
	
	@Test
	public void createFirewallWithRulesShouldPostWithCorrectRequest() throws Exception {
		
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_groups.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/create_security_group.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_group.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/authorize_security_group_ingress.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("GroupName", "WebServerSG"), 
            		hasEntry("GroupDescription", "Web Servers"),
            		hasEntry("Action", "CreateSecurityGroup"))))
            .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId.1", "sg-1a2b3c4d"), 
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(ec2MethodStub);
	    PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("GroupId", "sg-1a2b3c4d"),
	        		hasEntry("IpPermissions.1.IpProtocol", Protocol.TCP.name().toLowerCase()),
	        		hasEntry("IpPermissions.1.FromPort", "80"),
	        		hasEntry("IpPermissions.1.ToPort", "80"),
	        		hasEntry("IpPermissions.1.IpRanges.1.CidrIp", "192.0.2.0/24"),
	        		hasEntry("Action", "AuthorizeSecurityGroupIngress"))))
	        .thenReturn(ec2MethodStub);
		
		FirewallRuleCreateOptions ruleOptions = FirewallRuleCreateOptions.getInstance(
				Direction.INGRESS, Permission.ALLOW, RuleTarget.getCIDR("192.0.2.0/24"), Protocol.TCP, null, 80, 80, 10);
		FirewallCreateOptions options = FirewallCreateOptions.getInstance(
				"WebServerSG", "Web Servers", ruleOptions);
		assertEquals("sg-1a2b3c4d", securityGroup.create(options));
	}
	
	@Test
	public void createVpcFirewallShouldPostWithCorrectRequest() throws Exception {
		EC2Method ec2MethodStub = mock(EC2Method.class);
        when(ec2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/describe_security_groups.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/security_group/create_security_group.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("GroupName", "WebServerSG"), 
            		hasEntry("GroupDescription", "Web Servers"),
            		hasEntry("VpcId", "vpc-3325caf2"),
            		hasEntry("Action", "CreateSecurityGroup"))))
            .thenReturn(ec2MethodStub);
        PowerMockito.whenNew(EC2Method.class)
	        .withArguments(eq(awsCloudStub), argThat(allOf(
	        		hasEntry("Action", "DescribeSecurityGroups"))))
	        .thenReturn(ec2MethodStub);
		
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
}
