package org.opengroup.osdu.entitlements.v2.acceptance;

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

    @Override
    protected RequestData getRequestDataForUnauthorizedTest() {
        Token noAccessToken = tokenService.getNoDataAccessToken();
        return RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath("groups")
                .token(noAccessToken.getValue())
                .build();
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        return RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath("groups")
                .build();
    }

    @Override
    protected RequestData getRequestDataForUnauthorizedPartition() {
        Token token = tokenService.getToken();
        return RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getUnauthorizedTenantId())
                .relativePath("groups")
                .token(token.getValue())
                .build();
    }
}
