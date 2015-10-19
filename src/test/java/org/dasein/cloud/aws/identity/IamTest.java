/**
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.identity.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * @author Stas Maksimov (stas.maksimov@software.dell.com)
 * @since 2015.09
 */
@RunWith(JUnit4.class)
public class IamTest {

    private @Nullable Document fixture(@Nonnull String fixtureFilename) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(getClass().getClassLoader().getResourceAsStream("fixtures/identity/"+fixtureFilename));
        } catch (ParserConfigurationException|SAXException|IOException e) {
            e.printStackTrace();
        }
        return null;
    }

//    @Test
//    public void toManagedPolicyTest() {
//        IAM identity = mock(IAM.class, CALLS_REAL_METHODS);
//
//        Document doc = fixture("list_policies.xml");
//        assumeNotNull(doc);
//
//        NodeList blocks = doc.getElementsByTagName("member");
//        assumeTrue(blocks.getLength() > 0);
//
//        CloudPolicy policy = identity.toManagedPolicy(blocks.item(0));
//        assertEquals(policy.getName(), "AdministratorAccess");
//        assertEquals(policy.getProviderPolicyId(), "arn:aws:iam::aws:policy/AdministratorAccess");
//    }

    @Test
    public void listManagedPoliciesTest() {
        IAM identity = mock(IAM.class);
        try {
            Document doc1 = fixture("list_policies.xml");
            Document doc2 = fixture("list_policies_cont.xml");

            when(identity.invoke(eq(IAMMethod.LIST_POLICIES), anyMap()))
                    .thenReturn(doc1) // first page
                    .thenReturn(doc2); // second page
            when(identity.listManagedPolicies(anyString()))
                    .thenCallRealMethod();

            Iterable<CloudPolicy> policies = identity.listManagedPolicies("AWS");
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

    @Test
    public void getPolicyTest() {
        IAM identity = mock(IAM.class);
        try {
            when(identity.invoke(eq(IAMMethod.GET_POLICY), anyMap()))
                    .thenReturn(
                            fixture("get_policy.xml")
                    );
            when(identity.getPolicy(anyString(), any(CloudPolicyFilterOptions.class)))
                    .thenCallRealMethod();
            when(identity.getManagedPolicy(anyString()))
                    .thenCallRealMethod();

            CloudPolicy policy = identity.getPolicy("ANPAIWMBCKSKIEE64ZLYK", null);
            assertNotNull("Policy object should not be null", policy);
            assertEquals(policy.getName(), "AdministratorAccess");
            assertEquals(policy.getProviderPolicyId(), "arn:aws:iam::aws:policy/AdministratorAccess");
        } catch (CloudException|InternalException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getPolicyRules() {
        IAM identity = mock(IAM.class);
        try {
            when(identity.invoke(eq(IAMMethod.GET_POLICY_VERSION), anyMap()))
                    .thenReturn(
                            fixture("get_policy_version.xml")
                    );
            when(identity.invoke(eq(IAMMethod.GET_POLICY), anyMap()))
                    .thenReturn(
                            fixture("get_policy.xml")
                    );

            when(identity.getPolicyRules(anyString(), any(CloudPolicyFilterOptions.class)))
                    .thenCallRealMethod();
            when(identity.getManagedPolicyRules(anyString())).thenCallRealMethod();

            CloudPolicyRule[] rules = identity.getPolicyRules("ANPAIWMBCKSKIEE64ZLYK", null);
            assertNotNull("Policy rules array should not be null", rules);
            assertEquals(rules.length, 1);
            assertEquals(rules[0].getPermission(), CloudPermission.ALLOW);
        } catch (CloudException|InternalException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void listServicesTest() {
        IAM identity = mock(IAM.class);
        try {
            when(identity.listServices()).thenCallRealMethod();
            when(identity.readServiceActionsYaml()).thenCallRealMethod();
            Iterable<String> services = identity.listServices();
            assertNotNull("Services iterable should not be null", services);
            int count = 0;
            for( String s : services ) {
                ++count;
            }
            assertEquals("Number of services returned is incorrect", 2, count);
        } catch (CloudException | InternalException e) {
            fail("Unable to execute listServices() successfully: " + e.getMessage());
        }
    }

    @Test
    public void listServiceActionsTest() {
        IAM identity = mock(IAM.class);
        try {
            when(identity.listServiceActions(anyString())).thenCallRealMethod();
            when(identity.readServiceActionsYaml()).thenCallRealMethod();
            Iterable<ServiceAction> serviceActions = identity.listServiceActions(null);
            assertNotNull("Service actions iterable should not be null", serviceActions);
            int count = 0;
            for( ServiceAction s : serviceActions ) {
                ++count;
            }
            assertEquals("Number of service actions returned is incorrect", 6, count);
        } catch (CloudException | InternalException e) {
            fail("Unable to execute listServices() successfully: " + e.getMessage());
        }
    }
}
