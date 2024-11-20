package org.opengroup.osdu.entitlements.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.util.CommonConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.TokenTestUtils;


public class GetGroupsTest extends AcceptanceBaseTest {

    public GetGroupsTest() {
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
        this.testUtils = null;
    }
    
    @Test
    public void should200ForGetGroupsWithRoleEnabled() {
        test200ForGetGroupsWithRoleEnabled();
    }

    @Test
    public void shouldReturn200WhenMakingValidGetGroupsRequest() throws Exception {
        ListGroupResponse listGroupResponse = entitlementsV2Service.getGroups(testUtils.getToken());
        assertEquals(testUtils.getUserId(), listGroupResponse.getDesId());
        assertEquals(testUtils.getUserId(), listGroupResponse.getMemberEmail());
    }

    @SneakyThrows
    private void test200ForGetGroupsWithRoleEnabled() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("roleRequired", "true");
        RequestData getGroupsRequestData = RequestData.builder()
            .method("GET").dataPartitionId(configurationService.getTenantId())
            .relativePath("groups")
            .queryParams(queryParams)
            .token(testUtils.getToken()).build();
        CloseableHttpResponse response = httpClientService.send(getGroupsRequestData);
        assertEquals(200, response.getCode());
        String getGroupsResponseBody = EntityUtils.toString(response.getEntity());

        ListGroupResponse listGroupResponse =
            new Gson().fromJson(getGroupsResponseBody, ListGroupResponse.class);

        assertEquals(testUtils.getUserId(), listGroupResponse.getDesId());
        assertEquals(testUtils.getUserId(), listGroupResponse.getMemberEmail());

        //assert all items contain the role info
        int expectedHasRoleCount = listGroupResponse.getGroups().stream()
            .filter(item -> !item.getRole().isEmpty())
            .toList()
            .size();
        assertEquals(expectedHasRoleCount, listGroupResponse.groups.size());
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        return RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath("groups")
                .build();
    }
}
