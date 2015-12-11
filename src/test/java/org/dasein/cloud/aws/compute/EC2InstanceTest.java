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

import org.dasein.cloud.aws.AWSCloud;
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

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;

/**
 * Created by Jeffrey Yan on 11/19/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSCloud.class, EC2Instance.class})
public class EC2InstanceTest {

    private AWSCloud awsCloudStub;
    private EC2Instance ec2Instance;

    @Before
    public void setUp() {
        awsCloudStub = PowerMockito.spy(new AWSCloud());
        //PowerMockito.when(awsCloudStub.getContext()).thenReturn(null);
        PowerMockito.doReturn("https://ec2.amazonaws.com").when(awsCloudStub).getEc2Url();

        ec2Instance = new EC2Instance(awsCloudStub);
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
        Mockito.when(ec2MethodStub.invoke()).thenReturn(resource("get_password.xml"));

        PowerMockito.whenNew(EC2Method.class)
                .withArguments(eq(awsCloudStub), argThat(allOf(hasEntry("InstanceId", instanceId), hasEntry("Action", "GetPasswordData"))))
                .thenReturn(ec2MethodStub);

        assertEquals("TGludXggdmVyc2lvbiAyLjYuMTYteGVuVSAoYnVpbGRlckBwYXRjaGJhdC5hbWF6b25zYSkgKGdj",
                ec2Instance.getPassword(instanceId));
    }
}
