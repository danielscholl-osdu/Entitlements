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

package org.opengroup.osdu.entitlements.v2;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.util.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;


@Slf4j
public class GetDataGroupsIndexerServiceAccTest {

  private final String baseUrl;
  private final Client client;
  private TestUtils testUtils = null;
  private final ConfigurationService configurationService = new CommonConfigurationService();
  private final EntitlementsV2Service entitlementsV2Service;
  private final String indexerServiceAccountEmail;
  private final Gson gson = new Gson();
  private final Boolean dataRootGroupHierarchyEnabled;

  public GetDataGroupsIndexerServiceAccTest() {
    this.baseUrl = configurationService.getServiceUrl();
    this.client = getClient();
    this.entitlementsV2Service = new EntitlementsV2Service(configurationService, new HttpClientService(configurationService));
    this.indexerServiceAccountEmail = System.getProperty("INDEXER_SERVICE_ACCOUNT_EMAIL", System.getenv("INDEXER_SERVICE_ACCOUNT_EMAIL"));
    dataRootGroupHierarchyEnabled = Boolean.parseBoolean(System.getenv("DATA_ROOT_GROUP_HIERARCHY_ENABLED"));
  }

  @BeforeEach
  public void setupTest() throws Exception {
    this.testUtils = new TokenTestUtils();
  }

  @AfterEach
  public void tearTestDown() throws Exception {
    testUtils = null;
  }

  @Test
  public void shouldReturnCreatedDataGroupForIndexerServiceAcc() throws Exception {
    assumeTrue(dataRootGroupHierarchyEnabled);
    String dataGroupName = "data.indexer.test.group";
    String dataGroupEmail = dataGroupName + "@" + configurationService.getTenantId() + "." + configurationService.getDomain();

    if (isNotDataGroupExist(dataGroupEmail, testUtils.getToken())) {
      entitlementsV2Service.createGroup(dataGroupName, testUtils.getToken());
    }

    ClientResponse successfulResponse = sendGetParentGroupsRequest(testUtils.getToken(),
        indexerServiceAccountEmail);
    assertEquals(200, successfulResponse.getStatus());
    String successfulResponseBody = successfulResponse.getEntity(String.class);
    ListGroupResponse successfulGroupResponse = gson.fromJson(successfulResponseBody, ListGroupResponse.class);
    assertTrue(successfulGroupResponse.getGroups().stream().map(GroupItem::getEmail)
        .anyMatch(email1 -> email1.equals(dataGroupEmail)));

    entitlementsV2Service.deleteGroup(dataGroupEmail, testUtils.getToken());
    ClientResponse getIndexerAccGroupsResponse = sendGetParentGroupsRequest(testUtils.getToken(),
        indexerServiceAccountEmail);
    assertEquals(200, getIndexerAccGroupsResponse.getStatus());
    String indexerAccGroupsBody = getIndexerAccGroupsResponse.getEntity(String.class);
    ListGroupResponse indexerAccGroups = gson.fromJson(indexerAccGroupsBody, ListGroupResponse.class);
    assertFalse(
        indexerAccGroups.getGroups().stream()
            .map(GroupItem::getEmail)
            .anyMatch(email -> email.equals(dataGroupEmail))
    );
  }

  private boolean isNotDataGroupExist(String dataGroupEmail, String token) throws MalformedURLException {
    ClientResponse response = sendGetGroupsRequest(token);
    if (response == null || response.getStatus() != 200) {
      fail("Get groups request failed");
    }
    String body = response.getEntity(String.class);
    ListGroupResponse groupResponse = gson.fromJson(body, ListGroupResponse.class);
    return groupResponse.getGroups().stream().noneMatch(group -> group.getEmail().equals(dataGroupEmail));
  }

  private ClientResponse sendGetGroupsRequest(String token) throws MalformedURLException {
    String resourceUrl = new URL(baseUrl + "groups").toString();
    log.info("Sending request to URL: {}", resourceUrl);
    WebResource webResource = client.resource(resourceUrl);
    return webResource.getRequestBuilder()
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .header("data-partition-id", configurationService.getTenantId())
        .method("GET", ClientResponse.class);
  }

  private ClientResponse sendGetParentGroupsRequest(String token, String partitionServiceAccountEmail)
      throws MalformedURLException {
    String resourceUrl = new URL(baseUrl + "members/" + partitionServiceAccountEmail + "/groups?type=data").toString();
    log.info("Sending request to URL: {}", resourceUrl);
    WebResource webResource = client.resource(resourceUrl);
    return webResource.getRequestBuilder()
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .header("data-partition-id", configurationService.getTenantId())
        .method("GET", ClientResponse.class);
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
}
