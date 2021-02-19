package org.opengroup.osdu.entitlements.v2.gcp.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.entitlements.v2.api.AddMemberApi;
import org.opengroup.osdu.entitlements.v2.api.CreateGroupApi;
import org.opengroup.osdu.entitlements.v2.api.DeleteGroupApi;
import org.opengroup.osdu.entitlements.v2.api.ListGroupApi;
import org.opengroup.osdu.entitlements.v2.api.ListMemberApi;
import org.opengroup.osdu.entitlements.v2.api.RemoveMemberApi;
import org.opengroup.osdu.entitlements.v2.api.UpdateGroupApi;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.di.WhitelistSvcAccBeanConfiguration;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.service.PartitionRedisInstanceService;
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
import org.springframework.test.web.servlet.ResultActions;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ComponentScan("org.opengroup.osdu.entitlements.v2")
@WebMvcTest(controllers = {CreateGroupApi.class, DeleteGroupApi.class, AddMemberApi.class, RemoveMemberApi.class,
        ListMemberApi.class, ListGroupApi.class, UpdateGroupApi.class})
public class CreateMembershipsWorkflowSinglePartitionTest {
    private static final String userA = "user1@desid.com";
    private static final String jwtA = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhQGIuY29tIiwiaXNzIjoicHJldmlldy5jb20iLCJhdWQiOiJ0ZXN0LmNvbSIsImlhdCI6MTU5MDYwMDgyNCwiZXhwIjoxNjA0NDI5ODMxLCJwcm92aWRlciI6ImEuY29tIiwiY2xpZW50IjoidGVzdC5jb20iLCJ1c2VyaWQiOiJhQGIuY29tIiwiZW1haWwiOiJ1c2VyMUBkZXNpZC5jb20iLCJhdXRoeiI6IiIsImxhc3RuYW1lIjoiQiIsImZpcnN0bmFtZSI6IkEiLCJjb3VudHJ5IjoiIiwiY29tcGFueSI6IiIsImpvYnRpdGxlIjoiIiwic3ViaWQiOiJ1NUxTTkp1aFVmYUgweFAzdVlUbkl4Vk9BSjR4NkRKdWNXc3BzNWdEb280IiwiaWRwIjoibzM2NSIsImhkIjoic2xiLmNvbSIsImRlc2lkIjoiYUBkZXNpZC5jb20iLCJjb250YWN0X2VtYWlsIjoiYUBiLmNvbSIsInJ0X2hhc2giOiJ5UzFwdjdrQ29pNkdWV3ZzdzhLcXlBIiwianRpIjoiMjU3N2I0MTUtYTMwMi00NTY0LTlmNDItMmVkNWQyZjdmOWMwIn0.amFMAT5YWhkVANLqLbATVtSVxDwtss9uMdgTDvhftxM";
    private static final String userB = "user2@desid.com";
    private static final String jwtB = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJiQGIuY29tIiwiaXNzIjoicHJldmlldy5jb20iLCJhdWQiOiJ0ZXN0LmNvbSIsImlhdCI6MTU5MDYwMDgyNCwiZXhwIjoxNjA0NDI5ODg2LCJwcm92aWRlciI6ImEuY29tIiwiY2xpZW50IjoidGVzdC5jb20iLCJ1c2VyaWQiOiJiQGIuY29tIiwiZW1haWwiOiJ1c2VyMkBkZXNpZC5jb20iLCJhdXRoeiI6IiIsImxhc3RuYW1lIjoiQiIsImZpcnN0bmFtZSI6IkEiLCJjb3VudHJ5IjoiIiwiY29tcGFueSI6IiIsImpvYnRpdGxlIjoiIiwic3ViaWQiOiJ1NUxTTkp1aFVmYUgweFAzdVlUbkl4Vk9BSjR4NkRKdWNXc3BzNWdEb280IiwiaWRwIjoibzM2NSIsImhkIjoic2xiLmNvbSIsImRlc2lkIjoidXNlcjJAZGVzaWQuY29tIiwiY29udGFjdF9lbWFpbCI6ImFAYi5jb20iLCJydF9oYXNoIjoieVMxcHY3a0NvaTZHVld2c3c4S3F5QSIsImp0aSI6IjBhYTNjZWVmLTBkZTUtNDgyOS1hNGYyLWFjZGNmYzIzMTc2YiJ9.P4-6xN3-loF4EDHqfib6kvman4qNr6H8Pe0IeyAMlA4";
    private static final String userC = "user3@desid.com";
    private static final String jwtC = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJwcmV2aWV3LmNvbSIsImlhdCI6MTU5MjQyODA4OCwiZXhwIjoxNjA0NDI5OTM2LCJhdWQiOiJ0ZXN0LmNvbSIsInN1YiI6InVzZXIzQGIuY29tIiwibGFzdG5hbWUiOiJKb2hubnkiLCJmaXJzdCI6IlJvY2tldCIsImVtYWlsIjoidXNlcjNAZGVzaWQuY29tIiwiZGVzaWQiOiJ1c2VyM0BkZXNpZC5jb20iLCJqdGkiOiJjZDI1MDRlZC1hNDA1LTRlYmItYjNlNS01NDExOGNhYTdkMDgifQ.1lLN3kg88ncki3bWk9aYz2obN8cXgtfKU5JSzvqOeiM";
    private static final String datafierJWT = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhQGIuY29tIiwiaXNzIjoicHJldmlldy5jb20iLCJhdWQiOiJ0ZXN0LmNvbSIsImlhdCI6MTU5MDYwMDgyNCwiZXhwIjoxNTkzNzEzODI0LCJwcm92aWRlciI6ImEuY29tIiwiY2xpZW50IjoidGVzdC5jb20iLCJ1c2VyaWQiOiJkYXRhZmllckBldmQtZGRsLXVzLWNvbW1vbi5pYW0uZ3NlcnZpY2VhY2NvdW50LmNvbSIsImVtYWlsIjoiYUBiLmNvbSIsImF1dGh6IjoiIiwibGFzdG5hbWUiOiJCIiwiZmlyc3RuYW1lIjoiQSIsImNvdW50cnkiOiIiLCJjb21wYW55IjoiIiwiam9idGl0bGUiOiIiLCJzdWJpZCI6InU1TFNOSnVoVWZhSDB4UDN1WVRuSXhWT0FKNHg2REp1Y1dzcHM1Z0RvbzQiLCJpZHAiOiJvMzY1IiwiaGQiOiJzbGIuY29tIiwiZGVzaWQiOiJkYXRhZmllckBldmQtZGRsLXVzLWNvbW1vbi5pYW0uZ3NlcnZpY2VhY2NvdW50LmNvbSIsImNvbnRhY3RfZW1haWwiOiJkYXRhZmllckBldmQtZGRsLXVzLWNvbW1vbi5pYW0uZ3NlcnZpY2VhY2NvdW50LmNvbSIsInJ0X2hhc2giOiJ5UzFwdjdrQ29pNkdWV3ZzdzhLcXlBIiwianRpIjoiYjM0OTE5ZDUtZGMyOC00ZGM1LTkwODgtNmRjODU4NWQ2ZWJlIn0.LwpKthf1TFkhMoREurRAzDwBlEN3Pe93hQusfLC8DEU";
    private static RedisServer centralRedisServer;
    private static RedisClient centralRedisClient;
    private static RedisServer partitionRedisServer;
    private static RedisClient partitionRedisClient;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private GcpAppProperties config;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private WhitelistSvcAccBeanConfiguration whitelistSvcAccBeanConfiguration;
    @MockBean
    private PartitionRedisInstanceService partitionRedisInstanceService;
    @MockBean
    private ITenantFactory tenantFactory;
    @MockBean
    private AuthorizationService authService;

