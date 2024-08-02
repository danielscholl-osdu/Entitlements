package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import com.google.gson.Gson;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.AddMemberTest;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ErrorResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListMemberResponse;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

import java.util.HashMap;
import java.util.Map;

public class AddMemberAzureTest extends AddMemberTest {
    private static final String APP_SP_OID =
            System.getProperty("AZURE_AD_NO_DATA_ACCESS_SP_OID", System.getenv("AZURE_AD_NO_DATA_ACCESS_SP_OID"));

    private static final String GROUP_OID =
            System.getProperty("AZURE_AD_GROUP_OID", System.getenv("AZURE_AD_GROUP_OID"));

    private static final String isOidValidationTrue = System.getProperty("AZURE_OID_VALIDATION_INTEGRATION_TESTS", System.getenv("AZURE_OID_VALIDATION_INTEGRATION_TESTS"));

    public AddMemberAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }

    @Test
    public void addMemberBySPOid_shouldFailWith400() throws Exception{
        //when variable is unset, test will be ignored
        Assume.assumeNotNull(isOidValidationTrue);
        //when its set and false, this test will be ignored
        Assume.assumeTrue(isOidValidationTrue.equals("true"));

        String groupName = "groupName-" + currentTime;
        String memberEmail = APP_SP_OID;
        Token token = tokenService.getToken();

        GroupItem groupItem = entitlementsV2Service.createGroup(groupName, token.getValue());

        AddMemberRequestData addMemberRequestData = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(memberEmail).build();

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("role", addMemberRequestData.getRole().toUpperCase());
        requestBody.put("email", addMemberRequestData.getMemberEmail());
        RequestData requestData = RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members", addMemberRequestData.getGroupEmail()))
                .token(token.getValue())
                .body(new Gson().toJson(requestBody)).build();

        CloseableHttpResponse response = httpClientService.send(requestData);

        Assert.assertEquals(400, response.getCode());
        String errorMessage = "The given OID matches with a provisioned Service Principal. They should be added to OSDU groups via their Client ID. Please use the correct ID as the input";
        ErrorResponse expectedConflictResponse = ErrorResponse.builder().code(400).reason("Bad Request")
                .message(errorMessage).build();
        Assert.assertEquals(expectedConflictResponse, new Gson().fromJson(EntityUtils.toString(response.getEntity()), ErrorResponse.class));
    }

    @Test
    public void addMemberByGroupOID_shouldPassWith200() throws Exception{

        //when variable is unset, test will be ignored
        Assume.assumeNotNull(isOidValidationTrue);
        //when its set and false, this test will be ignored
        Assume.assumeTrue(isOidValidationTrue.equals("true"));

        String groupName = "groupName-" + currentTime;
        String memberEmail = GROUP_OID;
        Token token = tokenService.getToken();

        GroupItem groupItem = entitlementsV2Service.createGroup(groupName, token.getValue());

        AddMemberRequestData addMemberRequestData = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(memberEmail).build();

        entitlementsV2Service.addMember(addMemberRequestData, token.getValue());
        ListMemberResponse listMemberResponse = entitlementsV2Service.getMembers(groupItem.getEmail(), token.getValue());

        Assert.assertEquals(2, listMemberResponse.getMembers().size());
        verifyMemberInResponse(listMemberResponse, "MEMBER", memberEmail.toLowerCase());
        verifyMemberInResponse(listMemberResponse, "OWNER", token.getUserId());
    }

    private void verifyMemberInResponse(ListMemberResponse listMemberResponse, String role, String memberEmail) {
        boolean isMemberCreated = listMemberResponse.getMembers().stream()
                .filter(memberItem -> memberEmail.equals(memberItem.getEmail()))
                .anyMatch(memberItem -> memberItem.getRole().equals(role));
        Assert.assertTrue(isMemberCreated);
    }
}
