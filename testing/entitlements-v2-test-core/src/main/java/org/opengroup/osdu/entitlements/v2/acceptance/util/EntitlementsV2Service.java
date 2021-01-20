package org.opengroup.osdu.entitlements.v2.acceptance.util;

import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;
import lombok.RequiredArgsConstructor;
import org.junit.Assert;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListMemberResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Service has logic for successful scenarios.
 * To be used mostly in setup and teardown logic of integration tests
 */
@RequiredArgsConstructor
public class EntitlementsV2Service {

    private final ConfigurationService configurationService;
    private final HttpClientService httpClientService;

    public ListGroupResponse getGroups(String token) throws Exception {
        RequestData getGroupsRequestData = RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath("/groups")
                .token(token).build();
        ClientResponse response = httpClientService.send(getGroupsRequestData);
        Assert.assertEquals(200, response.getStatus());
        String getGroupsResponseBody = response.getEntity(String.class);
        return new Gson().fromJson(getGroupsResponseBody, ListGroupResponse.class);
    }

    public GroupItem createGroup(String groupName, String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath("/groups")
                .token(token)
                .body(new Gson().toJson(GroupItem.builder().name(groupName).description("desc").build())).build();
        ClientResponse response = httpClientService.send(requestData);
        Assert.assertEquals(202, response.getStatus());
        return new Gson().fromJson(response.getEntity(String.class), GroupItem.class);
    }

    public void deleteGroup(String groupEmail, String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("DELETE")
                .relativePath("/groups/" + groupEmail)
                .dataPartitionId(configurationService.getTenantId())
                .token(token)
                .build();
        ClientResponse response = httpClientService.send(requestData);
        Assert.assertEquals(202, response.getStatus());
    }

    public ListMemberResponse getMembers(String groupEmail, String token) throws Exception {
        RequestData getGroupsRequestData = RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("/groups/%s/members", groupEmail))
                .token(token).build();
        ClientResponse response = httpClientService.send(getGroupsRequestData);
        Assert.assertEquals(200, response.getStatus());
        String getGroupsResponseBody = response.getEntity(String.class);
        return new Gson().fromJson(getGroupsResponseBody, ListMemberResponse.class);
    }

    public GroupItem addMember(AddMemberRequestData addMemberRequestData, String token) throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("role", addMemberRequestData.getRole().toUpperCase());
        requestBody.put("email", addMemberRequestData.getMemberEmail());
        RequestData requestData = RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("/groups/%s/members", addMemberRequestData.getGroupEmail()))
                .token(token)
                .body(new Gson().toJson(requestBody)).build();
        ClientResponse response = httpClientService.send(requestData);
        Assert.assertEquals(202, response.getStatus());
        return new Gson().fromJson(response.getEntity(String.class), GroupItem.class);
    }

    public void removeMember(String groupEmail, String memberEmail, String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("DELETE").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("/groups/%s/members/%s", groupEmail, memberEmail))
                .token(token).build();
        ClientResponse response = httpClientService.send(requestData);
        Assert.assertEquals(202, response.getStatus());
    }
}
