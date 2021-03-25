package org.opengroup.osdu.entitlements.v2.acceptance.util;

import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;
import lombok.RequiredArgsConstructor;
import org.junit.Assert;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.GetGroupsRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.UpdateGroupRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListMemberResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service has logic for successful scenarios.
 * To be used mostly in setup and teardown logic of integration tests
 */
@RequiredArgsConstructor
public class EntitlementsV2Service {
    private final Gson gson = new Gson();
    private final ConfigurationService configurationService;
    private final HttpClientService httpClientService;

    public ListGroupResponse getGroups(String token) throws Exception {
        RequestData getGroupsRequestData = RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath("groups")
                .token(token).build();
        ClientResponse response = httpClientService.send(getGroupsRequestData);
        Assert.assertEquals(200, response.getStatus());
        String getGroupsResponseBody = response.getEntity(String.class);
        return gson.fromJson(getGroupsResponseBody, ListGroupResponse.class);
    }

    public ListGroupResponse getGroups(GetGroupsRequestData getGroupsRequestData, String token) throws Exception {
        Map<String, String> queryParams = new HashMap<String, String>() {{
            put("type", getGroupsRequestData.getType().toString());

            String appId = getGroupsRequestData.getAppId();
            if (appId != null) {
                put("appid", appId);
            }
        }};
        RequestData requestData = RequestData.builder()
                .method("GET")
                .relativePath(String.format("members/%s/groups", getGroupsRequestData.getMemberEmail()))
                .queryParams(queryParams)
                .dataPartitionId(configurationService.getTenantId())
                .token(token)
                .build();
        ClientResponse response = httpClientService.send(requestData);
        Assert.assertEquals(200, response.getStatus());
        String getGroupsResponseBody = response.getEntity(String.class);
        return gson.fromJson(getGroupsResponseBody, ListGroupResponse.class);
    }

    public GroupItem createGroup(String groupName, String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath("groups")
                .token(token)
                .body(gson.toJson(GroupItem.builder().name(groupName).description("desc").build())).build();
        ClientResponse response = httpClientService.send(requestData);
        Assert.assertEquals(201, response.getStatus());
        return gson.fromJson(response.getEntity(String.class), GroupItem.class);
    }

    public void deleteGroup(String groupEmail, String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("DELETE")
                .relativePath("groups/" + groupEmail)
                .dataPartitionId(configurationService.getTenantId())
                .token(token)
                .build();
        ClientResponse response = httpClientService.send(requestData);
        Assert.assertEquals(204, response.getStatus());
    }

    public ListMemberResponse getMembers(String groupEmail, String token) throws Exception {
        RequestData getGroupsRequestData = RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members", groupEmail))
                .token(token).build();
        ClientResponse response = httpClientService.send(getGroupsRequestData);
        Assert.assertEquals(200, response.getStatus());
        String getGroupsResponseBody = response.getEntity(String.class);
        return gson.fromJson(getGroupsResponseBody, ListMemberResponse.class);
    }

    public GroupItem addMember(AddMemberRequestData addMemberRequestData, String token) throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("role", addMemberRequestData.getRole().toUpperCase());
        requestBody.put("email", addMemberRequestData.getMemberEmail());
        RequestData requestData = RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members", addMemberRequestData.getGroupEmail()))
                .token(token)
                .body(gson.toJson(requestBody)).build();
        ClientResponse response = httpClientService.send(requestData);
        Assert.assertEquals(200, response.getStatus());
        return gson.fromJson(response.getEntity(String.class), GroupItem.class);
    }

    public void removeMember(String groupEmail, String memberEmail, String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("DELETE").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members/%s", groupEmail, memberEmail))
                .token(token).build();
        ClientResponse response = httpClientService.send(requestData);
        Assert.assertEquals(204, response.getStatus());
    }

    public void provisionGroupsForNewTenant(String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath("tenant-provisioning")
                .token(token).build();
        ClientResponse response = httpClientService.send(requestData);
        Assert.assertEquals(200, response.getStatus());
    }

    public ClientResponse deleteMember(String memberEmail, String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("DELETE")
                .relativePath("members/" + memberEmail)
                .dataPartitionId(configurationService.getTenantId())
                .token(token)
                .build();
        ClientResponse response = httpClientService.send(requestData);
        Assert.assertTrue(204 == response.getStatus() || 404 == response.getStatus());
        return response;
    }

    public ClientResponse updateGroupAppIds(String groupName, Set<String> newAppIds, String token) throws Exception {
        UpdateGroupRequestData requestBody = UpdateGroupRequestData.builder()
                .op("replace")
                .path("/appIds")
                .value(new ArrayList<>(newAppIds)).build();

        RequestData requestData = RequestData.builder()
                .method("PATCH")
                .relativePath("groups/" + configurationService.getIdOfGroup(groupName))
                .dataPartitionId(configurationService.getTenantId())
                .token(token)
                .body(gson.toJson(Collections.singletonList(requestBody))).build();

        ClientResponse response = httpClientService.send(requestData);
        Assert.assertEquals(200, response.getStatus());
        return response;
    }
}
