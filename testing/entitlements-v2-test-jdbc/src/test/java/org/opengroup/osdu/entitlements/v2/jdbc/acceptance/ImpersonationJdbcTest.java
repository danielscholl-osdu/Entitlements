/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.jdbc.acceptance;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.core.common.partition.*;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.MemberItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.EntitlementsV2Service;
import org.opengroup.osdu.entitlements.v2.acceptance.util.HttpClientService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;
import org.opengroup.osdu.entitlements.v2.util.GoogleServiceAccount;
import org.opengroup.osdu.entitlements.v2.util.JdbcConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.JdbcTokenService;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ImpersonationJdbcTest {

    private final String baseUrl;

    private final Client client;

    private final Gson gson = new Gson();

    private final TokenService tokenService = new JdbcTokenService();

    private final ConfigurationService configurationService = new JdbcConfigurationService();

    private final EntitlementsV2Service entitlementsV2Service;

    public ImpersonationJdbcTest() {
        this.baseUrl = configurationService.getServiceUrl();
        this.client = getClient();
        this.entitlementsV2Service = new EntitlementsV2Service(configurationService, new HttpClientService(configurationService));
    }

    @Test
    public void shouldGetOkImpersonatedGet() throws Exception {
        String memberEmail = "impersonatetestmember@test.com";
        String impersonationGroup = "users.datalake.impersonation@" + configurationService.getTenantId() + "." + configurationService.getDomain();
        String dataGroup = "users@" + configurationService.getTenantId() + "." + configurationService.getDomain();
        String userGroup = "service.entitlements.user@" + configurationService.getTenantId() + "." + configurationService.getDomain();
        String token = tokenService.getToken().getValue();

        //check if member in data group, add if not
        List<String> members = entitlementsV2Service.getMembers(dataGroup, token).getMembers().stream().map(MemberItem::getEmail).collect(Collectors.toList());
        if (!members.contains(memberEmail)) {
            AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
                    .groupEmail(dataGroup).role("MEMBER").memberEmail(memberEmail).build();
            entitlementsV2Service.addMember(addGroupMemberRequestData, token);
        }

        //check if member in impersonation group, add if not
        members = entitlementsV2Service.getMembers(impersonationGroup, token).getMembers().stream().map(MemberItem::getEmail).collect(Collectors.toList());
        if (!members.contains(memberEmail)) {
            AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
                    .groupEmail(impersonationGroup).role("MEMBER").memberEmail(memberEmail).build();
            entitlementsV2Service.addMember(addGroupMemberRequestData, token);
        }

        //check if member in user group, add if not
        members = entitlementsV2Service.getMembers(userGroup, token).getMembers().stream().map(MemberItem::getEmail).collect(Collectors.toList());
        if (!members.contains(memberEmail)) {
            AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
                    .groupEmail(userGroup).role("MEMBER").memberEmail(memberEmail).build();
            entitlementsV2Service.addMember(addGroupMemberRequestData, token);
        }

        //create control random group and add member
        String groupName = "groupname-" + System.currentTimeMillis();
        GroupItem groupItem = entitlementsV2Service.createGroup(groupName, token);
        AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(memberEmail).build();
        entitlementsV2Service.addMember(addGroupMemberRequestData, token);

        ClientResponse response = sendWithOnBehalfOfHeader("GET", memberEmail, token);

        Assert.assertEquals(200, response.getStatus());
        String getGroupsResponseBody = response.getEntity(String.class);
        ListGroupResponse listGroupResponse = gson.fromJson(getGroupsResponseBody, ListGroupResponse.class);
        Optional<String> delegationGroup = listGroupResponse.getGroups().stream().map(GroupItem::getName).filter(groupName::equalsIgnoreCase).findFirst();
        Assert.assertEquals(memberEmail, listGroupResponse.getMemberEmail());
        Assert.assertEquals(memberEmail, listGroupResponse.getDesId());
        Assert.assertTrue(delegationGroup.isPresent());
        Assert.assertEquals(groupName, delegationGroup.get());

        entitlementsV2Service.removeMember(groupItem.getEmail(), memberEmail, token);
        entitlementsV2Service.deleteGroup(groupItem.getEmail(), token);
    }

    @Test
    public void shouldFailIfNoImpersonationGroup() throws Exception {
        String token = tokenService.getToken().getValue();
        String memberEmail = "impersonationtestmemberwithoutgroup@test.com";
        String dataGroup = "users@" + configurationService.getTenantId() + "." + configurationService.getDomain();
        String impersonationGroup = "users.datalake.impersonation@" + configurationService.getTenantId() + "." + configurationService.getDomain();

        //check if member in data group, add if not
        List<String> members = entitlementsV2Service.getMembers(dataGroup, token).getMembers().stream().map(MemberItem::getEmail).collect(Collectors.toList());
        if (!members.contains(memberEmail)) {
            AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
                    .groupEmail(dataGroup).role("MEMBER").memberEmail(memberEmail).build();
            entitlementsV2Service.addMember(addGroupMemberRequestData, token);
        }

        //check if member in impersonation group, remove if yes
        members = entitlementsV2Service.getMembers(impersonationGroup, token).getMembers().stream().map(MemberItem::getEmail).collect(Collectors.toList());
        if (members.contains(memberEmail)) {
            entitlementsV2Service.removeMember(impersonationGroup, memberEmail, token);
        }


        ClientResponse response = sendWithOnBehalfOfHeader("GET", memberEmail, token);

        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void shouldFailIfTryToImpersonateTenantServiceAccount() throws Exception{
        String token = tokenService.getToken().getValue();
        String tenantServiceAccount = getPartitionProperty("serviceAccount");
        ClientResponse response = sendWithOnBehalfOfHeader("GET", tenantServiceAccount, token);

        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void shouldFailIfNoDelegationGroup() throws Exception {
        String noDataAccessToken = getNoDataAccToken().getValue();

        ClientResponse response = sendWithOnBehalfOfHeader("GET", "no.matter@user.id", noDataAccessToken);

        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void shouldGetBadRequestImpersonatedPost() throws Exception {
        String token = tokenService.getToken().getValue();
        ClientResponse response = sendWithOnBehalfOfHeader("POST", "no.matter@user.id", token);

        Assert.assertEquals(400, response.getStatus());
    }

    private ClientResponse sendWithOnBehalfOfHeader(String method, String onBehalfOf, String token) throws Exception {
        String resourceUrl = new URL(baseUrl + "groups").toString();
        log.info("Sending request to URL: {}", resourceUrl);
        WebResource webResource = client.resource(resourceUrl);

        WebResource.Builder builder = webResource.getRequestBuilder();

        builder.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .header("data-partition-id", configurationService.getTenantId())
                .header("on-behalf-of", onBehalfOf);

        if ("POST".equalsIgnoreCase(method)) {
            return builder.method(method, ClientResponse.class, "{}"); //fixes 411 error of empty-body POST
        }
        return builder.method(method, ClientResponse.class);
    }

    private String getPartitionProperty(String property) throws Exception{
        String partitionApi = System.getProperty("PARTITION_API", System.getenv("PARTITION_API"));
        String resourceUrl = new URL(partitionApi + "/partitions/" + configurationService.getTenantId()).toString();
        WebResource webResource = client.resource(resourceUrl);

        WebResource.Builder builder = webResource.getRequestBuilder();

        builder.accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON);

        String response = builder.method("GET", String.class);

        Type parametrizedType = TypeToken.getParameterized(Map.class, new Class[]{String.class, Property.class}).getType();
        Map<String, Property> properties = (Map)gson.fromJson(response, parametrizedType);

        Assert.assertNotNull(properties.get(property));
        return properties.get(property).getValue().toString();
    }

    private Client getClient() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {/*do nothing*/}
        return Client.create();
    }

    private Token getNoDataAccToken() {
        Token testerToken = null;
        String serviceAccountFile = System
                .getProperty("NO_DATA_ACCESS_TESTER", System.getenv("NO_DATA_ACCESS_TESTER"));
        try {
            GoogleServiceAccount testerAccount = new GoogleServiceAccount(serviceAccountFile);

            testerToken = Token.builder()
                    .value(testerAccount.getAuthToken())
                    .userId(testerAccount.getEmail())
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return testerToken;
    }
}
