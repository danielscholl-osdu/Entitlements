package org.opengroup.osdu.entitlements.v2.azure.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.entitlements.v2.api.CreateGroupApi;
import org.opengroup.osdu.entitlements.v2.api.DeleteGroupApi;
import org.opengroup.osdu.entitlements.v2.api.DeleteMemberApi;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.azure.AzureAppProperties;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberResponseDto;
import org.opengroup.osdu.entitlements.v2.model.listmember.MemberDto;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ComponentScan("org.opengroup.osdu.entitlements.v2")
@WebMvcTest(controllers = {CreateGroupApi.class, DeleteGroupApi.class, DeleteMemberApi.class})
public class CreateMembershipsWorkflowSinglePartitionTest {
    /**
     * JWT token of service principal
     */
    private static final String SERVICE_P_JWT = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhQGIuY29tIiwiaXNzIjoi" +
            "cHJldmlldy5jb20iLCJhdWQiOiJ0ZXN0LmNvbSIsImlhdCI6MTU5MDYwMDgyNCwiZXhwIjoxNTkzNzEzODI0LCJwcm92aWRlciI6ImEu" +
            "Y29tIiwiY2xpZW50IjoidGVzdC5jb20iLCJ1c2VyaWQiOiJkYXRhZmllckBldmQtZGRsLXVzLWNvbW1vbi5pYW0uZ3NlcnZpY2VhY2Nv" +
            "dW50LmNvbSIsImVtYWlsIjoiYUBiLmNvbSIsImF1dGh6IjoiIiwibGFzdG5hbWUiOiJCIiwiZmlyc3RuYW1lIjoiQSIsImNvdW50cnki" +
            "OiIiLCJjb21wYW55IjoiIiwiam9idGl0bGUiOiIiLCJzdWJpZCI6InU1TFNOSnVoVWZhSDB4UDN1WVRuSXhWT0FKNHg2REp1Y1dzcHM1" +
            "Z0RvbzQiLCJpZHAiOiJvMzY1IiwiaGQiOiJzbGIuY29tIiwiZGVzaWQiOiJkYXRhZmllckBldmQtZGRsLXVzLWNvbW1vbi5pYW0uZ3Nl" +
            "cnZpY2VhY2NvdW50LmNvbSIsImNvbnRhY3RfZW1haWwiOiJkYXRhZmllckBldmQtZGRsLXVzLWNvbW1vbi5pYW0uZ3NlcnZpY2VhY2Nv" +
            "dW50LmNvbSIsInJ0X2hhc2giOiJ5UzFwdjdrQ29pNkdWV3ZzdzhLcXlBIiwianRpIjoiYjM0OTE5ZDUtZGMyOC00ZGM1LTkwODgtNmRj" +
            "ODU4NWQ2ZWJlIn0.LwpKthf1TFkhMoREurRAzDwBlEN3Pe93hQusfLC8DEU";
    private static final String jwtA = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhQGIuY29tIiwiaXNzIjoicHJldmll" +
            "dy5jb20iLCJhdWQiOiJ0ZXN0LmNvbSIsImlhdCI6MTU5MDYwMDgyNCwiZXhwIjoxNjA0NDI5ODMxLCJwcm92aWRlciI6ImEuY29tIiwi" +
            "Y2xpZW50IjoidGVzdC5jb20iLCJ1c2VyaWQiOiJhQGIuY29tIiwiZW1haWwiOiJ1c2VyMUBkZXNpZC5jb20iLCJhdXRoeiI6IiIsImxh" +
            "c3RuYW1lIjoiQiIsImZpcnN0bmFtZSI6IkEiLCJjb3VudHJ5IjoiIiwiY29tcGFueSI6IiIsImpvYnRpdGxlIjoiIiwic3ViaWQiOiJ1" +
            "NUxTTkp1aFVmYUgweFAzdVlUbkl4Vk9BSjR4NkRKdWNXc3BzNWdEb280IiwiaWRwIjoibzM2NSIsImhkIjoic2xiLmNvbSIsImRlc2lk" +
            "IjoiYUBkZXNpZC5jb20iLCJjb250YWN0X2VtYWlsIjoiYUBiLmNvbSIsInJ0X2hhc2giOiJ5UzFwdjdrQ29pNkdWV3ZzdzhLcXlBIiwi" +
            "anRpIjoiMjU3N2I0MTUtYTMwMi00NTY0LTlmNDItMmVkNWQyZjdmOWMwIn0.amFMAT5YWhkVANLqLbATVtSVxDwtss9uMdgTDvhftxM";
    private static final String jwtB = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJiQGIuY29tIiwiaXNzIjoicHJldmll" +
            "dy5jb20iLCJhdWQiOiJ0ZXN0LmNvbSIsImlhdCI6MTU5MDYwMDgyNCwiZXhwIjoxNjA0NDI5ODg2LCJwcm92aWRlciI6ImEuY29tIiwi" +
            "Y2xpZW50IjoidGVzdC5jb20iLCJ1c2VyaWQiOiJiQGIuY29tIiwiZW1haWwiOiJ1c2VyMkBkZXNpZC5jb20iLCJhdXRoeiI6IiIsImxh" +
            "c3RuYW1lIjoiQiIsImZpcnN0bmFtZSI6IkEiLCJjb3VudHJ5IjoiIiwiY29tcGFueSI6IiIsImpvYnRpdGxlIjoiIiwic3ViaWQiOiJ1" +
            "NUxTTkp1aFVmYUgweFAzdVlUbkl4Vk9BSjR4NkRKdWNXc3BzNWdEb280IiwiaWRwIjoibzM2NSIsImhkIjoic2xiLmNvbSIsImRlc2lk" +
            "IjoidXNlcjJAZGVzaWQuY29tIiwiY29udGFjdF9lbWFpbCI6ImFAYi5jb20iLCJydF9oYXNoIjoieVMxcHY3a0NvaTZHVld2c3c4S3F5" +
            "QSIsImp0aSI6IjBhYTNjZWVmLTBkZTUtNDgyOS1hNGYyLWFjZGNmYzIzMTc2YiJ9.P4-6xN3-loF4EDHqfib6kvman4qNr6H8Pe0IeyA" +
            "MlA4";
    private static final String jwtC = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJwcmV2aWV3LmNvbSIsImlhdCI6MTU5" +
            "MjQyODA4OCwiZXhwIjoxNjA0NDI5OTM2LCJhdWQiOiJ0ZXN0LmNvbSIsInN1YiI6InVzZXIzQGIuY29tIiwibGFzdG5hbWUiOiJKb2hu" +
            "bnkiLCJmaXJzdCI6IlJvY2tldCIsImVtYWlsIjoidXNlcjNAZGVzaWQuY29tIiwiZGVzaWQiOiJ1c2VyM0BkZXNpZC5jb20iLCJqdGki" +
            "OiJjZDI1MDRlZC1hNDA1LTRlYmItYjNlNS01NDExOGNhYTdkMDgifQ.1lLN3kg88ncki3bWk9aYz2obN8cXgtfKU5JSzvqOeiM";
    public static final String JWT_FOR_MEMBER_DELETION = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSl" +
            "dUIEJ1aWxkZXIiLCJpYXQiOjE1OTk2NzE0NTEsImV4cCI6MTYzMTIwNzQ1MSwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoidX" +
            "Nlci1mb3ItZGVsZXRpb25AZGVzaWQuY29tIiwiZW1haWwiOiJ1c2VyLWZvci1kZWxldGlvbkBkZXNpZC5jb20iLCJkZXNpZCI6InVzZX" +
            "ItZm9yLWRlbGV0aW9uQGRlc2lkLmNvbSJ9.JkDGIlylJUDwaZcAiYGd4VDpFZOgrabYB31DrxjLCpw";

