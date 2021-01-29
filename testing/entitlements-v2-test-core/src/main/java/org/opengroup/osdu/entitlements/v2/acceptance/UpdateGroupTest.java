package org.opengroup.osdu.entitlements.v2.acceptance;

import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.UpdateGroupRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.UpdateGroupResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class UpdateGroupTest extends AcceptanceBaseTest {

    public UpdateGroupTest(ConfigurationService configurationService, TokenService tokenService) {
        super(configurationService, tokenService);
    }

    @Test
    public void shouldRenameGroupSuccessfully() throws Exception {
        Token token = tokenService.getToken();
        String oldGroupName = "oldGroupName-" + currentTime;
        String newGroupName = "newGroupName-" + currentTime;

        entitlementsV2Service.createGroup(oldGroupName, token.getValue());
        GroupItem newGroupItem = GroupItem.builder().name(newGroupName.toLowerCase())
                .description("desc")
                .email(configurationService.getIdOfGroup(newGroupName)).build();

        ClientResponse response = httpClientService.send(getRenameGroupRequestData(oldGroupName, newGroupName, token.getValue()));
        UpdateGroupResponse updateGroupResponse = new Gson().fromJson(response.getEntity(String.class), UpdateGroupResponse.class);
        Assert.assertEquals(202, response.getStatus());
        Assert.assertEquals(newGroupName.toLowerCase(), updateGroupResponse.getName());
        Assert.assertEquals(configurationService.getIdOfGroup(newGroupName).toLowerCase(), updateGroupResponse.getEmail());

        ListGroupResponse listGroupResponse = entitlementsV2Service.getGroups(token.getValue());
        GroupItem groupItemFromGetGroups = listGroupResponse.getGroups()
                .stream()
                .filter(groupItem -> groupItem.getEmail().equals(configurationService.getIdOfGroup(newGroupName)))
                .findFirst().orElse(null);
        Assert.assertEquals(newGroupItem, groupItemFromGetGroups);
    }

    @Test
    public void shouldUpdateAppIdsSuccessfully() throws Exception {
        Token token = tokenService.getToken();
        String groupName = "groupName-" + currentTime;
        Set<String> newAppIds = new HashSet<>();
        newAppIds.add("app1");
        newAppIds.add("app2");

        entitlementsV2Service.createGroup(groupName, token.getValue());

        ClientResponse response = httpClientService.send(getUpdateAppIdsRequestData(groupName, newAppIds, token.getValue()));
        UpdateGroupResponse updateGroupResponse = new Gson().fromJson(response.getEntity(String.class), UpdateGroupResponse.class);
        Assert.assertEquals(202, response.getStatus());
        Assert.assertEquals(groupName.toLowerCase(), updateGroupResponse.getName());
        Assert.assertEquals(configurationService.getIdOfGroup(groupName).toLowerCase(), updateGroupResponse.getEmail());
        Assert.assertEquals(new HashSet<>(updateGroupResponse.getAppIds()), newAppIds);
    }

    @Override
    protected void cleanup() throws Exception {
        Token token = tokenService.getToken();
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("newGroupName-" + currentTime), token.getValue());
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("groupName-" + currentTime), token.getValue());
    }

    @Override
    protected RequestData getRequestDataForUnauthorizedTest() {
        Token noAccessToken = tokenService.getNoDataAccessToken();
        UpdateGroupRequestData requestBody = UpdateGroupRequestData.builder()
                .op("replace")
                .path("/name")
                .value(Collections.singletonList("newGroupName")).build();
        return RequestData.builder()
                .method("PATCH").dataPartitionId(configurationService.getTenantId())
                .relativePath("groups/" + configurationService.getIdOfGroup("test"))
                .body(new Gson().toJson(Collections.singletonList(requestBody)))
                .token(noAccessToken.getValue())
                .build();
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        UpdateGroupRequestData requestBody = UpdateGroupRequestData.builder()
                .op("replace")
                .path("/name")
                .value(Collections.singletonList("newGroupName")).build();
        return RequestData.builder()
                .method("PATCH").dataPartitionId(configurationService.getTenantId())
                .relativePath("groups/" + configurationService.getIdOfGroup("test"))
                .body(new Gson().toJson(Collections.singletonList(requestBody)))
                .build();
    }

    @Override
    protected RequestData getRequestDataForUnauthorizedPartition() {
        Token token = tokenService.getToken();
        UpdateGroupRequestData requestBody = UpdateGroupRequestData.builder()
                .op("replace")
                .path("/name")
                .value(Collections.singletonList("newGroupName")).build();
        return RequestData.builder()
                .method("PATCH").dataPartitionId(configurationService.getUnauthorizedTenantId())
                .relativePath("groups/" + configurationService.getIdOfGroup("test"))
                .body(new Gson().toJson(Collections.singletonList(requestBody)))
                .token(token.getValue())
                .build();
    }

    private RequestData getRenameGroupRequestData(String oldGroupName, String newGroupName, String token) {
        UpdateGroupRequestData requestBody = UpdateGroupRequestData.builder()
                .op("replace")
                .path("/name")
                .value(Collections.singletonList(newGroupName)).build();
        return RequestData.builder()
                .method("PATCH").dataPartitionId(configurationService.getTenantId())
                .relativePath("groups/" + configurationService.getIdOfGroup(oldGroupName))
                .token(token)
                .body(new Gson().toJson(Collections.singletonList(requestBody))).build();
    }

    private RequestData getUpdateAppIdsRequestData(String groupName, Set<String> newAppIds, String token) {
        UpdateGroupRequestData requestBody = UpdateGroupRequestData.builder()
                .op("replace")
                .path("/appIds")
                .value(new ArrayList<>(newAppIds)).build();
        return RequestData.builder()
                .method("PATCH").dataPartitionId(configurationService.getTenantId())
                .relativePath("groups/" + configurationService.getIdOfGroup(groupName))
                .token(token)
                .body(new Gson().toJson(Collections.singletonList(requestBody))).build();
    }
}
