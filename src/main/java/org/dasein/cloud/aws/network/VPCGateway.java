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

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.compute.EC2Exception;
import org.dasein.cloud.aws.compute.EC2Method;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.network.*;
import org.dasein.cloud.util.APITrace;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@SuppressWarnings("UnusedDeclaration")
public class VPCGateway extends AbstractVpnSupport<AWSCloud> {
    Logger logger = AWSCloud.getLogger(VPCGateway.class);

    private VPCGatewayCapabilities capabilities;
    
    public VPCGateway(@Nonnull AWSCloud provider) { super(provider); }
    
    @Override
    public void attachToVlan(@Nonnull String providerVpnId, @Nonnull String providerVlanId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "attachVPNToVLAN");
        try {
            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.ATTACH_VPN_GATEWAY);
            EC2Method method;

            parameters.put("VpcId", providerVlanId);
            parameters.put("VpnGatewayId", providerVpnId);
            method = new EC2Method(getProvider(), parameters);
            try {
                method.invoke();
            }
            catch( EC2Exception e ) {
                logger.error(e.getSummary());
                throw new CloudException(e);
            }
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public void connectToGateway(@Nonnull String providerVpnId, @Nonnull String toGatewayId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "connectVPNToGateway");
        try {
            VpnGateway gateway = getGateway(toGatewayId);
            Vpn vpn = getVpn(providerVpnId);

            if( gateway == null ) {
                throw new CloudException("No such VPN gateway: " + toGatewayId);
            }
            if( vpn == null ) {
                throw new CloudException("No such VPN: " + providerVpnId);
            }
            if( !gateway.getProtocol().equals(vpn.getProtocol()) ) {
                throw new CloudException("VPN protocol mismatch between VPN and gateway: " + vpn.getProtocol() + " vs " + gateway.getProtocol());
            }

            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.CREATE_VPN_CONNECTION);
            EC2Method method;

