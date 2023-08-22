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

package org.opengroup.osdu.entitlements.v2.acceptance;

import com.google.gson.Gson;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.MemberItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.PartitionService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

@Slf4j
public abstract class ImpersonationTest extends AcceptanceBaseTest {

  private final Gson gson = new Gson();
  private final PartitionService partitionService;

  public ImpersonationTest(ConfigurationService configurationService, TokenService tokenService,
      PartitionService partitionService) {
    super(configurationService, tokenService);
    this.partitionService = partitionService;
  }

  @Test
  public void shouldGetOkImpersonatedGet() throws Exception {
    String memberEmail = "impersonatetestmember@test.com";
    String impersonationGroup =
        "users.datalake.impersonation@" + configurationService.getTenantId() + "."
            + configurationService.getDomain();
    String dataGroup =
        "users@" + configurationService.getTenantId() + "." + configurationService.getDomain();
    String userGroup = "service.entitlements.user@" + configurationService.getTenantId() + "."
        + configurationService.getDomain();
    String token = tokenService.getToken().getValue();

    //check if member in data group, add if not
    List<String> members = entitlementsV2Service.getMembers(dataGroup, token).getMembers().stream()
        .map(MemberItem::getEmail).collect(Collectors.toList());
    if (!members.contains(memberEmail)) {
      AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
          .groupEmail(dataGroup).role("MEMBER").memberEmail(memberEmail).build();
      entitlementsV2Service.addMember(addGroupMemberRequestData, token);
    }

    //check if member in impersonation group, add if not
    members = entitlementsV2Service.getMembers(impersonationGroup, token).getMembers().stream()
        .map(MemberItem::getEmail).collect(Collectors.toList());
    if (!members.contains(memberEmail)) {
      AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
          .groupEmail(impersonationGroup).role("MEMBER").memberEmail(memberEmail).build();
      entitlementsV2Service.addMember(addGroupMemberRequestData, token);
    }

    //check if member in user group, add if not
    members = entitlementsV2Service.getMembers(userGroup, token).getMembers().stream()
        .map(MemberItem::getEmail).collect(Collectors.toList());
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

    CloseableHttpResponse response = sendWithOnBehalfOfHeader("GET", memberEmail, token);

    Assert.assertEquals(200, response.getCode());
    String getGroupsResponseBody = new String(response.getEntity().getContent().readAllBytes());
    ListGroupResponse listGroupResponse = gson.fromJson(getGroupsResponseBody,
        ListGroupResponse.class);
    Optional<String> delegationGroup = listGroupResponse.getGroups().stream()
        .map(GroupItem::getName).filter(groupName::equalsIgnoreCase).findFirst();
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
    String dataGroup =
        "users@" + configurationService.getTenantId() + "." + configurationService.getDomain();
    String impersonationGroup =
        "users.datalake.impersonation@" + configurationService.getTenantId() + "."
            + configurationService.getDomain();

    //check if member in data group, add if not
    List<String> members = entitlementsV2Service.getMembers(dataGroup, token).getMembers().stream()
        .map(MemberItem::getEmail).collect(Collectors.toList());
    if (!members.contains(memberEmail)) {
      AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
          .groupEmail(dataGroup).role("MEMBER").memberEmail(memberEmail).build();
      entitlementsV2Service.addMember(addGroupMemberRequestData, token);
    }

    //check if member in impersonation group, remove if yes
    members = entitlementsV2Service.getMembers(impersonationGroup, token).getMembers().stream()
        .map(MemberItem::getEmail).collect(Collectors.toList());
    if (members.contains(memberEmail)) {
      entitlementsV2Service.removeMember(impersonationGroup, memberEmail, token);
    }

    CloseableHttpResponse response = sendWithOnBehalfOfHeader("GET", memberEmail, token);

    Assert.assertEquals(403, response.getCode());
  }

  @Test
  public void shouldFailIfTryToImpersonateTenantServiceAccount() throws Exception {
    String token = tokenService.getToken().getValue();
    String tenantServiceAccount = partitionService.getPartitionProperty("serviceAccount");
    CloseableHttpResponse response = sendWithOnBehalfOfHeader("GET", tenantServiceAccount, token);

    Assert.assertEquals(403, response.getCode());
  }

  @Test
  public void shouldFailIfNoDelegationGroup() throws Exception {
    String noDataAccessToken = tokenService.getNoAccToken().getValue();

    CloseableHttpResponse response = sendWithOnBehalfOfHeader("GET", "no.matter@user.id",
        noDataAccessToken);

    Assert.assertEquals(403, response.getCode());
  }

  @Override
  protected RequestData getRequestDataForNoTokenTest() {
    return RequestData.builder()
        .method("GET").dataPartitionId(configurationService.getTenantId())
        .relativePath("groups")
        .build();
  }

  private CloseableHttpResponse sendWithOnBehalfOfHeader(String method, String onBehalfOf,
      String token)
      throws Exception {
    String resourceUrl = new URL(configurationService.getServiceUrl() + "groups").toString();
    log.info("Sending request to URL: {}", resourceUrl);

    RequestData requestData = RequestData.builder()
        .relativePath("groups")
        .method(method)
        .dataPartitionId(configurationService.getTenantId())
        .token(token)
        .additionalHeaders(Collections.singletonMap("on-behalf-of", onBehalfOf))
        .build();

    return httpClientService.send(requestData);
  }

}