    private static final String userA = "user1@desid.com";
    private static final String userB = "user2@desid.com";
    private static final String userC = "user3@desid.com";
    private static final String USER_FOR_DELETION = "user-for-deletion@desid.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private AzureAppProperties config;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private ITenantFactory tenantFactory;
    @MockBean
    private AuthorizationService authService;

    @Before
    public void before() {
        Mockito.when(config.getDomain()).thenReturn("contoso.com");
        Mockito.when(config.getProjectId()).thenReturn("evd-ddl-us-services");
        Mockito.when(config.isHttpAccepted()).thenReturn(true);
        Mockito.when(config.getInitialGroups()).thenCallRealMethod();
        Mockito.when(config.getGroupsOfServicePrincipal()).thenCallRealMethod();
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setProjectId("evd-ddl-us-common");
        tenantInfo.setDataPartitionId("common");
        tenantInfo.setServiceAccount("a@b.com");
        Mockito.when(tenantFactory.getTenantInfo("common")).thenReturn(tenantInfo);
        when(authService.isAuthorized(any(), any())).thenReturn(true);
    }

    @Test
    public void shouldRunWorkflowSuccessfully() throws Exception {
        performBootstrappingOfGroupsAndUsers();

        //running second time to prove the the api is idempotent
        performBootstrappingOfGroupsAndUsers();

        String rootUserGroup = "users@common.contoso.com";
        performAddMemberRequest(new AddMemberDto(userA, Role.MEMBER), rootUserGroup, SERVICE_P_JWT);
        performAddMemberRequest(new AddMemberDto(userB, Role.MEMBER), rootUserGroup, SERVICE_P_JWT);
        performAddMemberRequest(new AddMemberDto(userC, Role.MEMBER), rootUserGroup, SERVICE_P_JWT);

        //create users group
        performCreateGroupRequest("users.myusers.operators", jwtA);
        assertGroupsEquals(new String[]{
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListGroupRequest(jwtA));

        //create data group
        performCreateGroupRequest("data.mydata1.operators", jwtA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA));

        updateGroupMetadata();

        //add data groups to users group
        performCreateGroupRequest("data.mydata2.operators", jwtA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA));