            parameters.put("Type", getAWSProtocol(vpn.getProtocol()));
            parameters.put("CustomerGatewayId", gateway.getProviderVpnGatewayId());
            parameters.put("VpnGatewayId", vpn.getProviderVpnId());
            method = new EC2Method(getProvider(), parameters);
            try {
                method.invoke();
            }
            catch( EC2Exception e ) {
                logger.error(e.getSummary());
                throw new CloudException(e);
            }
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Vpn createVpn(@Nonnull VpnCreateOptions vpnLaunchOptions) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "createVPN");
        try {
            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.CREATE_VPN_GATEWAY);
            EC2Method method;
            NodeList blocks;
            Document doc;

            parameters.put("Type", getAWSProtocol(vpnLaunchOptions.getProtocol()));
            method = new EC2Method(getProvider(), parameters);
            try {
                doc = method.invoke();
            }
            catch( EC2Exception e ) {
                logger.error(e.getSummary());
                throw new CloudException(e);
            }
            blocks = doc.getElementsByTagName("vpnGateway");

            for( int i=0; i<blocks.getLength(); i++ ) {
                Node item = blocks.item(i);
                Vpn vpn = toVPN(item);

                if( vpn != null ) {
                    return vpn;
                }
            }
            throw new CloudException("No VPN was created, but no error was reported");
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull VpnGateway createVpnGateway(@Nonnull VpnGatewayCreateOptions opt) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "createVPNGateway");
        try {
            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.CREATE_CUSTOMER_GATEWAY);
            EC2Method method;
            NodeList blocks;
            Document doc;

            parameters.put("Type", getAWSProtocol(opt.getProtocol()));
            parameters.put("IpAddress", opt.getEndpoint());
            parameters.put("BgpAsn", opt.getBgpAsn());
            method = new EC2Method(getProvider(), parameters);
            try {
                doc = method.invoke();
            }
            catch( EC2Exception e ) {
                logger.error(e.getSummary());
                if( logger.isDebugEnabled() ) {
                    logger.debug("createVpnGateway failed", e);
                }
                throw new CloudException(e);
            }
            blocks = doc.getElementsByTagName("customerGateway");

            for( int i=0; i<blocks.getLength(); i++ ) {
                Node item = blocks.item(i);
                VpnGateway gateway = toGateway(item);

                if( gateway != null ) {
                    return gateway;
                }
            }
            throw new CloudException("No VPN gateway was created, but no error was reported");
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public void deleteVpn(@Nonnull String providerVpnId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "deleteVPN");
        try {
            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.DELETE_VPN_GATEWAY);
            EC2Method method;

            parameters.put("VpnGatewayId", providerVpnId);
            method = new EC2Method(getProvider(), parameters);
            try {
                method.invoke();
            }
            catch( EC2Exception e ) {
                logger.error(e.getSummary());
                throw new CloudException(e);
            }
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public void deleteVpnGateway(@Nonnull String gatewayId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "deleteVPNGateway");
        try {
            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.DELETE_CUSTOMER_GATEWAY);
            EC2Method method;

            parameters.put("CustomerGatewayId", gatewayId);
            method = new EC2Method(getProvider(), parameters);
            try {
                method.invoke();
            }
            catch( EC2Exception e ) {
                logger.error(e.getSummary());
                throw new CloudException(e);
            }
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public void detachFromVlan(@Nonnull String providerVpnId, @Nonnull String providerVlanId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "detachVPNFromVLAN");
        try {
            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.DETACH_VPN_GATEWAY);
            EC2Method method;

            parameters.put("VpcId", providerVlanId);
            parameters.put("VpnGatewayId", providerVpnId);
            method = new EC2Method(getProvider(), parameters);
            try {
                method.invoke();
            }
            catch( EC2Exception e ) {
                logger.error(e.getSummary());
                throw new CloudException(e);
            }
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public void disconnectFromGateway(@Nonnull String vpnId, @Nonnull String gatewayId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "disconnectVPNFromGateway");
        try {
            VpnGateway gateway = getGateway(gatewayId);
            Vpn vpn = getVpn(vpnId);

            if( gateway == null ) {
                throw new CloudException("No such VPN gateway: " + gatewayId);
            }
            if( vpn == null ) {
                throw new CloudException("No such VPN: " + vpnId);
            }
            String connectionId = null;

            for( VpnConnection c : listConnections(vpnId, null) ) {
                if( gatewayId.equals(c.getProviderGatewayId()) ) {
                    connectionId = c.getProviderVpnConnectionId();
                    break;
                }
            }
            if( connectionId == null ) {
                logger.warn("Attempt to disconnect a VPN from a gateway when there was no connection in the cloud");
                return;
            }
            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.DELETE_VPN_CONNECTION);
            EC2Method method;

            parameters.put("VpnConnectionId", connectionId);
            method = new EC2Method(getProvider(), parameters);
            try {
                method.invoke();
            }
            catch( EC2Exception e ) {
                logger.error(e.getSummary());
                throw new CloudException(e);
            }
        }
        finally {
            APITrace.end();
        }
    }

    @Nonnull
    @Override
    public VpnCapabilities getCapabilities() {
        if( capabilities == null ) {
            capabilities = new VPCGatewayCapabilities(getProvider());
        }
        return capabilities;
    }

    private String getAWSProtocol(VpnProtocol protocol) throws CloudException {
        if( protocol.equals(VpnProtocol.IPSEC1) ) {
            return "ipsec.1";
        }
        throw new CloudException("AWS does not support " + protocol);
    }

    @Override
    public @Nullable VpnGateway getGateway(@Nonnull String gatewayId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "getGateway");
        try {
            Iterator<VpnGateway> it = listGateways(gatewayId, null).iterator();

            if( it.hasNext() ) {
                return it.next();
            }
            return null;
        }
        finally {
            APITrace.end();
        }
    }
    
    @Override
    public @Nullable Vpn getVpn(@Nonnull String providerVpnId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "getVPN");
        try {
            Iterator<Vpn> it = listVpns(providerVpnId).iterator();
        
            return (it.hasNext() ? it.next() : null);
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        APITrace.begin(getProvider(), "isSubscribedVPCGateway");
        try {
            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.DESCRIBE_CUSTOMER_GATEWAYS);
            EC2Method method;

            method = new EC2Method(getProvider(), parameters);
            try {
                method.invoke();
                return true;
            }
            catch( EC2Exception e ) {
                if( e.getStatus() == HttpStatus.SC_UNAUTHORIZED || e.getStatus() == HttpStatus.SC_FORBIDDEN ) {
                    return false;
                }
                String code = e.getCode();

                if( code != null && (code.equals("SubscriptionCheckFailed") || code.equals("AuthFailure") || code.equals("SignatureDoesNotMatch") || code.equals("UnsupportedOperation") || code.equals("InvalidClientTokenId") || code.equals("OptInRequired")) ) {
                    return false;
                }
                logger.error(e.getSummary());
                throw new CloudException(e);
            }
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<VpnConnection> listGatewayConnections(@Nonnull String toGatewayId) throws CloudException, InternalException {
        return listConnections(null, toGatewayId);
    }

    private @Nonnull Iterable<VpnConnection> listConnections(@Nullable String vpnId, @Nullable String gatewayId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "listVPCConnections");
        try {
            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.DESCRIBE_VPN_CONNECTIONS);
            EC2Method method;
            NodeList blocks;
            Document doc;

            if( gatewayId != null ) {
                parameters.put("Filter.1.Name", "customer-gateway-id");
                parameters.put("Filter.1.Value.1", gatewayId);
            }
            else if( vpnId != null ) {
                parameters.put("Filter.1.Name", "vpn-gateway-id");
                parameters.put("Filter.1.Value.1", vpnId);
            }
            method = new EC2Method(getProvider(), parameters);
            try {
                doc = method.invoke();
            }
            catch( EC2Exception e ) {
                String code = e.getCode();

                if( code != null ) {
                    if( code.startsWith("InvalidCustomer") || code.startsWith("InvalidVpn") ) {
                        return Collections.emptyList();
                    }
                }
                logger.error(e.getSummary());
                throw new CloudException(e);
            }
            List<VpnConnection> list = new ArrayList();

            blocks = doc.getElementsByTagName("item");
            for( int i=0; i<blocks.getLength(); i++ ) {
                Node item = blocks.item(i);
                VpnConnection c = toConnection(item);

                if( c != null ) {
                    list.add(c);
                }
            }
            return list;
        }
        finally {
            APITrace.end();
        }
    }


    @Override
    public @Nonnull Iterable<ResourceStatus> listGatewayStatus() throws CloudException, InternalException {
        APITrace.begin(getProvider(), "listVPCGatewayStatus");
        try {
            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.DESCRIBE_CUSTOMER_GATEWAYS);
            EC2Method method;
            NodeList blocks;
            Document doc;

            method = new EC2Method(getProvider(), parameters);
            try {
                doc = method.invoke();
            }
            catch( EC2Exception e ) {
                String code = e.getCode();

                if( code != null ) {
                    if( code.startsWith("InvalidCustomer") || code.startsWith("InvalidB") ) {
                        return Collections.emptyList();
                    }
                }
                logger.error(e.getSummary());
                throw new CloudException(e);
            }
            List<ResourceStatus> list = new ArrayList();

            blocks = doc.getElementsByTagName("item");
            for( int i=0; i<blocks.getLength(); i++ ) {
                ResourceStatus status = toGatewayStatus(blocks.item(i));

                if( status != null ) {
                    list.add(status);
                }
            }
            return list;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<VpnGateway> listGateways() throws CloudException, InternalException {
        return listGateways(null, null);
    }
    
    private @Nonnull Iterable<VpnGateway> listGateways(@Nullable String gatewayId, @Nullable String bgpAsn) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "listVPCGateways");
        try {
            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.DESCRIBE_CUSTOMER_GATEWAYS);
            EC2Method method;
            NodeList blocks;
            Document doc;

            if( gatewayId != null ) {
                parameters.put("Filter.1.Name", "customer-gateway-id");
                parameters.put("Filter.1.Value.1", gatewayId);
            }
            else if( bgpAsn != null ) {
                parameters.put("Filter.1.Name", "bgp-asn");
                parameters.put("Filter.1.Value.1", bgpAsn);
            }
            method = new EC2Method(getProvider(), parameters);
            try {
                doc = method.invoke();
            }
            catch( EC2Exception e ) {
                String code = e.getCode();

                if( code != null ) {
                    if( code.startsWith("InvalidCustomer") || code.startsWith("InvalidB") ) {
                        return Collections.emptyList();
                    }
                }
                logger.error(e.getSummary());
                throw new CloudException(e);
            }
            List<VpnGateway> list = new ArrayList();

            blocks = doc.getElementsByTagName("item");
            for( int i=0; i<blocks.getLength(); i++ ) {
                Node item = blocks.item(i);
                VpnGateway gw = toGateway(item);

                if( gw != null ) {
                    list.add(gw);
                }
            }
            return list;
        }
        finally {
            APITrace.end();
        }
    }
    
    @Override
    public @Nonnull Iterable<VpnGateway> listGatewaysWithBgpAsn(@Nonnull String bgpAsn) throws CloudException, InternalException {
        return listGateways(null, bgpAsn);
    }

    @Override
    public @Nonnull Iterable<VpnConnection> listVpnConnections(@Nonnull String toVpnId) throws CloudException, InternalException {
        return listConnections(toVpnId, null);
    }

    @Override
    public @Nonnull Iterable<ResourceStatus> listVpnStatus() throws CloudException, InternalException {
        APITrace.begin(getProvider(), "listVPNStatus");
        try {
            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.DESCRIBE_VPN_GATEWAYS);
            EC2Method method;
            NodeList blocks;
            Document doc;

            method = new EC2Method(getProvider(), parameters);
            try {
                doc = method.invoke();
            }
            catch( EC2Exception e ) {
                String code = e.getCode();

                if( code != null ) {
                    if( code.startsWith("InvalidVpn") ) {
                        return Collections.emptyList();
                    }
                }
                logger.error(e.getSummary());
                throw new CloudException(e);
            }
            List<ResourceStatus> list = new ArrayList();

            blocks = doc.getElementsByTagName("item");
            for( int i=0; i<blocks.getLength(); i++ ) {
                ResourceStatus status = toVPNStatus(blocks.item(i));

                if( status != null ) {
                    list.add(status);
                }
            }
            return list;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<Vpn> listVpns() throws CloudException, InternalException {
        return listVpns(null);
    }

    private @Nonnull Iterable<Vpn> listVpns(@Nullable String vpnId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "listVpns");
        try {
            Map<String,String> parameters = getProvider().getStandardParameters(getContext(), ELBMethod.DESCRIBE_VPN_GATEWAYS);
            EC2Method method;
            NodeList blocks;
            Document doc;

            if( vpnId != null ) {
                parameters.put("VpnGatewayId.1", vpnId);
            }
            method = new EC2Method(getProvider(), parameters);
            try {
                doc = method.invoke();
            }
            catch( EC2Exception e ) {
                String code = e.getCode();

                if( code != null ) {
                    if( code.startsWith("InvalidVpn") ) {
                        return Collections.emptyList();
                    }
                }
                logger.error(e.getSummary());
                throw new CloudException(e);
            }
            List<Vpn> list = new ArrayList();

            blocks = doc.getElementsByTagName("item");
            for( int i=0; i<blocks.getLength(); i++ ) {
                Node item = blocks.item(i);
                Vpn vpn = toVPN(item);

                if( vpn != null ) {
                    list.add(vpn);
                }
            }
            return list;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull String[] mapServiceAction(@Nonnull ServiceAction action) {
        if( action.equals(VpnSupport.ANY) ) {
            return new String[] { EC2Method.EC2_PREFIX + "*" };
        }
        else if( action.equals(VpnSupport.ATTACH) ) {
            return new String[] { EC2Method.EC2_PREFIX + EC2Method.ATTACH_VPN_GATEWAY };
        }
        else if( action.equals(VpnSupport.CREATE_GATEWAY) ) {
            return new String[] { EC2Method.EC2_PREFIX + EC2Method.CREATE_CUSTOMER_GATEWAY };
        }
        else if( action.equals(VpnSupport.CREATE_VPN) ) {
            return new String[] { EC2Method.EC2_PREFIX + EC2Method.CREATE_VPN_GATEWAY };
        }
        else if( action.equals(VpnSupport.GET_GATEWAY) || action.equals(VpnSupport.LIST_GATEWAY) ) {
            return new String[] { EC2Method.EC2_PREFIX + EC2Method.DESCRIBE_CUSTOMER_GATEWAYS, EC2Method.EC2_PREFIX + EC2Method.DESCRIBE_VPN_CONNECTIONS };
        }
        else if( action.equals(VpnSupport.GET_VPN) || action.equals(VpnSupport.LIST_VPN) ) {
            return new String[] { EC2Method.EC2_PREFIX + EC2Method.DESCRIBE_VPN_GATEWAYS, EC2Method.EC2_PREFIX + EC2Method.DESCRIBE_VPN_CONNECTIONS };
        }
        else if( action.equals(VpnSupport.REMOVE_GATEWAY) ) {
            return new String[] { EC2Method.EC2_PREFIX + EC2Method.DELETE_CUSTOMER_GATEWAY };
        }
        else if( action.equals(VpnSupport.REMOVE_VPN) ) {
            return new String[] { EC2Method.EC2_PREFIX + EC2Method.DELETE_VPN_GATEWAY };
        }
        else if( action.equals(VpnSupport.DETACH) ) {
            return new String[] { EC2Method.EC2_PREFIX + EC2Method.DETACH_VPN_GATEWAY };
        }
        return new String[0];
    }
    
    private @Nullable VpnConnection toConnection(@Nullable Node node) throws CloudException, InternalException {
        if( node == null ) {
            return null;
        }

        NodeList attributes = node.getChildNodes();
        VpnConnection connection = new VpnConnection();

        connection.setCurrentState(VpnConnectionState.PENDING);
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attr = attributes.item(i);
            String nodeName = attr.getNodeName();

            if( nodeName.equalsIgnoreCase("vpnConnectionId") && attr.hasChildNodes() ) {
                connection.setProviderVpnConnectionId(attr.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("customerGatewayId") && attr.hasChildNodes() ) {
                connection.setProviderGatewayId(attr.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("vpnGatewayId") && attr.hasChildNodes() ) {
                connection.setProviderVpnId(attr.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("customerGatewayConfiguration") && attr.hasChildNodes() ) {
                connection.setConfigurationXml(attr.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("state") && attr.hasChildNodes() ) {
                String state = attr.getFirstChild().getNodeValue().trim();

                if( state.equalsIgnoreCase("available") ) {
                    connection.setCurrentState(VpnConnectionState.AVAILABLE);
                }
                else if( state.equalsIgnoreCase("deleting") ) {
                    connection.setCurrentState(VpnConnectionState.DELETING);
                }
                else if( state.equalsIgnoreCase("deleted") ) {
                    connection.setCurrentState(VpnConnectionState.DELETED);
                }
                else if( state.equalsIgnoreCase("pending") ) {
                    connection.setCurrentState(VpnConnectionState.PENDING);
                }
                else {
                    logger.warn("DEBUG: Unknown VPN connection state: " + state);
                }
            }
            else if( nodeName.equalsIgnoreCase("type") && attr.hasChildNodes() ) {
                String t = attr.getFirstChild().getNodeValue().trim();

                if( t.equalsIgnoreCase("ipsec.1") ) {
                    connection.setProtocol(VpnProtocol.IPSEC1);
                }
                else if( t.equalsIgnoreCase("openvpn") ) {
                    connection.setProtocol(VpnProtocol.OPEN_VPN);
                }
                else {
                    logger.warn("DEBUG: Unknown VPN connection type: " + t);
                    connection.setProtocol(VpnProtocol.IPSEC1);
                }
            }
        }
        if( connection.getProviderVpnConnectionId() == null ) {
            return null;
        }
        return connection;
    }

    private @Nullable VpnGateway toGateway(@Nullable Node node) throws CloudException, InternalException {
        if( node == null ) {
            return null;
        }

        NodeList attributes = node.getChildNodes();
        VpnGateway gateway = new VpnGateway();
        
        gateway.setProviderOwnerId(getContext().getAccountNumber());
        gateway.setProviderRegionId(getContext().getRegionId());
        gateway.setCurrentState(VpnGatewayState.PENDING);
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attr = attributes.item(i);
            String nodeName = attr.getNodeName();
            
            if( nodeName.equalsIgnoreCase("customerGatewayId") && attr.hasChildNodes() ) {
                gateway.setProviderVpnGatewayId(attr.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("state") && attr.hasChildNodes() ) {
                String state = attr.getFirstChild().getNodeValue().trim();
                
                if( state.equalsIgnoreCase("available") ) {
                    gateway.setCurrentState(VpnGatewayState.AVAILABLE);
                }
                else if( state.equalsIgnoreCase("deleting") ) {
                    gateway.setCurrentState(VpnGatewayState.DELETING);
                }
                else if( state.equalsIgnoreCase("deleted") ) {
                    gateway.setCurrentState(VpnGatewayState.DELETED);
                }
                else if( state.equalsIgnoreCase("pending") ) {
                    gateway.setCurrentState(VpnGatewayState.PENDING);
                }
                else {
                    logger.warn("DEBUG: Unknown VPN gateway state: " + state);
                }
            }
            else if( nodeName.equalsIgnoreCase("type") && attr.hasChildNodes() ) {
                String t = attr.getFirstChild().getNodeValue().trim();
                
                if( t.equalsIgnoreCase("ipsec.1") ) {
                    gateway.setProtocol(VpnProtocol.IPSEC1);
                }
                else if( t.equalsIgnoreCase("openvpn") ) {
                    gateway.setProtocol(VpnProtocol.OPEN_VPN);
                }
                else {
                    logger.warn("DEBUG: Unknown VPN gateway type: " + t);
                    gateway.setProtocol(VpnProtocol.IPSEC1);
                }
            }
            else if( nodeName.equalsIgnoreCase("ipAddress") && attr.hasChildNodes() ) {
                gateway.setEndpoint(attr.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("bgpAsn") && attr.hasChildNodes() ) {
                gateway.setBgpAsn(attr.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("tagSet") && attr.hasChildNodes() ) {
                getProvider().setTags(attr, gateway);
            }
        }
        if( gateway.getProviderVpnGatewayId() == null ) {
            return null;
        }
        if( gateway.getName() == null ) {
            gateway.setName(gateway.getProviderVpnGatewayId() + " [" + gateway.getEndpoint() + "]");
        }
        if( gateway.getDescription() == null ) {
            gateway.setDescription(gateway.getName());
        }
        return gateway;
    }

    private @Nonnull VpnGatewayState toGatewayState(@Nonnull String state) {

        if( state.equalsIgnoreCase("available") ) {
            return VpnGatewayState.AVAILABLE;
        }
        else if( state.equalsIgnoreCase("deleting") ) {
            return VpnGatewayState.DELETING;
        }
        else if( state.equalsIgnoreCase("deleted") ) {
            return VpnGatewayState.DELETED;
        }
        else if( state.equalsIgnoreCase("pending") ) {
            return VpnGatewayState.PENDING;
        }
        else {
            logger.warn("DEBUG: Unknown AWS VPN gateway state: " + state);
            return VpnGatewayState.PENDING;
        }
    }

    private @Nullable ResourceStatus toGatewayStatus(@Nullable Node node) throws CloudException, InternalException {
        if( node == null ) {
            return null;
        }

        NodeList attributes = node.getChildNodes();
        VpnGatewayState state = VpnGatewayState.PENDING;
        String gatewayId = null;

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attr = attributes.item(i);
            String nodeName = attr.getNodeName();

            if( nodeName.equalsIgnoreCase("customerGatewayId") && attr.hasChildNodes() ) {
                gatewayId = attr.getFirstChild().getNodeValue().trim();
            }
            else if( nodeName.equalsIgnoreCase("state") && attr.hasChildNodes() ) {
                state = toGatewayState(attr.getFirstChild().getNodeValue().trim());
            }
        }
        if( gatewayId == null ) {
            return null;
        }
        return new ResourceStatus(gatewayId, state);
    }

    private @Nullable Vpn toVPN(@Nullable Node node) throws CloudException, InternalException {
        if( node == null ) {
            return null;
        }
        
        NodeList attributes = node.getChildNodes();
        String name = null, description = null;
        Vpn vpn = new Vpn();

        vpn.setCurrentState(VpnState.PENDING);
        vpn.setProviderRegionId(getContext().getRegionId());
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attr = attributes.item(i);
            String nodeName = attr.getNodeName();
            
            if( nodeName.equalsIgnoreCase("vpnGatewayId") && attr.hasChildNodes() ) {
                vpn.setProviderVpnId(attr.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("state") ) {
                vpn.setCurrentState(toVPNState(attr.getFirstChild().getNodeValue().trim()));
            }
            else if( nodeName.equalsIgnoreCase("type") && attr.hasChildNodes() ) {
                String t = attr.getFirstChild().getNodeValue().trim();

                if( t.equalsIgnoreCase("ipsec.1") ) {
                    vpn.setProtocol(VpnProtocol.IPSEC1);
                }
                else if( t.equalsIgnoreCase("openvpn") ) {
                    vpn.setProtocol(VpnProtocol.OPEN_VPN);
                }
                else {
                    logger.warn("DEBUG: Unknown VPN gateway type: " + t);
                    vpn.setProtocol(VpnProtocol.IPSEC1);
                }
            }
            else if( nodeName.equalsIgnoreCase("attachments") && attr.hasChildNodes() ) {
                TreeSet<String> vlans = new TreeSet<String>();
                NodeList list = attr.getChildNodes();
                
                for( int j=0; j<list.getLength(); j++ ) {
                    Node att = list.item(j);
                    
                    if( att.getNodeName().equalsIgnoreCase("item") && att.hasChildNodes() ) {
                        NodeList aaList = attr.getChildNodes();
                        String id = null;
                        
                        for( int k=0; k<aaList.getLength(); k++ ) {
                            Node aa = aaList.item(k);
                            
                            if( aa.getNodeName().equalsIgnoreCase("vpcId") && aa.hasChildNodes() ) {
                                id = aa.getFirstChild().getNodeValue().trim();
                                break;
                            }
                        }
                        if( id != null ) {
                            vlans.add(id);
                        }
                    }
                }
                vpn.setProviderVlanIds(vlans.toArray(new String[vlans.size()]));
            }
            else if( nodeName.equalsIgnoreCase("tagSet") && attr.hasChildNodes() ) {
                getProvider().setTags(attr, vpn);
                if( vpn.getTags().get("name") != null ) {
                    name =  vpn.getTags().get("name");
                }
                if( vpn.getTags().get("description") != null ) {
                    description =  vpn.getTags().get("description");
                }
            }
        }
        if( vpn.getProviderVpnId() == null ) {
            return null;
        }
        if( vpn.getName() == null ) {
            vpn.setName(name == null ? vpn.getProviderVpnId() : name);
        }
        if( vpn.getDescription() == null ) {
            vpn.setDescription(description == null ? vpn.getName() : description);
        }
        return vpn;
    }

    private @Nonnull VpnState toVPNState(@Nonnull String status) throws CloudException, InternalException {
        if( status.equalsIgnoreCase("available") ) {
            return VpnState.AVAILABLE;
        }
        else if( status.equalsIgnoreCase("deleting") ) {
            return VpnState.DELETING;
        }
        else if( status.equalsIgnoreCase("deleted") ) {
            return VpnState.DELETED;
        }
        else if( status.equalsIgnoreCase("pending") ) {
            return VpnState.PENDING;
        }
        else {
            logger.warn("DEBUG: Unknown AWS VPN state: " + status);
            return VpnState.PENDING;
        }
    }

    private @Nullable ResourceStatus toVPNStatus(@Nullable Node node) throws CloudException, InternalException {
        if( node == null ) {
            return null;
        }

        NodeList attributes = node.getChildNodes();
        VpnState state = VpnState.PENDING;
        String vpnId = null;

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attr = attributes.item(i);
            String nodeName = attr.getNodeName();

            if( nodeName.equalsIgnoreCase("vpnGatewayId") && attr.hasChildNodes() ) {
                vpnId = attr.getFirstChild().getNodeValue().trim();
            }
            else if( nodeName.equalsIgnoreCase("state") ) {
                state = toVPNState(attr.getFirstChild().getNodeValue().trim());
            }

        }
        if( vpnId == null ) {
            return null;
        }
        return new ResourceStatus(vpnId, state);
    }
}
