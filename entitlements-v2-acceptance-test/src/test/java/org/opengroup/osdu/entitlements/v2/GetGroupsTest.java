package org.opengroup.osdu.entitlements.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.model.Token;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.util.AnthosConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.OpenIDTokenProvider;


public class GetGroupsTest extends AcceptanceBaseTest {

    public GetGroupsTest() {
        super(new AnthosConfigurationService(), new OpenIDTokenProvider());
    }

    @Test
    public void should200ForGetGroupsWithRoleEnabled() {
        test200ForGetGroupsWithRoleEnabled();
    }

    @Test
    public void shouldReturn200WhenMakingValidGetGroupsRequest() throws Exception {
        Token token = tokenService.getToken();
        ListGroupResponse listGroupResponse = entitlementsV2Service.getGroups(token.getValue());
        assertEquals(token.getUserId(), listGroupResponse.getDesId());
        assertEquals(token.getUserId(), listGroupResponse.getMemberEmail());
    }

    @SneakyThrows
    private void test200ForGetGroupsWithRoleEnabled() {
        Token token = tokenService.getToken();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("roleRequired", "true");
        RequestData getGroupsRequestData = RequestData.builder()
            .method("GET").dataPartitionId(configurationService.getTenantId())
            .relativePath("groups")
            .queryParams(queryParams)
            .token(token.getValue()).build();
        CloseableHttpResponse response = httpClientService.send(getGroupsRequestData);
        assertEquals(200, response.getCode());
        String getGroupsResponseBody = EntityUtils.toString(response.getEntity());

        ListGroupResponse listGroupResponse =
            new Gson().fromJson(getGroupsResponseBody, ListGroupResponse.class);

        assertEquals(token.getUserId(), listGroupResponse.getDesId());
        assertEquals(token.getUserId(), listGroupResponse.getMemberEmail());

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
