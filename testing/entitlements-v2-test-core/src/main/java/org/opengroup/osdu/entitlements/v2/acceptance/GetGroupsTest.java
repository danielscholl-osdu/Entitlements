package org.opengroup.osdu.entitlements.v2.acceptance;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

public abstract class GetGroupsTest extends AcceptanceBaseTest {

    public GetGroupsTest(ConfigurationService configurationService, TokenService tokenService) {
        super(configurationService, tokenService);
    }

    @Test
    public void shouldReturn200WhenMakingValidGetGroupsRequest() throws Exception {
        Token token = tokenService.getToken();
        ListGroupResponse listGroupResponse = entitlementsV2Service.getGroups(token.getValue());
        Assert.assertEquals(token.getUserId(), listGroupResponse.getDesId());
        Assert.assertEquals(token.getUserId(), listGroupResponse.getMemberEmail());
    }

    @SneakyThrows
    protected void test200ForGetGroupsWithRoleEnabled() {
        Token token = tokenService.getToken();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("roleRequired", "true");
        RequestData getGroupsRequestData = RequestData.builder()
            .method("GET").dataPartitionId(configurationService.getTenantId())
            .relativePath("groups")
            .queryParams(queryParams)
            .token(token.getValue()).build();
        CloseableHttpResponse response = httpClientService.send(getGroupsRequestData);
        Assert.assertEquals(200, response.getCode());
        String getGroupsResponseBody = EntityUtils.toString(response.getEntity());

        ListGroupResponse listGroupResponse =
            new Gson().fromJson(getGroupsResponseBody, ListGroupResponse.class);

        Assert.assertEquals(token.getUserId(), listGroupResponse.getDesId());
        Assert.assertEquals(token.getUserId(), listGroupResponse.getMemberEmail());

        //assert all items contain the role info
        int expectedHasRoleCount = listGroupResponse.getGroups().stream()
            .filter(item -> !item.getRole().isEmpty())
            .toList()
            .size();
        Assert.assertEquals(expectedHasRoleCount, listGroupResponse.groups.size());
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        return RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath("groups")
                .build();
    }
}