        String dataGroup1 = "data.mydata1.operators@common.contoso.com";
        String dataGroup2 = "data.mydata2.operators@common.contoso.com";
        String userGroup1 = "users.myusers.operators@common.contoso.com";

        addDataGroup1ToDataGroup2(dataGroup1, dataGroup2);

        addUserGroupToDataGroups(userGroup1, dataGroup1, dataGroup2);

        addNewUserToUsersOperators(userB, jwtB, userGroup1);

        //create new root hierarchy
        performCreateGroupRequest("users.myusers2.operators", jwtC);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC));

        //add it to users operators
        String userGroup2 = "users.myusers2.operators@common.contoso.com";
        performAddMemberRequest(new AddMemberDto(userGroup2, Role.MEMBER), userGroup1, jwtA);

        assertMembersEquals(new String[]{userA,
                userB,
                "users.data.root@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListMemberRequest(userGroup1, jwtA));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC));

        removeDataGroup1FromDataGroup2(dataGroup1, dataGroup2);
        deleteDataGroup2(dataGroup2);
        deleteUserGroup1(userGroup1, dataGroup1);

        performAddMemberRequest(new AddMemberDto(userB, Role.OWNER), userGroup2, jwtC);
        performDeleteGroupRequest(userGroup2, jwtB);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com"}, performListGroupRequest(jwtB));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com"}, performListGroupRequest(jwtC));

        testGroupUpdateApi();

        testDeleteMemberApi();
    }

    private void removeDataGroup1FromDataGroup2(String dataGroup1, String dataGroup2) {
        performRemoveMemberRequest(dataGroup2, dataGroup1, jwtA);
        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, jwtA));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtB));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC));
    }

    private void deleteDataGroup2(String dataGroup2) {
        performDeleteGroupRequest(dataGroup2, jwtA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtB));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC));
    }

    private void deleteUserGroup1(String userGroup1, String dataGroup1) {
        performDeleteGroupRequest(userGroup1, jwtA);
        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com"}, performListMemberRequest(dataGroup1, jwtA));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com"}, performListGroupRequest(jwtB));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC));
    }

    private void addDataGroup1ToDataGroup2(String dataGroup1, String dataGroup2) {
        performAddMemberRequest(new AddMemberDto(dataGroup1, Role.MEMBER), dataGroup2, jwtA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, jwtA));
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com"}, performListMemberRequest(dataGroup1, jwtA));
    }

    private void addUserGroupToDataGroups(String userGroup, String dataGroup1, String dataGroup2) {
        performAddMemberRequest(new AddMemberDto(userGroup, Role.MEMBER), dataGroup1, jwtA);
        performAddMemberRequest(new AddMemberDto(userGroup, Role.MEMBER), dataGroup2, jwtA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, jwtA));

        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup1, jwtA));
    }

    private void addNewUserToUsersOperators(String newUser, String newUserJwt, String userGroup) {
        performAddMemberRequest(new AddMemberDto(newUser, Role.MEMBER), userGroup, jwtA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                userB}, performListMemberRequest(userGroup, jwtA));

        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(newUserJwt));
    }

    private void updateGroupMetadata() throws Exception {
        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(Arrays.asList("App1", "App2"))),
                "data.mydata1.operators@common.contoso.com", jwtA);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListGroupRequest(jwtA));

        //update group metadata with audience
        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(Collections.singletonList("test.com"))),
                "data.mydata1.operators@common.contoso.com", jwtA);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA));

        //update group metadata with empty list
        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(Collections.emptyList())),
                "data.mydata1.operators@common.contoso.com", jwtA);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA));
    }

    private void testGroupUpdateApi() throws Exception {
        // there aren't new groups
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com"
        }, performListGroupRequest(jwtA));

        checkThatUsersGroupCannotBeRenamedByExistingName();

        // data group was created
        String dataGroupName = "data.testdata.operators";
        String dataGroupEmail = dataGroupName + "@common.contoso.com";
        performCreateGroupRequest(dataGroupName, jwtA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail
        }, performListGroupRequest(jwtA));

        // users group 1 was created to be updated by a new email
        String usersGroup1Name = "users.testusers1.operators";
        String usersGroup1Email = usersGroup1Name + "@common.contoso.com";
        performCreateGroupRequest(usersGroup1Name, jwtA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email}, performListGroupRequest(jwtA));

        // users group 2 was created
        String usersGroup2Name = "users.testusers2.operators";
        String usersGroup2Email = usersGroup2Name + "@common.contoso.com";
        performCreateGroupRequest(usersGroup2Name, jwtA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email,
                usersGroup2Email}, performListGroupRequest(jwtA));

        // users group 1 was added to data group
        performAddMemberRequest(new AddMemberDto(usersGroup1Email, Role.MEMBER), dataGroupEmail, jwtA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                usersGroup1Email}, performListMemberRequest(dataGroupEmail, jwtA));

        // users group 2 was added to users group 1
        performAddMemberRequest(new AddMemberDto(usersGroup2Email, Role.MEMBER), usersGroup1Email, jwtA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                usersGroup2Email
        }, performListMemberRequest(usersGroup1Email, jwtA));

        // users group 1 email was updated
        String newUsersGroup1Name = "users.testusers3.operators";
        String newUsersGroup1Email = newUsersGroup1Name + "@common.contoso.com";

        checkThatUsersGroupCannotBeRenamedToBootstrapGroup(usersGroup1Email);
        checkBootstrappedGroupCannotBeRenamed(newUsersGroup1Name);

        performUpdateGroupRequest(Collections.singletonList(getRenameGroupOperation(newUsersGroup1Name)),
                usersGroup1Email, SERVICE_P_JWT);
        usersGroup1Email = newUsersGroup1Email;
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email, // updated email
                usersGroup2Email}, performListGroupRequest(jwtA));

        // data group has a member with updated email
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                usersGroup1Email // updated email
        }, performListMemberRequest(dataGroupEmail, jwtA));

        // users group 2 still is a member of users group 1 even after the updated email
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                usersGroup2Email}, performListMemberRequest(usersGroup1Email, jwtA)); // updated email

        // clean-up
        performDeleteGroupRequest(dataGroupEmail, SERVICE_P_JWT);
        performDeleteGroupRequest(usersGroup1Email, SERVICE_P_JWT);
        performDeleteGroupRequest(usersGroup2Email, SERVICE_P_JWT);
    }

    private void checkThatUsersGroupCannotBeRenamedByExistingName() throws Exception {
        // users group 1 was created
        String usersGroup1Name = "users.testusers1.operators";
        String usersGroup1Email = usersGroup1Name + "@common.contoso.com";

        //"users group 1 to be updated by a new email"
        performCreateGroupRequest(usersGroup1Name, jwtA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                usersGroup1Email}, performListGroupRequest(jwtA));

        // users group 2 was created
        String usersGroup2Name = "users.testusers2.operators";
        String usersGroup2Email = usersGroup2Name + "@common.contoso.com";
        performCreateGroupRequest(usersGroup2Name, jwtA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                usersGroup1Email,
                usersGroup2Email}, performListGroupRequest(jwtA));

        // bad request when users group 1 email is updated by users group 2 name
        mockMvc.perform(patch("/groups/{group_email}", usersGroup1Email)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(Collections.singletonList(getRenameGroupOperation(usersGroup2Name))))
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + SERVICE_P_JWT)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"code\":400,\"reason\":\"Bad Request\"," +
                        "\"message\":\"Invalid group name : \\\"users.testusers2.operators\\\", it already exists\"}"));

        // nothing changed
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                usersGroup1Email,
                usersGroup2Email}, performListGroupRequest(jwtA));

        // clean-up
        performDeleteGroupRequest(usersGroup1Email, SERVICE_P_JWT);
        performDeleteGroupRequest(usersGroup2Email, SERVICE_P_JWT);
    }

    private void checkThatUsersGroupCannotBeRenamedToBootstrapGroup(final String existingGroupEmail) throws Exception {
        String nameOfBootstrappedGroup = "users.datalake.ops";

        // bad request when users group 1 email is updated by users group 2 name
        mockMvc.perform(patch("/groups/{group_email}", existingGroupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(Collections.singletonList(getRenameGroupOperation(nameOfBootstrappedGroup))))
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + SERVICE_P_JWT)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"code\":400,\"reason\":\"Bad Request\"," +
                        "\"message\":\"Invalid group, group update API cannot work with bootstrapped groups\"}"));

        // nothing changed
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users.testusers1.operators@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.testdata.operators@common.contoso.com", "data.default.viewers@common.contoso.com",
                "users.testusers2.operators@common.contoso.com",
                "users@common.contoso.com"}, performListGroupRequest(jwtA));
    }

    private void checkBootstrappedGroupCannotBeRenamed(final String newGroupName) throws Exception {
        // users group 1 was created
        String bootstrappedGroupEmail = "users.datalake.ops@common.contoso.com";

        // bad request when users group 1 email is updated by users group 2 name
        mockMvc.perform(patch("/groups/{group_email}", bootstrappedGroupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(Collections.singletonList(getRenameGroupOperation(newGroupName))))
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + SERVICE_P_JWT)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"code\":400,\"reason\":\"Bad Request\"," +
                        "\"message\":\"Invalid group, group update API cannot work with bootstrapped groups\"}"));

        // nothing changed
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users.testusers1.operators@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.testdata.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.testusers2.operators@common.contoso.com",
                "users@common.contoso.com"}, performListGroupRequest(jwtA));
    }

    private void testDeleteMemberApi() throws Exception {
        String rootUserGroup = "users@common.contoso.com";

        String permissionGroupName = "users.test.editors";
        String permissionGroup = permissionGroupName + "@common.contoso.com";
        performCreateGroupRequest(permissionGroupName, SERVICE_P_JWT);

        assertTrue(performListGroupRequest(JWT_FOR_MEMBER_DELETION).getGroups().isEmpty());
        performAddMemberRequest(new AddMemberDto(USER_FOR_DELETION, Role.MEMBER), rootUserGroup, SERVICE_P_JWT);
        performAddMemberRequest(new AddMemberDto(USER_FOR_DELETION, Role.MEMBER), permissionGroup, SERVICE_P_JWT);

        assertGroupsEquals(new String[]{
                rootUserGroup,
                permissionGroup,
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com"
        }, performListGroupRequest(JWT_FOR_MEMBER_DELETION));
        performDeleteMemberRequest(USER_FOR_DELETION, SERVICE_P_JWT);
        assertTrue(performListGroupRequest(JWT_FOR_MEMBER_DELETION).getGroups().isEmpty());

        performDeleteGroupRequest(permissionGroup, SERVICE_P_JWT);
    }

    private void performCreateGroupRequest(String groupName, String jwt) {
        try {
            CreateGroupDto dto = new CreateGroupDto(groupName, groupName + ": description");
            mockMvc.perform(MockMvcRequestBuilders.post("/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common")
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(MockMvcResultMatchers.status().isCreated());
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
    }

    private void performDeleteGroupRequest(String groupEmail, String jwt) {
        try {
            mockMvc.perform(MockMvcRequestBuilders.delete("/groups/{group_email}", groupEmail)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                    .andExpect(MockMvcResultMatchers.status().isNoContent());
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
    }

    private void performAddMemberRequest(AddMemberDto dto, String groupEmail, String jwt) {
        try {
            mockMvc.perform(MockMvcRequestBuilders.post("/groups/{group_email}/members", groupEmail)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common")
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
    }

    private ListGroupResponseDto performListGroupRequest(String jwt) {
        try {
            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common"));
            return objectMapper.readValue(
                    result.andExpect(MockMvcResultMatchers.status().isOk()).andReturn().getResponse().getContentAsString(),
                    ListGroupResponseDto.class);
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
        return ListGroupResponseDto.builder().build();
    }

    private ListMemberResponseDto performListMemberRequest(String groupEmail, String jwt) {
        try {
            ResultActions result = mockMvc.perform(get("/groups/{group_email}/members", groupEmail)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                    .andExpect(status().isOk());

            return new Gson().fromJson(
                    result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                    ListMemberResponseDto.class);
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
        return ListMemberResponseDto.builder().build();
    }

    private void performRemoveMemberRequest(String groupEmail, String memberEmail, String jwt) {
        try {
            mockMvc.perform(delete("/groups/{group_email}/members/{member_email}", groupEmail, memberEmail)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                    .andExpect(status().isNoContent());
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
    }

    private void performUpdateGroupRequest(List<UpdateGroupOperation> operations, String groupEmail, String jwt) throws Exception {
        mockMvc.perform(patch("/groups/{group_email}", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .content(objectMapper.writeValueAsString(operations)))
                .andExpect(status().isOk());
    }

    private void performDeleteMemberRequest(String memberEmail, String jwt) throws Exception {
        mockMvc.perform(delete("/members/{member_email}", memberEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                .andExpect(status().isNoContent());
    }

    private void assertGroupsEquals(String[] expectedGroupEmails, ListGroupResponseDto dto) {
        Set<String> actualGroups = new HashSet<>(
                Lists.transform(new ArrayList<>(dto.getGroups()), ParentReference::getId));
        Assertions.assertEquals(new HashSet<>(Arrays.asList(expectedGroupEmails)), actualGroups);
    }

    private void assertMembersEquals(final String[] expectedMemberEmails, final ListMemberResponseDto dto) {
        Set<String> actualMembers = new HashSet<>(Lists.transform(dto.getMembers(), MemberDto::getEmail));
        assertEquals(new HashSet<>(Arrays.asList(expectedMemberEmails)), actualMembers);
    }

    private UpdateGroupOperation getRenameGroupOperation(String newGroupName) {
        return UpdateGroupOperation.builder()
                .operation("replace")
                .path("/name")
                .value(Collections.singletonList(newGroupName)).build();
    }

    private UpdateGroupOperation getUpdateAppIdsOperation(List<String> appIds) {
        return UpdateGroupOperation.builder()
                .operation("replace")
                .path("/appIds")
                .value(appIds).build();
    }

    private void performBootstrappingOfGroupsAndUsers() throws Exception {
        final RequestBuilder request = MockMvcRequestBuilders.post("/tenant-provisioning")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header("authorization", "Bearer " + SERVICE_P_JWT)
                .header("data-partition-id", "common");
        mockMvc.perform(request).andDo(MockMvcResultHandlers.print()).andExpect(status().isOk());
        assertGroupsEquals(new String[]{"service.entitlements.user@common.contoso.com",
                "users.datalake.viewers@common.contoso.com", "service.search.admin@common.contoso.com",
                "data.default.owners@common.contoso.com", "users.datalake.editors@common.contoso.com",
                "service.messaging.admin@common.contoso.com", "service.storage.creator@common.contoso.com",
                "users.data.root@common.contoso.com", "service.search.user@common.contoso.com",
                "users@common.contoso.com", "service.entitlements.admin@common.contoso.com",
                "service.legal.user@common.contoso.com", "data.default.viewers@common.contoso.com",
                "users.datalake.ops@common.contoso.com", "service.storage.admin@common.contoso.com",
                "service.legal.admin@common.contoso.com", "users.datalake.admins@common.contoso.com",
                "service.plugin.user@common.contoso.com", "service.plugin.admin@common.contoso.com",
                "service.messaging.user@common.contoso.com", "service.storage.viewer@common.contoso.com",
                "service.legal.editor@common.contoso.com"}, performListGroupRequest(SERVICE_P_JWT));
    }
}
