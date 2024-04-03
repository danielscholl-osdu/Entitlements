package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import com.google.gson.Gson;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.GetGroupsTest;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GetGroupsAzureTest extends GetGroupsTest {

    public GetGroupsAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }

    @Test
    public void shouldReturn200WhenMakingValidGetGroupsRequestWithRoleEnabled() throws Exception {
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

        ListGroupResponse listGroupResponse = new Gson().fromJson(getGroupsResponseBody, ListGroupResponse.class);

        Assert.assertEquals(token.getUserId(), listGroupResponse.getDesId());
        Assert.assertEquals(token.getUserId(), listGroupResponse.getMemberEmail());

        //assert all items contain the role info
        Assert.assertEquals(listGroupResponse.getGroups().stream().filter(item-> !item.getRole().isEmpty()).collect(Collectors.toList()).size(), listGroupResponse.groups.size() );
    }
}
