package org.opengroup.osdu.entitlements.v2.acceptance;

import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ErrorResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

public abstract class CreateGroupTest extends AcceptanceBaseTest {
    private final ErrorResponse expectedConflictResponse = ErrorResponse.builder().code(409).reason("Conflict")
            .message("This group already exists").build();

    public CreateGroupTest(ConfigurationService configurationService, TokenService tokenService) {
        super(configurationService, tokenService);
    }

    @Test
    public void shouldCreateGroupOnlyOneTimeSuccessfully() throws Exception {
        String groupName = "groupName-" + currentTime;
        Token token = tokenService.getToken();
        GroupItem expectedGroup = GroupItem.builder().name(groupName.toLowerCase())
                .description("desc")
                .email(configurationService.getIdOfGroup(groupName)).build();

        Assert.assertEquals(expectedGroup, entitlementsV2Service.createGroup(groupName, token.getValue()));

        verifyConflictException(groupName, token.getValue());
    }

    @Override
    protected void cleanup() throws Exception {
        String tokenValue = tokenService.getToken().getValue();
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("groupName-" + currentTime), tokenValue);
    }

    private void verifyConflictException(String groupName, String token) throws Exception {
        RequestData requestData = RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath("groups")
                .token(token)
                .body(new Gson().toJson(GroupItem.builder().name(groupName).description("desc").build())).build();
        ClientResponse conflictResponse = httpClientService.send(requestData);
        Assert.assertEquals(409, conflictResponse.getStatus());
        Assert.assertEquals(expectedConflictResponse, new Gson().fromJson(conflictResponse.getEntity(String.class), ErrorResponse.class));
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        return RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath("groups")
                .body(new Gson().toJson(GroupItem.builder().name("groupName-" + System.currentTimeMillis())
                        .description("desc").build()))
                .build();
    }

}
