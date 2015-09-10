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

package org.dasein.cloud.aws.network;

import org.dasein.cloud.*;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.network.VpnCapabilities;
import org.dasein.cloud.network.VpnProtocol;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;

/**
 * Describes the capabilities of AWS with respect to Dasein VPN operations.
 * <p>Created by Stas Maksimov: 10/03/2014 00:48</p>
 *
 * @author Stas Maksimov
 * @version 2014.03 initial version
 * @since 2014.03
 */
public class VPCGatewayCapabilities extends AbstractCapabilities<AWSCloud> implements VpnCapabilities {
    public VPCGatewayCapabilities(AWSCloud provider) {
        super(provider);
    }


    @Nonnull
    @Override
    public Iterable<VpnProtocol> listSupportedVpnProtocols() throws CloudException, InternalException {
        return Collections.singletonList(VpnProtocol.IPSEC1);
    }

    @Nullable
    @Override
    public VisibleScope getVpnVisibleScope() {
        return VisibleScope.ACCOUNT_REGION;
    }

    @Override
    public Requirement identifyLabelsRequirement() throws CloudException, InternalException {
        return null;
    }

    @Override
    public Requirement identifyVlanIdRequirement() throws CloudException, InternalException {
        return null;
    }

    @Override
    public Requirement identifyDataCenterIdRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement identifyGatewayCidrRequirement() throws CloudException, InternalException {
        return null;
    }

    @Override
    public Requirement identifyGatewaySharedSecretRequirement() throws CloudException, InternalException {
        return null;
    }

    @Override
    public Requirement identifyGatewayBgpAsnRequirement() throws CloudException, InternalException {
        return null;
    }

    @Override
    public Requirement identifyGatewayVlanNameRequirement() throws CloudException, InternalException {
        return null;
    }

    @Override
    public Requirement identifyGatewayVpnNameRequirement() throws CloudException, InternalException {
        return null;
    }

    @Override
    public boolean supportsAutoConnect() throws CloudException, InternalException {
        return false;
    }
}
