package org.opengroup.osdu.entitlements.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.model.response.MembersCountResponse;
import org.opengroup.osdu.entitlements.v2.util.CommonConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.TokenTestUtils;
import org.springframework.http.HttpStatus;



public class GetMembersCountTest extends AcceptanceBaseTest {

    public GetMembersCountTest() {
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
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("child-group-name-1" + currentTime), testUtils.getToken());
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("child-group-name-2" + currentTime), testUtils.getToken());
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("child-group-name-3" + currentTime), testUtils.getToken());
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("group-" + currentTime), testUtils.getToken());
        this.testUtils = null;
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        final String groupEmail = configurationService.getIdOfGroup("group");
        return RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/membersCount", groupEmail))
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
    public void shouldSuccessfullyCountMembers() throws Exception {
        String groupName = "group-" + currentTime;
        String childGroupName1 = "child-group-name-1" + currentTime;
        String childGroupName2 = "child-group-name-2" + currentTime;
        String childGroupName3 = "child-group-name-3" + currentTime;

        GroupItem groupItem = entitlementsV2Service.createGroup(groupName, testUtils.getToken());
        GroupItem childGroupItem1 = entitlementsV2Service.createGroup(childGroupName1, testUtils.getToken());
        GroupItem childGroupItem2 = entitlementsV2Service.createGroup(childGroupName2, testUtils.getToken());
        GroupItem childGroupItem3 = entitlementsV2Service.createGroup(childGroupName3, testUtils.getToken());

        AddMemberRequestData addMemberRequestData1 = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(childGroupItem1.getEmail()).build();
        AddMemberRequestData addMemberRequestData2 = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(childGroupItem2.getEmail()).build();
        AddMemberRequestData addMemberRequestData3 = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(childGroupItem3.getEmail()).build();
        entitlementsV2Service.addMember(addMemberRequestData1, testUtils.getToken());
        entitlementsV2Service.addMember(addMemberRequestData2, testUtils.getToken());
        entitlementsV2Service.addMember(addMemberRequestData3, testUtils.getToken());

        MembersCountResponse membersCountResponse = entitlementsV2Service.getMembersCount(groupItem.getEmail(), testUtils.getToken());

        boolean isGroupReturnedSameAsProvided = membersCountResponse.getGroupEmail().equals(groupItem.getEmail());
        assertTrue(isGroupReturnedSameAsProvided);
        assertEquals(4, membersCountResponse.getMembersCount());
    }

    @Test
    public void shouldReturnBadRequestWhenMakingHttpRequestWithInvalidUrl() throws Exception {
        RequestData requestData = RequestData.builder()
                .method("GET")
                .relativePath("groups/%3B/membersCount")
                .dataPartitionId(configurationService.getTenantId())
                .token(testUtils.getToken())
                .build();

        CloseableHttpResponse closeableHttpResponse = httpClientService.send(requestData);

        assertEquals(HttpStatus.BAD_REQUEST.value(), closeableHttpResponse.getCode());
    }
}
