package org.opengroup.osdu.entitlements.v2.acceptance;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.MemberItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ErrorResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListMemberResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

public abstract class CreateGroupTest extends AcceptanceBaseTest {
    private final ErrorResponse expectedConflictResponse = ErrorResponse.builder().code(409).reason("Conflict")
            .message("This group already exists").build();

    public CreateGroupTest(ConfigurationService configurationService, TokenService tokenService) {
        super(configurationService, tokenService);
    }

    private boolean isSecondGroupMemberofFirst(String firstEmail, String secondEmail, String token) throws Exception {
        ListMemberResponse response = entitlementsV2Service.getMembers(firstEmail, token);
        for (MemberItem item : response.getMembers()) {
            if (item.getEmail().equalsIgnoreCase(secondEmail)) {
                return true;
            }
        }
        return false;
    }

    private void verifyRootGroupMembership(GroupItem createdGroup, String token) throws Exception {
        String createdEmail = createdGroup.getEmail();
        String suffix = createdEmail.split("@")[1];
        String rootEmail = String.format("users.data.root@%s", suffix);

        boolean rootIsMemberOfDataGroup = isSecondGroupMemberofFirst(createdEmail, rootEmail, token);
        boolean dataGroupIsMemberOfRoot = isSecondGroupMemberofFirst(rootEmail, createdEmail, token);

        assertNotEquals("Ensure that the flag to disable the automatic add of the root group to newly created data groups is enabled (flag name: `disable-data-root-group-hierarchy`)",
                    dataGroupIsMemberOfRoot, rootIsMemberOfDataGroup);
        assertFalse("Ensure that the newly created data group is NOT a member of the root group", dataGroupIsMemberOfRoot);
        assertTrue("Ensure that the root group is a member of the newly created data group", rootIsMemberOfDataGroup);
    }

    @Test
    public void shouldAddDataRootAsMemberOfNewDataGroup() throws Exception {
        String groupName = "data.groupName-" + currentTime;
        Token token = tokenService.getToken();
        GroupItem expectedGroup = GroupItem.builder().name(groupName.toLowerCase())
                                           .description("desc")
                                           .email(configurationService.getIdOfGroup(groupName)).build();

        Assert.assertEquals(expectedGroup, entitlementsV2Service.createGroup(groupName, token.getValue()));

        verifyRootGroupMembership(expectedGroup, token.getValue());
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
        CloseableHttpResponse conflictResponse = httpClientService.send(requestData);
        Assert.assertEquals(409, conflictResponse.getCode());
        Assert.assertEquals(expectedConflictResponse, new Gson().fromJson(EntityUtils.toString(conflictResponse.getEntity()), ErrorResponse.class));
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