    @BeforeClass
    public static void setupClass() throws IOException {
        centralRedisServer = new RedisServer(7000);
        centralRedisServer.start();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(7000).build();
        centralRedisClient = RedisClient.create(uri);

        partitionRedisServer = new RedisServer(6379);
        partitionRedisServer.start();
        uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        partitionRedisClient = RedisClient.create(uri);
    }

    @AfterClass
    public static void end() {
        centralRedisServer.stop();
        partitionRedisServer.stop();
    }

    @Before
    public void before() {
        when(config.getCentralRedisInstIp()).thenReturn("localhost");
        when(config.getCentralRedisInstPort()).thenReturn(7000);
        when(config.getPartitionEntityNodeDb()).thenReturn(1);
        when(config.getPartitionParentRefDb()).thenReturn(2);
        when(config.getPartitionChildrenRefDb()).thenReturn(3);
        when(config.getPartitionAssociationDb()).thenReturn(0);
        when(config.getlistGroupCacheDb()).thenReturn(1);
        when(config.getCachedItemsDb()).thenReturn(10);
        when(config.getPartitionAppIdDb()).thenReturn(4);
        when(config.getPartitionRedisInstanceId()).thenReturn("instanceId");
        when(config.getDomain()).thenReturn("contoso.com");
        when(config.getProjectId()).thenReturn("evd-ddl-us-services");
        when(config.isHttpAccepted()).thenReturn(true);
        List<String> list = new ArrayList<>();
        list.add("/provisioning/groups/datalake_user_groups.json");
        list.add("/provisioning/groups/datalake_service_groups.json");
        list.add("/provisioning/groups/data_groups.json");
        when(config.getInitialGroups()).thenReturn(list);
        when(partitionRedisInstanceService.getHostOfRedisInstanceForPartition("common")).thenReturn("localhost");
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setProjectId("evd-ddl-us-common");
        tenantInfo.setDataPartitionId("common");
        tenantInfo.setServiceAccount("datafier@evd-ddl-us-common.iam.gserviceaccount.com");
        when(tenantFactory.getTenantInfo("common")).thenReturn(tenantInfo);
        when(authService.isAuthorized(any(),any())).thenReturn(true);
    }

