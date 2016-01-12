/*
 *  *
 *  Copyright (C) 2009-2015 Dell, Inc.
 *  See annotations for authorship information
 *
 *  ====================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ====================================================================
 *
 */

package org.dasein.cloud.aws.compute;

import org.dasein.cloud.AbstractProviderService;
import org.dasein.cloud.Cloud;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineProductFilterOptions;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.method;

/**
 * Created by Jeffrey Yan on 11/19/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSCloud.class, EC2Instance.class, AbstractProviderService.class})
public class EC2InstanceTest {

    private AWSCloud awsCloudStub;
    private EC2Instance ec2Instance, ec2InstanceMock;
    private ProviderContext context;

    @Before
    public void setUp() {
        awsCloudStub = PowerMockito.spy(new AWSCloud());
        //PowerMockito.when(awsCloudStub.getContext()).thenReturn(null);
        PowerMockito.doReturn("https://ec2.amazonaws.com").when(awsCloudStub).getEc2Url();

        ec2Instance = PowerMockito.spy(new EC2Instance(awsCloudStub));//new EC2Instance(awsCloudStub);
        ec2InstanceMock = PowerMockito.mock(EC2Instance.class);
        context = PowerMockito.mock(ProviderContext.class);
        Cloud cloud = Cloud.register("AWS", "AWS", "test-endpoint.awscloud.com", AWSCloud.class);
        PowerMockito.doReturn(cloud).when(context).getCloud();
    }

    private Document resource(String resourceName) throws Exception {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return documentBuilder
                .parse(getClass().getClassLoader().getResourceAsStream("org/dasein/cloud/aws/compute/" + resourceName));
    }

    @Test
    public void testGetPassword() throws Exception {
        String instanceId = "i-2574e22a";

        EC2Method ec2MethodStub = Mockito.mock(EC2Method.class);
        when(ec2MethodStub.invoke()).thenReturn(resource("get_password.xml"));

        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(hasEntry("InstanceId", instanceId), hasEntry("Action", "GetPasswordData"))))
                .thenReturn(ec2MethodStub);

        assertEquals("TGludXggdmVyc2lvbiAyLjYuMTYteGVuVSAoYnVpbGRlckBwYXRjaGJhdC5hbWF6b25zYSkgKGdj",
                ec2Instance.getPassword(instanceId));
    }

    /**
     * Tests that the list of *all* products is not empty (FB8437).
     * 
     * @throws Exception
     */
    @Test
    public void testListAllProducts() throws Exception {
        when(ec2InstanceMock.listAllProducts()).thenCallRealMethod();
        when(ec2InstanceMock.listProducts(any(VirtualMachineProductFilterOptions.class), any(Architecture.class))).thenCallRealMethod();
        when(ec2InstanceMock.toProduct(any(JSONObject.class))).thenCallRealMethod();
        PowerMockito.when(ec2InstanceMock, method(AbstractProviderService.class, "getProvider")).withNoArguments().thenReturn(awsCloudStub);
        PowerMockito.when(ec2InstanceMock, method(EC2Instance.class, "getContext")).withNoArguments().thenReturn(context);
        Iterable<VirtualMachineProduct> products = ec2InstanceMock.listAllProducts();
        int count = 0;
        for( VirtualMachineProduct p : products ) {
            count ++;
        }
        assertThat("Product count should be greater than zero", count, greaterThan(0));
    }
}
