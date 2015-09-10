/*
 * Copyright (C) 2009-2015 Dell, Inc.
 * See annotations for authorship information
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.aws.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.*;

import org.dasein.cloud.AbstractProviderService;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.identity.CloudPolicy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Node;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Stas Maksimov (stas.maksimov@software.dell.com)
 * @since 2015.09
 */
@RunWith(JUnit4.class)
public class IamTest {

    private @Nullable Document readFixture(@Nonnull String fixtureFilename) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(getClass().getClassLoader().getResourceAsStream("fixtures/identity/"+fixtureFilename));
        } catch (ParserConfigurationException|SAXException|IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    public void toManagedPolicyTest() {
        IAM identity = mock(IAM.class, CALLS_REAL_METHODS);

        Document doc = readFixture("list_policies.xml");
        assumeNotNull(doc);

        NodeList blocks = doc.getElementsByTagName("member");
        assumeTrue(blocks.getLength() > 0);

        CloudPolicy policy = identity.toManagedPolicy(blocks.item(0));
        assertEquals(policy.getName(), "AdministratorAccess");
        assertEquals(policy.getProviderPolicyId(), "arn:aws:iam::aws:policy/AdministratorAccess");
    }

    @Test
    public void listPoliciesTest() {
        IAM identity = mock(IAM.class);
        try {
            Document doc1 = readFixture("list_policies.xml");
            Document doc2 = readFixture("list_policies_cont.xml");

            when(identity.invoke(eq(IAMMethod.LIST_POLICIES), anyMap()))
                    .thenReturn(doc1) // first page
                    .thenReturn(doc2); // second page
            when(identity.listPolicies())
                    .thenCallRealMethod();
            when(identity.toManagedPolicy(any(Node.class)))
                    .thenCallRealMethod();

            Iterable<CloudPolicy> policies = identity.listPolicies();
            assertNotNull("Policies list should not be null", policies);
            assertTrue("Policies list should not be empty", policies.iterator().hasNext());
            CloudPolicy policy = policies.iterator().next();
            assertEquals(policy.getName(), "AdministratorAccess");
            assertEquals(policy.getProviderPolicyId(), "arn:aws:iam::aws:policy/AdministratorAccess");
            int count = 0;
            for( CloudPolicy p : policies ) {
                ++count;
            }
            assertEquals("Number of policies returned is incorrect", 150, count);

        } catch (CloudException|InternalException e) {
            e.printStackTrace();
        }
    }
}
