package org.opengroup.osdu.entitlements.v2;

import static org.junit.Assert.assertEquals;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.util.CommonConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.TokenTestUtils;
import org.springframework.http.HttpStatus;

public class DeleteGroupTest extends AcceptanceBaseTest {

    public DeleteGroupTest() {
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
        String groupName = "groupName-" + currentTime;
        GroupItem groupItem = entitlementsV2Service.createGroup(groupName, testUtils.getToken());
        entitlementsV2Service.deleteGroup(groupItem.getEmail(), testUtils.getToken());
        verifyGroupDoesNotExist(groupItem.getEmail(), testUtils.getToken());
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
    public void shouldReturnBadRequestWhenMakingHttpRequestWithInvalidUrl() throws Exception {
        RequestData requestData = RequestData.builder()
                .method("DELETE")
                .relativePath("groups/%25")
                .dataPartitionId(configurationService.getTenantId())
                .token(testUtils.getToken())
                .build();

        CloseableHttpResponse closeableHttpResponse = httpClientService.send(requestData);

        assertEquals(HttpStatus.BAD_REQUEST.value(), closeableHttpResponse.getCode());
    }
}
