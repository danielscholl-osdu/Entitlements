package org.opengroup.osdu.entitlements.v2;

import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.model.request.UpdateGroupRequestData;
import org.opengroup.osdu.entitlements.v2.model.response.UpdateGroupResponse;
import org.opengroup.osdu.entitlements.v2.util.CommonConfigurationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.opengroup.osdu.entitlements.v2.util.TokenTestUtils;
import org.springframework.http.HttpStatus;

public class UpdateGroupTest extends AcceptanceBaseTest {

    public UpdateGroupTest() {
        super(new CommonConfigurationService());
    }

    @BeforeEach
    @Override
    public void setupTest() throws Exception {
        this.testUtils = new TokenTestUtils();
    }

    @AfterEach
    @Override
    public void tearTestDown() throws Exception {
        String token = testUtils.getToken();
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("newGroupName-" + currentTime), token);
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("groupName-" + currentTime), token);
        this.testUtils = null;
    }

    @Test
    public void shouldRenameGroupSuccessfully() throws Exception {
        String oldGroupName = "oldGroupName-" + currentTime;
        String newGroupName = "newGroupName-" + currentTime;

        entitlementsV2Service.createGroup(oldGroupName, testUtils.getToken());

        CloseableHttpResponse response = httpClientService.send(getRenameGroupRequestData(oldGroupName, newGroupName, testUtils.getToken()));
        UpdateGroupResponse updateGroupResponse = new Gson().fromJson(EntityUtils.toString(response.getEntity()), UpdateGroupResponse.class);
        assertEquals(200, response.getCode());
        assertEquals(newGroupName.toLowerCase(), updateGroupResponse.getName());
        assertEquals(configurationService.getIdOfGroup(newGroupName).toLowerCase(), updateGroupResponse.getEmail());
        assertEquals(newGroupName.toLowerCase(), updateGroupResponse.getName().toLowerCase());
    }

    @Test
    public void shouldUpdateAppIdsSuccessfully() throws Exception {
        String groupName = "groupName-" + currentTime;
        Set<String> newAppIds = new HashSet<>();
        newAppIds.add("app1");
        newAppIds.add("app2");

        entitlementsV2Service.createGroup(groupName, testUtils.getToken());

        CloseableHttpResponse response = httpClientService.send(getUpdateAppIdsRequestData(groupName, newAppIds, testUtils.getToken()));
        UpdateGroupResponse updateGroupResponse = new Gson().fromJson(EntityUtils.toString(response.getEntity()), UpdateGroupResponse.class);
        assertEquals(200, response.getCode());
        assertEquals(groupName.toLowerCase(), updateGroupResponse.getName());
        assertEquals(configurationService.getIdOfGroup(groupName).toLowerCase(), updateGroupResponse.getEmail());
        assertEquals(new HashSet<>(updateGroupResponse.getAppIds()), newAppIds);
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
    public void shouldReturnBadRequestWhenMakingHttpRequestWithoutValidUrl() throws Exception {
        RequestData requestData = RequestData.builder()
                .method("PATCH")
                .relativePath("groups/%25")
                .dataPartitionId(configurationService.getTenantId())
                .token(testUtils.getToken())
                .build();

        CloseableHttpResponse closeableHttpResponse = httpClientService.send(requestData);

        assertEquals(HttpStatus.BAD_REQUEST.value(), closeableHttpResponse.getCode());
    }
}
