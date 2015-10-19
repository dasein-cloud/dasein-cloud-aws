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

import org.dasein.cloud.AbstractCapabilities;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.identity.CloudPolicyType;
import org.dasein.cloud.identity.IdentityAndAccessCapabilities;
import org.dasein.cloud.util.NamingConstraints;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Locale;

/**
 * Created by stas on 18/06/15.
 */
public class IAMCapabilities extends AbstractCapabilities<AWSCloud> implements IdentityAndAccessCapabilities {
    public IAMCapabilities(AWSCloud provider) {
        super(provider);
    }

    @Override
    public boolean supportsAccessControls() throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean supportsConsoleAccess() throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean supportsApiAccess() throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean supportsPolicies() throws CloudException, InternalException {
        return true;
    }

    @Nullable
    @Override
    public String getConsoleUrl() throws CloudException, InternalException {
        return String.format("https://%s.signin.aws.amazon.com/console", getContext().getAccountNumber());
    }

    @Nonnull
    @Override
    public String getProviderTermForUser(Locale locale) {
        return "user";
    }

    @Nonnull
    @Override
    public String getProviderTermForGroup(@Nonnull Locale locale) {
        return "group";
    }

    @Nonnull
    @Override
    public String getProviderTermForPolicy(@Nonnull Locale locale) {
        return "policy";
    }

    @Nonnull
    @Override
    public NamingConstraints getPasswordConstraints() throws CloudException, InternalException {
        return NamingConstraints.getAlphaNumeric(1, 128);
    }

    @Nonnull
    @Override
    public Iterable<CloudPolicyType> listSupportedPolicyTypes() throws CloudException, InternalException {
        return Arrays.asList(
                CloudPolicyType.ACCOUNT_MANAGED_POLICY,
                CloudPolicyType.PROVIDER_MANAGED_POLICY,
                CloudPolicyType.INLINE_POLICY
        );
    }

    @Override
    public int getMaximumPoliciesPerUser() throws CloudException, InternalException {
        return 10;
    }

    @Override
    public int getMaximumPoliciesPerGroup() throws CloudException, InternalException {
        return 10;
    }

    @Override
    public int getMaximumGroupsPerUser() throws CloudException, InternalException {
        return 100;
    }

    @Override
    public int getMaximumAccessKeysPerUser() throws CloudException, InternalException {
        return 2;
    }

    @Override
    public int getMaximumUsers() throws CloudException, InternalException {
        return 1000;
    }

    @Override
    public int getMaximumGroups() throws CloudException, InternalException {
        return 1000;
    }
}
