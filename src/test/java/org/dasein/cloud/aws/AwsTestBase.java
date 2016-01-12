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

package org.dasein.cloud.aws;

import org.dasein.cloud.Cloud;
import org.dasein.cloud.ProviderContext;
import org.junit.Before;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

/**
 * Created by Jeffrey Yan on 1/7/2016.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class AwsTestBase {

    protected final String ENDPOINT = "https://ec2.amazonaws.com";
    protected final String ACCOUNT_NO = "123456789012";
    protected final String REGION = "us-west";

    protected AWSCloud awsCloudStub;
    protected ProviderContext providerContextStub;
    protected Cloud cloudMock;

    @Before
    public void setUp() {
        awsCloudStub = PowerMockito.spy(new AWSCloud());
        providerContextStub = Mockito.mock(ProviderContext.class);
        cloudMock = Mockito.mock(Cloud.class);

        //PowerMockito.when(awsCloudStub.getContext()).thenReturn(null);
//        PowerMockito.doReturn(ENDPOINT).when(awsCloudStub).getEc2Url();
        PowerMockito.doReturn(providerContextStub).when(awsCloudStub).getContext();

        Mockito.doReturn(cloudMock).when(providerContextStub).getCloud();
        Mockito.doReturn(ACCOUNT_NO).when(providerContextStub).getAccountNumber();
        Mockito.doReturn(REGION).when(providerContextStub).getRegionId();
        Mockito.doReturn(ENDPOINT).when(providerContextStub).getEndpoint();

        Mockito.doReturn(ENDPOINT).when(cloudMock).getEndpoint();
    }
}
