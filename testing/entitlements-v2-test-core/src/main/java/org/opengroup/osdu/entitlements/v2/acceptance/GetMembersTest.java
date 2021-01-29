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

public abstract class GetMembersTest extends AcceptanceBaseTest {

    public GetMembersTest(ConfigurationService configurationService, TokenService tokenService) {
        super(configurationService, tokenService);
    }

    @Override
    protected RequestData getRequestDataForUnauthorizedTest() {
        Token noAccessToken = tokenService.getNoDataAccessToken();
        final String groupEmail = configurationService.getIdOfGroup("group");
        return RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members", groupEmail))
                .token(noAccessToken.getValue())
                .build();
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        final String groupEmail = configurationService.getIdOfGroup("group");
        return RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members", groupEmail))
                .build();
    }

    @Override
    protected RequestData getRequestDataForUnauthorizedPartition() {
        Token token = tokenService.getToken();
        final String groupEmail = configurationService.getIdOfGroup("group");
        return RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getUnauthorizedTenantId())
                .relativePath(String.format("groups/%s/members", groupEmail))
                .token(token.getValue())
                .build();
    }

    /**
     * 1) create group
     * 2) create child group
     * 3) add child group 1 to group as member
     * 4) add child group 2 to group as member
     * 5) add child group 3 to group as member
     * 6) List members of the group and check child group 1,2,3 are a member of group
     * 7) delete child group 1
     * 8) delete child group 2
     * 9) delete child group 3
     * 10) delete group
     */
    @Test
    public void shouldSuccessfullyListMember() throws Exception {
        String groupName = "group-" + currentTime;
        String childGroupName1 = "child-group-name-1" + currentTime;
        String childGroupName2 = "child-group-name-2" + currentTime;
        String childGroupName3 = "child-group-name-3" + currentTime;
        Token token = tokenService.getToken();
        GroupItem groupItem = entitlementsV2Service.createGroup(groupName, token.getValue());
        GroupItem childGroupItem1 = entitlementsV2Service.createGroup(childGroupName1, token.getValue());
        GroupItem childGroupItem2 = entitlementsV2Service.createGroup(childGroupName2, token.getValue());
        GroupItem childGroupItem3 = entitlementsV2Service.createGroup(childGroupName3, token.getValue());

        AddMemberRequestData addMemberRequestData1 = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(childGroupItem1.getEmail()).build();
        AddMemberRequestData addMemberRequestData2 = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(childGroupItem2.getEmail()).build();
        AddMemberRequestData addMemberRequestData3 = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(childGroupItem3.getEmail()).build();
        entitlementsV2Service.addMember(addMemberRequestData1, token.getValue());
        entitlementsV2Service.addMember(addMemberRequestData2, token.getValue());
        entitlementsV2Service.addMember(addMemberRequestData3, token.getValue());

        ListMemberResponse listMemberResponse1 = entitlementsV2Service.getMembers(groupItem.getEmail(), token.getValue());
        boolean isChildGroup1AMember = listMemberResponse1.getMembers()
                .stream().anyMatch(memberItem -> memberItem.getEmail().equals(childGroupItem1.getEmail()));
        Assert.assertTrue(isChildGroup1AMember);
        ListMemberResponse listMemberResponse2 = entitlementsV2Service.getMembers(groupItem.getEmail(), token.getValue());
        boolean isChildGroup2AMember = listMemberResponse2.getMembers()
                .stream().anyMatch(memberItem -> memberItem.getEmail().equals(childGroupItem2.getEmail()));
        Assert.assertTrue(isChildGroup2AMember);
        ListMemberResponse listMemberResponse3 = entitlementsV2Service.getMembers(groupItem.getEmail(), token.getValue());
        boolean isChildGroup3AMember = listMemberResponse3.getMembers()
                .stream().anyMatch(memberItem -> memberItem.getEmail().equals(childGroupItem3.getEmail()));
        Assert.assertTrue(isChildGroup3AMember);
    }

    @Override
    protected void cleanup() throws Exception {
        String tokenValue = tokenService.getToken().getValue();
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("child-group-name-1" + currentTime), tokenValue);
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("child-group-name-2" + currentTime), tokenValue);
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("child-group-name-3" + currentTime), tokenValue);
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("group-" + currentTime), tokenValue);
    }
}
