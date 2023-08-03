package org.opengroup.osdu.entitlements.v2.acceptance.util;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.GetGroupsRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListGroupInPartitionResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListMemberResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

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
        CloseableHttpResponse response = httpClientService.send(getGroupsRequestData);
        Assert.assertEquals(200, response.getCode());
        String getGroupsResponseBody = EntityUtils.toString(response.getEntity());
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
        CloseableHttpResponse response = httpClientService.send(requestData);
        Assert.assertEquals(200, response.getCode());
        String getGroupsResponseBody = EntityUtils.toString(response.getEntity());
        return gson.fromJson(getGroupsResponseBody, ListGroupResponse.class);
    }

    public GroupItem createGroup(String groupName, String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath("groups")
                .token(token)
                .body(gson.toJson(GroupItem.builder().name(groupName).description("desc").build())).build();
        CloseableHttpResponse response = httpClientService.send(requestData);
        Assert.assertEquals(201, response.getCode());
        return gson.fromJson(EntityUtils.toString(response.getEntity()), GroupItem.class);
    }

    public void deleteGroup(String groupEmail, String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("DELETE")
                .relativePath("groups/" + groupEmail)
                .dataPartitionId(configurationService.getTenantId())
                .token(token)
                .build();
        CloseableHttpResponse response = httpClientService.send(requestData);
        Assert.assertEquals(204, response.getCode());
    }

    public ListMemberResponse getMembers(String groupEmail, String token) throws Exception {
        RequestData getGroupsRequestData = RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members", groupEmail))
                .token(token).build();
        CloseableHttpResponse response = httpClientService.send(getGroupsRequestData);
        Assert.assertEquals(200, response.getCode());
        String getGroupsResponseBody = EntityUtils.toString(response.getEntity());
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
        CloseableHttpResponse response = httpClientService.send(requestData);
        Assert.assertEquals(200, response.getCode());
        return gson.fromJson(EntityUtils.toString(response.getEntity()), GroupItem.class);
    }

    public void removeMember(String groupEmail, String memberEmail, String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("DELETE").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members/%s", groupEmail, memberEmail))
                .token(token).build();
        CloseableHttpResponse response = httpClientService.send(requestData);
        Assert.assertEquals(204, response.getCode());
    }

    public void provisionGroupsForNewTenant(String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath("tenant-provisioning")
                .token(token).build();
        CloseableHttpResponse response = httpClientService.send(requestData);
        Assert.assertEquals(200, response.getCode());
    }

    public CloseableHttpResponse deleteMember(String memberEmail, String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("DELETE")
                .relativePath("members/" + memberEmail)
                .dataPartitionId(configurationService.getTenantId())
                .token(token)
                .build();
        CloseableHttpResponse response = httpClientService.send(requestData);
        Assert.assertTrue(204 == response.getCode() || 404 == response.getCode());
        return response;
    }

    public ListGroupInPartitionResponse getGroupsWithinPartition(String token, String...params) throws Exception {
        String joinedParams = "";
        if(params.length > 0){
            StringJoiner paramJoiner = new StringJoiner("&", "?", "");
            for (String param: params){
                paramJoiner.add(param);
            }
            joinedParams = paramJoiner.toString();
        }
        RequestData requestData = RequestData.builder()
            .method("GET")
            .relativePath("groups/all" + joinedParams)
            .dataPartitionId(configurationService.getTenantId())
            .token(token)
            .build();

        CloseableHttpResponse clientResponse = httpClientService.send(requestData);
        Assert.assertEquals(200, clientResponse.getCode());
        String entity = EntityUtils.toString(clientResponse.getEntity());
        return gson.fromJson(entity, ListGroupInPartitionResponse.class);
    }
}
