package org.dasein.cloud.aws.network;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AwsTestBase;
import org.dasein.cloud.aws.compute.EC2Method;
import org.dasein.cloud.network.NetworkInterface;
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
	public void getNetworkInterfaceShouldReturnCorrectResult() throws Exception {
		
		String nicId = "eni-0f62d866";
		
		EC2Method describeNICEC2MethodStub = mock(EC2Method.class);
        when(describeNICEC2MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/ipaddress/describe_network_interface.xml"));
        PowerMockito.whenNew(EC2Method.class)
            .withArguments(eq(awsCloudStub), argThat(allOf(
            		hasEntry("NetworkInterfaceId.1", nicId),
            		hasEntry("Action", "DescribeNetworkInterfaces"))))
            .thenReturn(describeNICEC2MethodStub);
		
        NetworkInterface networkInterface = vpc.getNetworkInterface(nicId);
	}
	
	
	
}