    @After
    public void cleanup() {
        StatefulRedisConnection<String, String> connection = centralRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.flushall();
        connection = partitionRedisClient.connect();
        commands = connection.sync();
        commands.flushall();
    }

    @Test
    public void shouldRunWorkflowSuccessfully() throws Exception {
        String rootUserGroup = "users@common.contoso.com";

        // Create users and users.data.root groups to pass phase 1 api's logic
        CreateGroupDto dto = new CreateGroupDto("users.data.root", "Users data root group");
        performCreateGroupRequest(dto, datafierJWT);
        dto = new CreateGroupDto("users", "Root users group");
        performCreateGroupRequest(dto, datafierJWT);
        performAddMemberRequest(new AddMemberDto(userA, Role.MEMBER), rootUserGroup, datafierJWT);
        performAddMemberRequest(new AddMemberDto(userB, Role.MEMBER), rootUserGroup, datafierJWT);
        performAddMemberRequest(new AddMemberDto(userC, Role.MEMBER), rootUserGroup, datafierJWT);

        //create users group
        dto = new CreateGroupDto("users.myusers.operators", "My users group");
        performCreateGroupRequest(dto, jwtA);

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListGroupRequest(jwtA));

        //create data groups
        dto = new CreateGroupDto("data.mydata1.operators", "My data group");
        performCreateGroupRequest(dto, jwtA);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA));

        // TODO: Uncomment when AppId filter is enabled again US https://dev.azure.com/slb-swt/data-at-rest/_workitems/edit/599488
        // updateGroupMetadata();

        //add data groups to users group
        dto = new CreateGroupDto("data.mydata2.operators", "My data group");
        performCreateGroupRequest(dto, jwtA);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
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
        dto = new CreateGroupDto("users.myusers2.operators", "My data group");
        performCreateGroupRequest(dto, jwtC);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC));

        //add it to users operators
        String userGroup2 = "users.myusers2.operators@common.contoso.com";
        performAddMemberRequest(new AddMemberDto(userGroup2, Role.MEMBER), userGroup1, jwtA);

        assertMembersEquals(new String[]{userA,
                userB,
                "users.data.root@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListMemberRequest(userGroup1, jwtA));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC));

        //remove data group A from Data group B
        performRemoveMemberRequest(dataGroup2, dataGroup1, jwtA);

        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, jwtA));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtB));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC));

        //delete data group B
        performDeleteGroupRequest(dataGroup2, jwtA);

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtB));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC));

        //delete user group 1
        performDeleteGroupRequest(userGroup1, jwtA);
        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com"}, performListMemberRequest(dataGroup1, jwtA));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA));
        assertGroupsEquals(new String[]{"users@common.contoso.com"}, performListGroupRequest(jwtB));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC));

        //add user b as OWNER of user group 2 and delete it
        performAddMemberRequest(new AddMemberDto(userB, Role.OWNER), userGroup2, jwtC);
        performDeleteGroupRequest(userGroup2, jwtB);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA));
        assertGroupsEquals(new String[]{"users@common.contoso.com"}, performListGroupRequest(jwtB));
        assertGroupsEquals(new String[]{"users@common.contoso.com"}, performListGroupRequest(jwtC));

        testGroupUpdateApi();
    }

    private void addNewUserToUsersOperators(String newUser, String newUserJwt, String userGroup) throws Exception {
        performAddMemberRequest(new AddMemberDto(newUser, Role.MEMBER), userGroup, jwtA);

        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com",
                userB}, performListMemberRequest(userGroup, jwtA));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(newUserJwt));
    }

    private void addUserGroupToDataGroups(String userGroup, String dataGroup1, String dataGroup2) throws Exception {
        performAddMemberRequest(new AddMemberDto(userGroup, Role.MEMBER), dataGroup1, jwtA);
        performAddMemberRequest(new AddMemberDto(userGroup, Role.MEMBER), dataGroup2, jwtA);

        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, jwtA));

        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup1, jwtA));
    }

    private void addDataGroup1ToDataGroup2(String dataGroup1, String dataGroup2) throws Exception {
        performAddMemberRequest(new AddMemberDto(dataGroup1, Role.MEMBER), dataGroup2, jwtA);
        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, jwtA));
        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com"}, performListMemberRequest(dataGroup1, jwtA));
    }

    private void updateGroupMetadata() throws Exception {
        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(Arrays.asList("App1", "App2"))),
                "data.mydata1.operators@common.contoso.com", jwtA);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListGroupRequest(jwtA));

        //update group metadata with audience
        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(Collections.singletonList("test.com"))),
                "data.mydata1.operators@common.contoso.com", jwtA);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA));

        //update group metadata with empty list
        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(Collections.emptyList())),
                "data.mydata1.operators@common.contoso.com", jwtA);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA));
    }

    private void testGroupUpdateApi() throws Exception {
        // there aren't new groups
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com"
        }, performListGroupRequest(jwtA));

        checkThatUsersGroupCannotBeRenamedByExistingName();

        // data group was created
        String dataGroupName = "data.testdata.operators";
        String dataGroupEmail = dataGroupName + "@common.contoso.com";
        performCreateGroupRequest(new CreateGroupDto(dataGroupName, "data group"), jwtA);
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail
        }, performListGroupRequest(jwtA));

        // users group 1 was created
        String usersGroup1Name = "users.testusers1.operators";
        String usersGroup1Email = usersGroup1Name + "@common.contoso.com";
        performCreateGroupRequest(
                new CreateGroupDto(usersGroup1Name, "users group 1 to be updated by a new email"), jwtA);
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email
        }, performListGroupRequest(jwtA));

        // users group 2 was created
        String usersGroup2Name = "users.testusers2.operators";
        String usersGroup2Email = usersGroup2Name + "@common.contoso.com";
        performCreateGroupRequest(new CreateGroupDto(usersGroup2Name, "users group 2"), jwtA);
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email,
                usersGroup2Email
        }, performListGroupRequest(jwtA));

        // users group 1 was added to data group
        performAddMemberRequest(new AddMemberDto(usersGroup1Email, Role.MEMBER), dataGroupEmail, jwtA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                usersGroup1Email
        }, performListMemberRequest(dataGroupEmail, jwtA));

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
                usersGroup1Email, datafierJWT);
        usersGroup1Email = newUsersGroup1Email;
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email, // updated email
                usersGroup2Email
        }, performListGroupRequest(jwtA));

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
                usersGroup2Email
        }, performListMemberRequest(usersGroup1Email, jwtA)); // updated email

        // clean-up
        performDeleteGroupRequest(dataGroupEmail, datafierJWT);
        performDeleteGroupRequest(usersGroup1Email, datafierJWT);
        performDeleteGroupRequest(usersGroup2Email, datafierJWT);
    }

    private void checkThatUsersGroupCannotBeRenamedByExistingName() throws Exception {
        // users group 1 was created
        String usersGroup1Name = "users.testusers1.operators";
        String usersGroup1Email = usersGroup1Name + "@common.contoso.com";
        performCreateGroupRequest(
                new CreateGroupDto(usersGroup1Name, "users group 1 to be updated by a new email"), jwtA);
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com",
                usersGroup1Email
        }, performListGroupRequest(jwtA));

        // users group 2 was created
        String usersGroup2Name = "users.testusers2.operators";
        String usersGroup2Email = usersGroup2Name + "@common.contoso.com";
        performCreateGroupRequest(new CreateGroupDto(usersGroup2Name, "users group 2"), jwtA);
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com",
                usersGroup1Email,
                usersGroup2Email
        }, performListGroupRequest(jwtA));

        // bad request when users group 1 email is updated by users group 2 name
        mockMvc.perform(patch("/groups/{group_email}", usersGroup1Email)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(Collections.singletonList(getRenameGroupOperation(usersGroup2Name))))
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + datafierJWT)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"code\":400,\"reason\":\"Bad Request\"," +
                        "\"message\":\"Invalid group name : \\\"users.testusers2.operators\\\", it already exists\"}"));

        // nothing changed
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com",
                usersGroup1Email,
                usersGroup2Email
        }, performListGroupRequest(jwtA));

        // clean-up
        performDeleteGroupRequest(usersGroup1Email, datafierJWT);
        performDeleteGroupRequest(usersGroup2Email, datafierJWT);
    }

    private void checkThatUsersGroupCannotBeRenamedToBootstrapGroup(final String existingGroupEmail) throws Exception {
        String nameOfBootstrappedGroup = "users.datalake.ops";

        // bad request when users group 1 email is updated by users group 2 name
        mockMvc.perform(patch("/groups/{group_email}", existingGroupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(Collections.singletonList(getRenameGroupOperation(nameOfBootstrappedGroup))))
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + datafierJWT)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"code\":400,\"reason\":\"Bad Request\"," +
                        "\"message\":\"Invalid group, group update API cannot work with bootstrapped groups\"}"));

        // nothing changed
        assertGroupsEquals(new String[]{"data.mydata1.operators@common.contoso.com",
                        "users.testusers1.operators@common.contoso.com",
                        "data.testdata.operators@common.contoso.com",
                        "users.testusers2.operators@common.contoso.com",
                        "users@common.contoso.com"},
                performListGroupRequest(jwtA));
    }

    private void checkBootstrappedGroupCannotBeRenamed(final String newGroupName) throws Exception {
        // users group 1 was created
        String bootstrappedGroupEmail = "users.datalake.ops@common.contoso.com";

        // bad request when users group 1 email is updated by users group 2 name
        mockMvc.perform(patch("/groups/{group_email}", bootstrappedGroupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(Collections.singletonList(getRenameGroupOperation(newGroupName))))
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + datafierJWT)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"code\":400,\"reason\":\"Bad Request\"," +
                        "\"message\":\"Invalid group, group update API cannot work with bootstrapped groups\"}"));

        // nothing changed
        assertGroupsEquals(new String[]{"data.mydata1.operators@common.contoso.com",
                        "users.testusers1.operators@common.contoso.com",
                        "data.testdata.operators@common.contoso.com",
                        "users.testusers2.operators@common.contoso.com",
                        "users@common.contoso.com"},
                performListGroupRequest(jwtA));
    }

    private void performCreateGroupRequest(CreateGroupDto dto, String jwt) throws Exception {
        mockMvc.perform(post("/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    private ListGroupResponseDto performListGroupRequest(String jwt) throws Exception {
        ResultActions result = mockMvc.perform(get("/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"));
        return objectMapper.readValue(
                result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                ListGroupResponseDto.class);
    }

    private void performAddMemberRequest(AddMemberDto dto, String groupEmail, String jwt) throws Exception {
        mockMvc.perform(post("/groups/{group_email}/members", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    private ListMemberResponseDto performListMemberRequest(String groupEmail, String jwt) throws Exception {
        ResultActions result = mockMvc.perform(get("/groups/{group_email}/members", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                .andExpect(status().isOk());

        return new Gson().fromJson(
                result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                ListMemberResponseDto.class);
    }

    private void performRemoveMemberRequest(String groupEmail, String memberEmail, String jwt) throws Exception {
        mockMvc.perform(delete("/groups/{group_email}/members/{member_email}", groupEmail, memberEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                .andExpect(status().isNoContent());
    }

    private void performDeleteGroupRequest(String groupEmail, String jwt) throws Exception {
        mockMvc.perform(delete("/groups/{group_email}", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                .andExpect(status().isNoContent());
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

    private void assertMembersEquals(final String[] expectedMemberEmails, final ListMemberResponseDto dto) {
        Set<String> actualMembers = new HashSet<>(Lists.transform(dto.getMembers(), MemberDto::getEmail));
        assertEquals(new HashSet<>(Arrays.asList(expectedMemberEmails)), actualMembers);
    }

    private void assertGroupsEquals(final String[] expectedGroupEmails, final ListGroupResponseDto dto) {
        Set<String> actualGroups = new HashSet<>(Lists.transform(new ArrayList<>(dto.getGroups()), ParentReference::getId));
        assertEquals(new HashSet<>(Arrays.asList(expectedGroupEmails)), actualGroups);
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
}
