package org.dasein.cloud.aws.network;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.unitils.reflectionassert.ReflectionAssert.*;

import java.util.Arrays;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AwsTestBase;
import org.dasein.cloud.aws.compute.EC2Exception;
import org.dasein.cloud.network.DNSRecord;
import org.dasein.cloud.network.DNSRecordType;
import org.dasein.cloud.network.DNSZone;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSCloud.class, Route53.class})
public class Route53Test extends AwsTestBase {

	private Route53 route53;
	
	@Rule
    public final TestName name = new TestName();
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		try {
			PowerMockito.doReturn("AIDIODR4TAW7CSEXAMPLE".getBytes()).when(providerContextStub).getAccessPublic();
			PowerMockito.doReturn("AIDIODR4TAW7CSEXAMPLE".getBytes()).when(providerContextStub).getAccessPrivate();
			PowerMockito.doReturn("AIDIODR4TAW7CSEXAMPLE").when(awsCloudStub).signAWS3(
					Mockito.anyString(), 
					Mockito.any(byte[].class), 
					Mockito.anyString());
		} catch (InternalException e) {
			throw new RuntimeException("mock provider signAWS3 failed!");
		}
		
		if (name.getMethodName().startsWith("isSubscribed")) {
			try {
				Route53Method route53MethodStub = mock(Route53Method.class);
				if (name.getMethodName().endsWith("UnAuthorized")) {
					when(route53MethodStub.invoke())
			        	.thenThrow(EC2Exception.create(401, "410c2a4b-e435-49c9-8382-3770d80d7d4c", 
			        			"SC_UNAUTHORIZED", "SC_UNAUTHORIZED"));
				} else if (name.getMethodName().endsWith("Forbidden")) {
					when(route53MethodStub.invoke())
			        	.thenThrow(EC2Exception.create(403, "410c2a4b-e435-49c9-8382-3770d80d7d4c", 
			        			"SC_FORBIDDEN", "SC_FORBIDDEN"));
				} else if (name.getMethodName().endsWith("SubscriptionCheckFailed")) {
					when(route53MethodStub.invoke())
			        	.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", 
			        			"SubscriptionCheckFailed", "SubscriptionCheckFailed"));
				} else if (name.getMethodName().endsWith("AuthFailure")) {
					when(route53MethodStub.invoke())
		        		.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", 
		        				"AuthFailure", "AuthFailure"));
				} else if (name.getMethodName().endsWith("SignatureDoesNotMatch")) {
					when(route53MethodStub.invoke())
		        		.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", 
		        			"SignatureDoesNotMatch", "SignatureDoesNotMatch"));
				} else if (name.getMethodName().endsWith("InvalidClientTokenId")) {
					when(route53MethodStub.invoke())
		        		.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", 
		        			"InvalidClientTokenId", "InvalidClientTokenId"));
				} else if (name.getMethodName().endsWith("OptInRequired")) {
					when(route53MethodStub.invoke())
		        		.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", 
		        			"OptInRequired", "OptInRequired"));
				} else if (name.getMethodName().endsWith("UnknownException")) {
					when(route53MethodStub.invoke())
		        		.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", 
		        			"UnknownException", "UnknownException"));
				} else {
					when(route53MethodStub.invoke())
		        		.thenReturn(resource("org/dasein/cloud/aws/network/route53/list_hosted_zones.xml"));
				}
				PowerMockito.whenNew(Route53Method.class)
					.withArguments(
						eq(Route53Method.LIST_HOSTED_ZONES), 
						eq(awsCloudStub), 
						Mockito.anyString())
					.thenReturn(route53MethodStub);
			} catch (Exception e) {
				
			}
		}
		route53 = new Route53(awsCloudStub);
	}
	
	@Test
	public void getDnsZoneShouldReturnCorrectResult() throws Exception {
		
		String providerDnsZoneId = "Z1D633PJN98FT9";
		
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke()).thenReturn(resource(
        		"org/dasein/cloud/aws/network/route53/get_hosted_zone.xml"));
        PowerMockito.whenNew(Route53Method.class)
        	.withArguments(
        			eq(Route53Method.GET_HOSTED_ZONE), 
        			eq(awsCloudStub), 
        			Mockito.anyString())
        	.thenReturn(route53MethodStub);
		
		assertReflectionEquals(
				createDNSZone("Z1D633PJN98FT9", "example.com.", "example.com.", "This is my first hosted zone.", ACCOUNT_NO,
						"ns-2048.awsdns-64.com", "ns-2049.awsdns-65.net", "ns-2050.awsdns-66.org", "ns-2051.awsdns-67.co.uk"), 
				route53.getDnsZone(providerDnsZoneId));
	}
	
	@Test
	public void getDnsZoneErrorNoSuchHostedZoneShouldReturnCorrectResult() throws Exception {
		
		String providerDnsZoneId = "Z1PA6795UKMFR9";

		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", "NoSuchHostedZone", "Test Invoke with NoSuchHostedZone exception!"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.GET_HOSTED_ZONE), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		
		assertNull(route53.getDnsZone(providerDnsZoneId));
	}
	
	@Test
	public void getDnsZoneErrorAccessDeniedShouldReturnCorrectResult() throws Exception {
		
		String providerDnsZoneId = "Z2682N5HXP0BZ4";

		Route53Method getHostedZoneMethodStub = mock(Route53Method.class);
        when(getHostedZoneMethodStub.invoke())
        	.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", "AccessDenied", "Test Invoke with AccessDenied exception!"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.GET_HOSTED_ZONE), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(getHostedZoneMethodStub);
        
        Route53Method listHostedZonesMethodStub = mock(Route53Method.class);
        when(listHostedZonesMethodStub.invoke())
	    	.thenReturn(resource(
	            "org/dasein/cloud/aws/network/route53/list_hosted_zones.xml"));	
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(listHostedZonesMethodStub);
        
        assertReflectionEquals(
        		createDNSZone("Z2682N5HXP0BZ4", "example3.com.", "example3.com.", "This is my third hosted zone.", ACCOUNT_NO), 
        		route53.getDnsZone(providerDnsZoneId));
	}
	
	@Test
	public void getDnsZoneErrorInvalidInputShouldReturnCorrectResult() throws Exception {
		
		String providerDnsZoneId = "Z2682N5INVALID";
		
		Route53Method getHostedZoneMethodStub = mock(Route53Method.class);
        when(getHostedZoneMethodStub.invoke())
        	.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", "InvalidInput", "Test Invoke with InvalidInput exception!"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.GET_HOSTED_ZONE), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(getHostedZoneMethodStub);
        
        Route53Method listHostedZonesMethodStub = mock(Route53Method.class);
        when(listHostedZonesMethodStub.invoke())
        	.thenReturn(resource(
                "org/dasein/cloud/aws/network/route53/list_hosted_zones.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(listHostedZonesMethodStub);
        
        assertNull(route53.getDnsZone(providerDnsZoneId));
	}
	
	@Test
	public void listDnsZonesShouldReturnCorrectResult() throws Exception {
		
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke()).thenReturn(resource(
        		"org/dasein/cloud/aws/network/route53/list_hosted_zones.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		
		assertReflectionEquals(
				Arrays.asList(
						createDNSZone("Z222222VVVVVVV", "example2.com.", "example2.com.", "This is my second hosted zone.", ACCOUNT_NO),
						createDNSZone("Z2682N5HXP0BZ4", "example3.com.", "example3.com.", "This is my third hosted zone.", ACCOUNT_NO),
						createDNSZone("Z1D633PJN98FT9", "example.com.", "example.com.", "This is my first hosted zone.", ACCOUNT_NO)), 
				route53.listDnsZones());
	}
	
	@Test
	public void listDnsZonesCrossPageShouldReturnCorrectResult() throws Exception {
		
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/route53/list_hosted_zones_first_page.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/route53/list_hosted_zones_second_page.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		
		assertReflectionEquals(
				Arrays.asList(
						createDNSZone("Z222222VVVVVVV", "example2.com.", "example2.com.", "This is my second hosted zone.", ACCOUNT_NO),
						createDNSZone("Z2682N5HXP0BZ4", "example3.com.", "example3.com.", "This is my third hosted zone.", ACCOUNT_NO),
						createDNSZone("Z1D633PJN98FT9", "example.com.", "example.com.", "This is my first hosted zone.", ACCOUNT_NO)), 
				route53.listDnsZones());
	}
	
	@Test
	public void listDnsZoneStatusShouldReturnCorrectResult() throws Exception {
		
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke()).thenReturn(resource(
        		"org/dasein/cloud/aws/network/route53/list_hosted_zones.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
        
        assertReflectionEquals(
        		Arrays.asList(
        				new ResourceStatus("Z222222VVVVVVV", Boolean.TRUE),
        				new ResourceStatus("Z2682N5HXP0BZ4", Boolean.TRUE),
        				new ResourceStatus("Z1D633PJN98FT9", Boolean.TRUE)),
        		route53.listDnsZoneStatus());
	}
	
	@Test
	public void listDnsZoneStatusCrossPageShouldReturnCorrectResult() throws Exception {
		
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/route53/list_hosted_zones_first_page.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/route53/list_hosted_zones_second_page.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
        
        assertReflectionEquals(
        		Arrays.asList(
        				new ResourceStatus("Z222222VVVVVVV", Boolean.TRUE),
        				new ResourceStatus("Z2682N5HXP0BZ4", Boolean.TRUE),
        				new ResourceStatus("Z1D633PJN98FT9", Boolean.TRUE)),
        		route53.listDnsZoneStatus());
	}
	
	@Test
	public void listDnsRecordsShouldReturnCorrectResult() throws Exception {
		
		String providerDnsZoneId = "Z2682N5HXP0BZ4";
		
		Route53Method getHostedZoneMethodStub = mock(Route53Method.class);
        when(getHostedZoneMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/route53/get_hosted_zone.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.GET_HOSTED_ZONE), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(getHostedZoneMethodStub);
        
        Route53Method listRecordSetsMethodStub = mock(Route53Method.class);
        when(listRecordSetsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/route53/list_record_sets.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_RESOURCE_RECORD_SETS), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(listRecordSetsMethodStub);
        
        assertReflectionEquals(
        		Arrays.asList(
        				createDNSRecord(providerDnsZoneId, "example1.com.", 900, DNSRecordType.A, "192.0.2.3"),
        				createDNSRecord(providerDnsZoneId, "example2.com.", 900, DNSRecordType.SOA, "ns-2048.awsdns-64.net. hostmaster.awsdns.com. 1 7200 900 1209600 86400"),
        				createDNSRecord(providerDnsZoneId, "example3.com.", 172800, DNSRecordType.NS, 
        						"ns-2048.awsdns-64.com.", "ns-2049.awsdns-65.net.", "ns-2050.awsdns-66.org.", "ns-2051.awsdns-67.co.uk.")),
        		route53.listDnsRecords(providerDnsZoneId, null, null));
	}
	
	@Test
	public void listDnsRecordsCrossPageShouldReturnCorrectResult() throws Exception {
		String providerDnsZoneId = "Z2682N5HXP0BZ4";
		
		Route53Method getHostedZoneMethodStub = mock(Route53Method.class);
        when(getHostedZoneMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/route53/get_hosted_zone.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.GET_HOSTED_ZONE), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(getHostedZoneMethodStub);
        
        Route53Method listRecordSetsMethodStub = mock(Route53Method.class);
        when(listRecordSetsMethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/route53/list_record_sets_first_page.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/route53/list_record_sets_second_page.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_RESOURCE_RECORD_SETS), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(listRecordSetsMethodStub);
        
        assertReflectionEquals(
        		Arrays.asList(
        				createDNSRecord(providerDnsZoneId, "example1.com.", 900, DNSRecordType.A, "192.0.2.3"),
        				createDNSRecord(providerDnsZoneId, "example2.com.", 900, DNSRecordType.SOA, "ns-2048.awsdns-64.net. hostmaster.awsdns.com. 1 7200 900 1209600 86400"),
        				createDNSRecord(providerDnsZoneId, "example3.com.", 172800, DNSRecordType.NS, 
        						"ns-2048.awsdns-64.com.", "ns-2049.awsdns-65.net.", "ns-2050.awsdns-66.org.", "ns-2051.awsdns-67.co.uk.")),
        		route53.listDnsRecords(providerDnsZoneId, null, null));
	}
	
	@Test
	public void getProviderTermForRecordShouldReturnCorrectResult() {
		assertEquals("resource", route53.getProviderTermForRecord(null));
	}
	
	@Test
	public void getProviderTermForZoneShouldReturnCorrectResult() {
		assertEquals("hosted zone", route53.getProviderTermForZone(null));
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResult() throws Exception {
        assertTrue(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfUnAuthorized() throws Exception {
        assertFalse(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfForbidden() throws Exception {
        assertFalse(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfSubscriptionCheckFailed() throws Exception {
        assertFalse(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfAuthFailure() throws Exception {
        assertFalse(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfSignatureDoesNotMatch() throws Exception {
        assertFalse(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfInvalidClientTokenId() throws Exception {
        assertFalse(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfOptInRequired() throws Exception {
        assertFalse(route53.isSubscribed());
	}
	
	@Test(expected = CloudException.class)
	public void isSubscribedShouldThrowExceptionIfUnknownException() throws Exception {
        route53.isSubscribed();
	}
	
	@Test
	public void addDnsRecordShouldPostWithCorrectRequest() throws Exception {
		
		String providerDnsZoneId = "Z1D633PJN98FT9";
		String recordName = "example1.com.";
		int recordTTL = 900;
		String[] recordValues = Arrays.asList("192.0.2.3").toArray(new String[1]);
		
		PowerMockito.doCallRealMethod().when(awsCloudStub).getRoute53Version();
		
		Route53Method getHostedZoneMethodStub = mock(Route53Method.class);
        when(getHostedZoneMethodStub.invoke())
        	.thenReturn(resource(
        		"org/dasein/cloud/aws/network/route53/get_hosted_zone.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.GET_HOSTED_ZONE), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(getHostedZoneMethodStub);
        
        Route53Method listRecordSetsMethodStub = mock(Route53Method.class);
        when(listRecordSetsMethodStub.invoke())
        	.thenReturn(resource(
        		"org/dasein/cloud/aws/network/route53/list_empty_record_sets.xml"))
    		.thenReturn(resource(
    			"org/dasein/cloud/aws/network/route53/list_record_sets.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_RESOURCE_RECORD_SETS), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(listRecordSetsMethodStub);
        
        Route53Method changeRecordSetsMethodStub = mock(Route53Method.class);
        when(changeRecordSetsMethodStub.invoke(Mockito.anyString()))
    		.thenReturn(resource("org/dasein/cloud/aws/network/route53/change_basic_record_sets.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.CHANGE_RESOURCE_RECORD_SETS), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(changeRecordSetsMethodStub);
        
        assertReflectionEquals(
        		createDNSRecord("Z1D633PJN98FT9", recordName, recordTTL, DNSRecordType.A, recordValues), 
        		route53.addDnsRecord(providerDnsZoneId, DNSRecordType.A, recordName, recordTTL, recordValues));
	}
	
	@Test
	public void createDnsZoneShouldPostWithCorrectRequest() throws Exception {
		
		String domainName = "example.com";
		String name = "example.com.";
		String description = "This is my first hosted zone.";
		
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke(Mockito.anyString()))
        	.thenReturn(resource(
        		"org/dasein/cloud/aws/network/route53/create_hosted_zone.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.CREATE_HOSTED_ZONE), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
        
		assertEquals(
				"Z1PA6795UKMFR9", 
				route53.createDnsZone(domainName, name, description));
	}
	
	@Test
	public void deleteDnsRecordsShouldPostWithCorrectRequest() throws Exception {
		
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke(Mockito.anyString()))
        	.thenReturn(resource(
        		"org/dasein/cloud/aws/network/route53/create_hosted_zone.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.CHANGE_RESOURCE_RECORD_SETS), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);

        DNSRecord record = new DNSRecord();
        record.setProviderZoneId("Z1D633PJN98FT9");
        record.setName("example1.com.");
        record.setTtl(900);
        record.setType(DNSRecordType.A);
        record.setValues(Arrays.asList("192.0.2.3").toArray(new String[1]));
        
        route53.deleteDnsRecords(Arrays.asList(record).toArray(new DNSRecord[1]));
	}
	
	@Test
	public void deleteDnsZoneShouldDeleteWithCorrectRequest() throws Exception {
		
		String providerDnsZoneId = "Z1D633PJN98FT9";
		
		Route53Method route53MethodStub = mock(Route53Method.class);
		when(route53MethodStub.invoke())
    		.thenReturn(resource("org/dasein/cloud/aws/network/route53/delete_hosted_zone.xml"));
	    PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.DELETE_HOSTED_ZONE), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		
		route53.deleteDnsZone(providerDnsZoneId);
	}
	
	private DNSZone createDNSZone(String providerDnsZoneId, String name, String domainName, String description, String providerOwnerId, String ... nameServers) {
		DNSZone dnsZone = new DNSZone();
		dnsZone.setProviderDnsZoneId(providerDnsZoneId);
		dnsZone.setName(name);
		dnsZone.setDomainName(domainName);
		dnsZone.setDescription(description);
		dnsZone.setProviderOwnerId(providerOwnerId);
		dnsZone.setNameservers(nameServers);
		return dnsZone;
	}
	
	private DNSRecord createDNSRecord(String providerDnsZoneId, String name, int ttl, DNSRecordType type, String ... values) {
		DNSRecord record = new DNSRecord();
		record.setProviderZoneId(providerDnsZoneId);
		record.setName(name);
		record.setTtl(ttl);
		record.setType(type);
		record.setValues(values);
        return record;
	}
}
