package org.opengroup.osdu.entitlements.v2.acceptance;

import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListMemberResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

public abstract class RemoveMemberTest extends AcceptanceBaseTest {

    public RemoveMemberTest(ConfigurationService configurationService, TokenService tokenService) {
        super(configurationService, tokenService);
    }

    @Override
    protected RequestData getRequestDataForUnauthorizedTest() {
        Token noAccessToken = tokenService.getNoDataAccessToken();
        final String groupEmail = configurationService.getIdOfGroup("group");
        return RequestData.builder()
                .method("DELETE").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members/%s", groupEmail, "member@test.com"))
                .token(noAccessToken.getValue())
                .build();
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        final String groupEmail = configurationService.getIdOfGroup("group");
        return RequestData.builder()
                .method("DELETE").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members/%s", groupEmail, "member@test.com"))
                .build();
    }

    @Override
    protected RequestData getRequestDataForUnauthorizedPartition() {
        Token token = tokenService.getToken();
        final String groupEmail = configurationService.getIdOfGroup("group");
        return RequestData.builder()
                .method("DELETE").dataPartitionId(configurationService.getUnauthorizedTenantId())
                .relativePath(String.format("groups/%s/members/%s", groupEmail, "member@test.com"))
                .token(token.getValue())
                .build();
    }
    /**
     * 1) create group
     * 2) create child group
     * 3) add child group to group as member
     * 4) remove child group from group
     * 5) check child group is not member of group
     * 6) check child group exists
     * 7) delete group
     * 8) delete child group
     */
    @Test
    public void shouldSuccessfullyRemoveMember() throws Exception {
        String groupName = "group-" + currentTime;
        String childGroupName = "child-group-name" + currentTime;
        Token token = tokenService.getToken();
        GroupItem groupItem = entitlementsV2Service.createGroup(groupName, token.getValue());
        GroupItem childGroupItem = entitlementsV2Service.createGroup(childGroupName, token.getValue());
        AddMemberRequestData addMemberRequestData = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(childGroupItem.getEmail()).build();
        entitlementsV2Service.addMember(addMemberRequestData, token.getValue());

        entitlementsV2Service.removeMember(groupItem.getEmail(), childGroupItem.getEmail(), token.getValue());

        ListMemberResponse listMemberResponse = entitlementsV2Service.getMembers(groupItem.getEmail(), token.getValue());
        boolean isChildGroupAMember = listMemberResponse.getMembers()
                .stream().anyMatch(memberItem -> memberItem.getEmail().equals(childGroupItem.getEmail()));
        Assert.assertFalse(isChildGroupAMember);
        entitlementsV2Service.getMembers(childGroupItem.getEmail(), token.getValue());
    }

    @Override
    protected void cleanup() throws Exception {
        String tokenValue = tokenService.getToken().getValue();
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("group-" + currentTime), tokenValue);
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("child-group-name" + currentTime), tokenValue);
    }
}
