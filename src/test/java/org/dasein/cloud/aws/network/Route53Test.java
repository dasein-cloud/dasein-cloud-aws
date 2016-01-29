package org.dasein.cloud.aws.network;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AwsTestBase;
import org.dasein.cloud.aws.compute.EC2Exception;
import org.dasein.cloud.aws.compute.EC2Instance;
import org.dasein.cloud.network.DNSRecord;
import org.dasein.cloud.network.DNSRecordType;
import org.dasein.cloud.network.DNSZone;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSCloud.class, EC2Instance.class, Route53.class})
public class Route53Test extends AwsTestBase {

	private Route53 route53;
	
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
		
		DNSZone response = route53.getDnsZone(providerDnsZoneId);
		assertEquals("Z1D633PJN98FT9", response.getProviderDnsZoneId());
		assertEquals("example.com.", response.getName());
		assertEquals("example.com.", response.getDomainName());
		assertEquals("This is my first hosted zone.", response.getDescription());
		assertEquals(ACCOUNT_NO, response.getProviderOwnerId());
		assertArrayEquals(new String[] {
				"ns-2048.awsdns-64.com", 
				"ns-2049.awsdns-65.net",
				"ns-2050.awsdns-66.org", 
				"ns-2051.awsdns-67.co.uk"}, 
				response.getNameservers());
	}
	
	@Test
	public void getDnsZoneErrorNotFoundSuchHostedZoneShouldReturnCorrectResult() throws Exception {
		
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

		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", "AccessDenied", "Test Invoke with AccessDenied exception!"))
        	.thenReturn(resource(
                "org/dasein/cloud/aws/network/route53/list_hosted_zones.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.GET_HOSTED_ZONE), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
        
        DNSZone response = route53.getDnsZone(providerDnsZoneId);
		assertEquals("Z2682N5HXP0BZ4", response.getProviderDnsZoneId());
		assertEquals("example3.com.", response.getName());
		assertEquals("example3.com.", response.getDomainName());
		assertEquals("This is my third hosted zone.", response.getDescription());
		assertEquals(ACCOUNT_NO, response.getProviderOwnerId());
		assertEquals(0, response.getNameservers().length);
	}
	
	@Test
	public void getDnsZoneErrorInvalidInputShouldReturnCorrectResult() throws Exception {
		
		String providerDnsZoneId = "Z2682N5INVALID";
		
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", "InvalidInput", "Test Invoke with InvalidInput exception!"))
        	.thenReturn(resource(
                "org/dasein/cloud/aws/network/route53/list_hosted_zones.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.GET_HOSTED_ZONE), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
        
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
		
        Iterator<DNSZone> dnsZonesIter = route53.listDnsZones().iterator();
        
		DNSZone dnsZone1 = dnsZonesIter.next();
		assertEquals("Z222222VVVVVVV", dnsZone1.getProviderDnsZoneId());
		assertEquals("example2.com.", dnsZone1.getName());
		assertEquals("example2.com.", dnsZone1.getDomainName());
		assertEquals("This is my second hosted zone.", dnsZone1.getDescription());
		assertEquals(0, dnsZone1.getNameservers().length);
		assertEquals(ACCOUNT_NO, dnsZone1.getProviderOwnerId());
		
		DNSZone dnsZone2 = dnsZonesIter.next();
		assertEquals("Z2682N5HXP0BZ4", dnsZone2.getProviderDnsZoneId());
		assertEquals("example3.com.", dnsZone2.getName());
		assertEquals("example3.com.", dnsZone2.getDomainName());
		assertEquals("This is my third hosted zone.", dnsZone2.getDescription());
		assertEquals(0, dnsZone2.getNameservers().length);
		assertEquals(ACCOUNT_NO, dnsZone2.getProviderOwnerId());
		
		DNSZone dnsZone3 = dnsZonesIter.next();
		assertEquals("Z1D633PJN98FT9", dnsZone3.getProviderDnsZoneId());
		assertEquals("example.com.", dnsZone3.getName());
		assertEquals("example.com.", dnsZone3.getDomainName());
		assertEquals("This is my first hosted zone.", dnsZone3.getDescription());
		assertEquals(0, dnsZone3.getNameservers().length);
		assertEquals(ACCOUNT_NO, dnsZone3.getProviderOwnerId());
		
		assertFalse(dnsZonesIter.hasNext());
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
		
        Iterator<ResourceStatus> dnsZoneStatusesIter = route53.listDnsZoneStatus().iterator();
        
        ResourceStatus resourceStatus1 = dnsZoneStatusesIter.next();
        assertEquals("Z222222VVVVVVV", resourceStatus1.getProviderResourceId());
        assertEquals(Boolean.TRUE, resourceStatus1.getResourceStatus());
        
        ResourceStatus resourceStatus2 = dnsZoneStatusesIter.next();
        assertEquals("Z2682N5HXP0BZ4", resourceStatus2.getProviderResourceId());
        assertEquals(Boolean.TRUE, resourceStatus2.getResourceStatus());
        
        ResourceStatus resourceStatus3 = dnsZoneStatusesIter.next();
        assertEquals("Z1D633PJN98FT9", resourceStatus3.getProviderResourceId());
        assertEquals(Boolean.TRUE, resourceStatus3.getResourceStatus());
        
        assertFalse(dnsZoneStatusesIter.hasNext());
	}
	
	@Test
	public void listDnsRecordsShouldReturnCorrectResult() throws Exception {
		
		String providerDnsZoneId = "Z2682N5HXP0BZ4";
		
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/route53/get_hosted_zone.xml"))
        	.thenReturn(resource("org/dasein/cloud/aws/network/route53/list_record_sets.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.GET_HOSTED_ZONE), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_RESOURCE_RECORD_SETS), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
        
        Iterator<DNSRecord> iterator = route53.listDnsRecords(providerDnsZoneId, null, null).iterator();
       
        DNSRecord record1 = iterator.next();
        assertEquals(providerDnsZoneId, record1.getProviderZoneId());
        assertEquals("example1.com.", record1.getName());
        assertEquals(900, record1.getTtl());
        assertEquals(DNSRecordType.A, record1.getType());
        assertArrayEquals(new String[] {"192.0.2.3"}, record1.getValues());
        
        DNSRecord record2 = iterator.next();
        assertEquals(providerDnsZoneId, record2.getProviderZoneId());
        assertEquals("example2.com.", record2.getName());
        assertEquals(900, record2.getTtl());
        assertEquals(DNSRecordType.SOA, record2.getType());
        assertArrayEquals(
        		new String[] {"ns-2048.awsdns-64.net. hostmaster.awsdns.com. 1 7200 900 1209600 86400"}, 
        		record2.getValues());
        
        DNSRecord record3 = iterator.next();
        assertEquals(providerDnsZoneId, record3.getProviderZoneId());
        assertEquals("example3.com.", record3.getName());
        assertEquals(172800, record3.getTtl());
        assertEquals(DNSRecordType.NS, record3.getType());
        assertArrayEquals(
        		new String[] {
        				"ns-2048.awsdns-64.com.",
        				"ns-2049.awsdns-65.net.",
        				"ns-2050.awsdns-66.org.",
        				"ns-2051.awsdns-67.co.uk."}, 
        		record3.getValues());
        
        assertFalse(iterator.hasNext());
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
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenReturn(resource("org/dasein/cloud/aws/network/route53/list_hosted_zones.xml"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		assertTrue(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfSC_UNAUTHORIZED() throws Exception {
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenThrow(EC2Exception.create(401, "410c2a4b-e435-49c9-8382-3770d80d7d4c", "SC_UNAUTHORIZED", "SC_UNAUTHORIZED"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		assertFalse(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfSC_FORBIDDEN() throws Exception {
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenThrow(EC2Exception.create(403, "410c2a4b-e435-49c9-8382-3770d80d7d4c", "SC_FORBIDDEN", "SC_FORBIDDEN"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		assertFalse(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfSubscriptionCheckFailed() throws Exception {
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", "SubscriptionCheckFailed", "SubscriptionCheckFailed"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		assertFalse(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfAuthFailure() throws Exception {
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", "AuthFailure", "AuthFailure"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		assertFalse(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfSignatureDoesNotMatch() throws Exception {
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", "SignatureDoesNotMatch", "SignatureDoesNotMatch"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		assertFalse(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfInvalidClientTokenId() throws Exception {
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", "InvalidClientTokenId", "InvalidClientTokenId"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		assertFalse(route53.isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnCorrectResultIfOptInRequired() throws Exception {
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", "OptInRequired", "OptInRequired"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		assertFalse(route53.isSubscribed());
	}
	
	@Test(expected = CloudException.class)
	public void isSubscribedShouldThrowExceptionIfUnknownException() throws Exception {
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenThrow(EC2Exception.create(404, "410c2a4b-e435-49c9-8382-3770d80d7d4c", "UnknownException", "UnknownException"));
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_HOSTED_ZONES), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		route53.isSubscribed();
	}
	
	@Test
	public void addDnsRecordShouldPostWithCorrectRequest() throws Exception {
		
		String providerDnsZoneId = "Z1D633PJN98FT9";
		String recordName = "example1.com.";
		int recordTTL = 900;
		String[] recordValues = Arrays.asList("192.0.2.3").toArray(new String[1]);
		
		Route53Method route53MethodStub = mock(Route53Method.class);
        when(route53MethodStub.invoke())
        	.thenReturn(resource(
        		"org/dasein/cloud/aws/network/route53/get_hosted_zone.xml"))
        	.thenReturn(resource(
        		"org/dasein/cloud/aws/network/route53/get_hosted_zone.xml"))
        	.thenReturn(resource(
        		"org/dasein/cloud/aws/network/route53/list_empty_record_sets.xml"))
        	.thenReturn(resource(
        		"org/dasein/cloud/aws/network/route53/get_hosted_zone.xml"))
            .thenReturn(resource(
        		"org/dasein/cloud/aws/network/route53/list_record_sets.xml"));
        when(route53MethodStub.invoke(Mockito.anyString()))
        	.thenReturn(resource("org/dasein/cloud/aws/network/route53/change_basic_record_sets.xml"));
        
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.GET_HOSTED_ZONE), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.LIST_RESOURCE_RECORD_SETS), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
        PowerMockito.whenNew(Route53Method.class)
			.withArguments(
				eq(Route53Method.CHANGE_RESOURCE_RECORD_SETS), 
				eq(awsCloudStub), 
				Mockito.anyString())
			.thenReturn(route53MethodStub);
		
		DNSRecord dnsRecord = route53.addDnsRecord(providerDnsZoneId, DNSRecordType.A, recordName, recordTTL, recordValues);
		assertEquals("Z1D633PJN98FT9", dnsRecord.getProviderZoneId());
		assertEquals(recordName, dnsRecord.getName());
		assertEquals(recordTTL, dnsRecord.getTtl());
		assertEquals(DNSRecordType.A, dnsRecord.getType());
		assertArrayEquals(recordValues, dnsRecord.getValues());
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
}
