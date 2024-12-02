package org.opengroup.osdu.entitlements.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.model.response.ErrorResponse;
import org.opengroup.osdu.entitlements.v2.model.response.ListMemberResponse;
import org.opengroup.osdu.entitlements.v2.util.CommonConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.TokenTestUtils;

import java.util.HashMap;
import java.util.Map;

public class AddMemberTest extends AcceptanceBaseTest {

    public AddMemberTest() {
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
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("child-groupName-" + currentTime), testUtils.getToken());
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("groupName-" + currentTime), testUtils.getToken());
        this.testUtils = null;
        //        TODO add revoke member logic for memberEmail and ownerMemberEmail
    }

    @Test
    public void shouldAddMemberSuccessfully() throws Exception {
        String groupName = "groupName-" + currentTime;
        String childGroupName = "child-groupName-" + currentTime;
        String memberEmail = this.configurationService.getMemberMailId();
        String ownerMemberEmail = this.configurationService.getOwnerMailId();

        GroupItem groupItem = entitlementsV2Service.createGroup(groupName, testUtils.getToken());

        AddMemberRequestData addOwnerMemberRequestData = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("OWNER").memberEmail(ownerMemberEmail).build();
        entitlementsV2Service.addMember(addOwnerMemberRequestData, testUtils.getToken());

        AddMemberRequestData addMemberRequestData = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(memberEmail).build();
        entitlementsV2Service.addMember(addMemberRequestData, testUtils.getToken());

        verifyConflictError(addMemberRequestData, testUtils.getToken());

        GroupItem childGroupItem = entitlementsV2Service.createGroup(childGroupName, testUtils.getToken());
        AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(childGroupItem.getEmail()).build();
        entitlementsV2Service.addMember(addGroupMemberRequestData, testUtils.getToken());
        ListMemberResponse listMemberResponse = entitlementsV2Service.getMembers(groupItem.getEmail(), testUtils.getToken());

        assertEquals(4, listMemberResponse.getMembers().size());
        verifyMemberInResponse(listMemberResponse, "MEMBER", childGroupItem.getEmail());
        verifyMemberInResponse(listMemberResponse, "MEMBER", memberEmail.toLowerCase());
        verifyMemberInResponse(listMemberResponse, "OWNER", ownerMemberEmail.toLowerCase());
        verifyMemberInResponse(listMemberResponse, "OWNER", testUtils.getUserId());
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("role", "MEMBER");
        requestBody.put("email", this.configurationService.getMemberMailId());
        return RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members", configurationService.getIdOfGroup("users")))
                .body(new Gson().toJson(requestBody))
                .build();
    }

    private void verifyMemberInResponse(ListMemberResponse listMemberResponse, String role, String memberEmail) {
        boolean isMemberCreated = listMemberResponse.getMembers().stream()
                .filter(memberItem -> memberEmail.equals(memberItem.getEmail()))
                .anyMatch(memberItem -> memberItem.getRole().equals(role));
        assertTrue(isMemberCreated);
    }

    private void verifyConflictError(AddMemberRequestData addMemberRequestData, String token) throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("role", addMemberRequestData.getRole().toUpperCase());
        requestBody.put("email", addMemberRequestData.getMemberEmail());
        RequestData requestData = RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members", addMemberRequestData.getGroupEmail()))
                .token(token)
                .body(new Gson().toJson(requestBody)).build();
        CloseableHttpResponse response = httpClientService.send(requestData);
        assertEquals(409, response.getCode());
        String errorMessage = String.format("%s is already a member of group %s",
                addMemberRequestData.getMemberEmail().toLowerCase(), addMemberRequestData.getGroupEmail());
        ErrorResponse expectedConflictResponse = ErrorResponse.builder().code(409).reason("Conflict")
                .message(errorMessage).build();
        assertEquals(expectedConflictResponse, new Gson().fromJson(EntityUtils.toString(response.getEntity()), ErrorResponse.class));
    }
}
