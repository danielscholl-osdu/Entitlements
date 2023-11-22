package org.opengroup.osdu.entitlements.v2.acceptance;

import com.google.gson.Gson;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.UpdateGroupRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.UpdateGroupResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import java.io.IOException;

public abstract class UpdateGroupTest extends AcceptanceBaseTest {

    private final Token token = tokenService.getToken();

    public UpdateGroupTest(ConfigurationService configurationService, TokenService tokenService) {
        super(configurationService, tokenService);
    }

    @Test
    public void shouldRenameGroupSuccessfully() throws Exception {
        Token token = tokenService.getToken();
        String oldGroupName = "oldGroupName-" + currentTime;
        String newGroupName = "newGroupName-" + currentTime;

        entitlementsV2Service.createGroup(oldGroupName, token.getValue());

        CloseableHttpResponse response = httpClientService.send(getRenameGroupRequestData(oldGroupName, newGroupName, token.getValue()));
        UpdateGroupResponse updateGroupResponse = new Gson().fromJson(EntityUtils.toString(response.getEntity()), UpdateGroupResponse.class);
        assertEquals(200, response.getCode());
        assertEquals(newGroupName.toLowerCase(), updateGroupResponse.getName());
        assertEquals(configurationService.getIdOfGroup(newGroupName).toLowerCase(), updateGroupResponse.getEmail());
        assertEquals(newGroupName.toLowerCase(), updateGroupResponse.getName().toLowerCase());
    }

    @Test
    public void shouldUpdateAppIdsSuccessfully() throws Exception {
        Token token = tokenService.getToken();
        String groupName = "groupName-" + currentTime;
        Set<String> newAppIds = new HashSet<>();
        newAppIds.add("app1");
        newAppIds.add("app2");

        entitlementsV2Service.createGroup(groupName, token.getValue());

        CloseableHttpResponse response = httpClientService.send(getUpdateAppIdsRequestData(groupName, newAppIds, token.getValue()));
        UpdateGroupResponse updateGroupResponse = new Gson().fromJson(EntityUtils.toString(response.getEntity()), UpdateGroupResponse.class);
        assertEquals(200, response.getCode());
        assertEquals(groupName.toLowerCase(), updateGroupResponse.getName());
        assertEquals(configurationService.getIdOfGroup(groupName).toLowerCase(), updateGroupResponse.getEmail());
        assertEquals(new HashSet<>(updateGroupResponse.getAppIds()), newAppIds);
    }

    @Override
    protected void cleanup() throws Exception {
        Token token = tokenService.getToken();
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("newGroupName-" + currentTime), token.getValue());
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("groupName-" + currentTime), token.getValue());
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

    @Test
    public void shouldReturnBadRequestWhenMakingHttpRequestWithoutValidUrl() throws IOException {
        RequestData requestData = RequestData.builder()
                .method("PATCH")
                .relativePath("groups/%25")
                .dataPartitionId(configurationService.getTenantId())
                .token(token.getValue())
                .build();

        CloseableHttpResponse closeableHttpResponse = httpClientService.send(requestData);

        assertEquals(HttpStatus.BAD_REQUEST.value(), closeableHttpResponse.getCode());
    }
}
