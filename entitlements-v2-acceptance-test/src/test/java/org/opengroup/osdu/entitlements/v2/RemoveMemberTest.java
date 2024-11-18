package org.opengroup.osdu.entitlements.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.model.GroupType;
import org.opengroup.osdu.entitlements.v2.model.Token;
import org.opengroup.osdu.entitlements.v2.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.model.request.GetGroupsRequestData;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.model.response.ListMemberResponse;
import org.opengroup.osdu.entitlements.v2.util.CommonConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.OpenIDTokenProvider;


public class RemoveMemberTest extends AcceptanceBaseTest {

    public RemoveMemberTest() {
        super(new CommonConfigurationService(), new OpenIDTokenProvider());
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        final String groupEmail = configurationService.getIdOfGroup("group");
        return RequestData.builder()
                .method("DELETE").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members/%s", groupEmail, "member@test.com"))
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
        assertFalse(isChildGroupAMember);
        entitlementsV2Service.getMembers(childGroupItem.getEmail(), token.getValue());
    }

    /**
     * 1) Create a Group
     * 2) Add user to both groups as member, newly created group as well as elementary data-partition users group
     * 3) Attempt removing child group from elementary data-partition users group
     * 4) Check the attempt should fail
     * 5) Remove the user from newly created group
     * 6) Check user does not exist in that group
     * 7) Remove the user from elementary data-partition users group
     * 8) Check the user is removed
     * 9) Delete the newly created group
     */
    @Test
    public void shouldFailToRemoveMemberFromElementaryDPGroupIfUserIsMemberOfOtherGroups() throws Exception {
        Token token = tokenService.getToken();
        String groupName = "group-" + currentTime;
        String userName = configurationService.getMemberMailId_toBeDeleted(currentTime);
        String elementaryDataPartitionUsersGroup = String.format("users@%s.%s", configurationService.getTenantId(), configurationService.getDomain());

        //Create a Group
        GroupItem groupItem = entitlementsV2Service.createGroup(groupName, token.getValue());

        //Add the user to above group
        AddMemberRequestData addMemberRequestDataToNewGroup = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(userName).build();
        entitlementsV2Service.addMember(addMemberRequestDataToNewGroup, token.getValue());

        //Add the user to elementary data-partition group
        AddMemberRequestData addMemberRequestDataToElementaryDPGroup = AddMemberRequestData.builder()
                .groupEmail(elementaryDataPartitionUsersGroup).role("MEMBER").memberEmail(userName).build();
        entitlementsV2Service.addMember(addMemberRequestDataToElementaryDPGroup, token.getValue());

        //attempt removal from elementary data partition group
        RequestData requestData = RequestData.builder()
                .method("DELETE").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members/%s", elementaryDataPartitionUsersGroup, userName))
                .token(token.getValue())
                .build();
        CloseableHttpResponse response = httpClientService.send(requestData);
        assertEquals(400, response.getCode());

        //use Delete MEMBER to delete the user
        entitlementsV2Service.deleteMember(userName, token.getValue());

        //check the member should not be present into any groups
        GetGroupsRequestData getGroupsRequestData = GetGroupsRequestData
                .builder()
                .memberEmail(userName)
                .type(GroupType.NONE)
                .build();
        //check removal should be successful
        ListGroupResponse listGroupResponse = entitlementsV2Service.getGroups(getGroupsRequestData, token.getValue());
        assertTrue(listGroupResponse.groups.isEmpty());
    }


    @Override
    protected void cleanup() throws Exception {
        String tokenValue = tokenService.getToken().getValue();
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("group-" + currentTime), tokenValue);
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("child-group-name" + currentTime), tokenValue);
    }
}
