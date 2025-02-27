package org.opengroup.osdu.entitlements.v2.acceptance;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;
import org.springframework.http.HttpStatus;
import java.io.IOException;

public abstract class DeleteGroupTest extends AcceptanceBaseTest {

    private final Token token = tokenService.getToken();

    public DeleteGroupTest(ConfigurationService configurationService, TokenService tokenService) {
        super(configurationService, tokenService);
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        return RequestData.builder()
                .method("DELETE")
                .relativePath("groups/" + configurationService.getIdOfGroup("test"))
                .dataPartitionId(configurationService.getTenantId())
                .build();
    }

    /**
     * 1) create a group
     * 2) delete a group
     * 3) check group does not exist by verifying 404 when getting all its members
     */
    @Test
    public void shouldReturn202WhenMakingValidHttpRequest() throws Exception {
        Token token = tokenService.getToken();
        String groupName = "groupName-" + currentTime;
        GroupItem groupItem = entitlementsV2Service.createGroup(groupName, token.getValue());
        entitlementsV2Service.deleteGroup(groupItem.getEmail(), token.getValue());
        verifyGroupDoesNotExist(groupItem.getEmail(), token.getValue());
    }

    private void verifyGroupDoesNotExist(String email, String value) throws Exception {
        RequestData getGroupsRequestData = RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members", email))
                .token(value).build();
        CloseableHttpResponse response = httpClientService.send(getGroupsRequestData);
        assertEquals(404, response.getCode());
    }

    @Test
    public void shouldReturnBadRequestWhenMakingHttpRequestWithInvalidUrl() throws IOException {
        RequestData requestData = RequestData.builder()
                .method("DELETE")
                .relativePath("groups/%25")
                .dataPartitionId(configurationService.getTenantId())
                .token(token.getValue())
                .build();

        CloseableHttpResponse closeableHttpResponse = httpClientService.send(requestData);

        assertEquals(HttpStatus.BAD_REQUEST.value(), closeableHttpResponse.getCode());
    }
}
