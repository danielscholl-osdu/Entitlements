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
import org.opengroup.osdu.entitlements.v2.model.GroupType;
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
    private static final String jwtA = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMUBkZXNpZC5jb20iLCJpc3MiOiJwcmV2aWV3LmNvbSIsImF1ZCI6InRlc3QuY29tIiwiaWF0IjoxNTkwNjAwODI0LCJleHAiOjE2MDQ0Mjk4MzEsInByb3ZpZGVyIjoiYS5jb20iLCJjbGllbnQiOiJ0ZXN0LmNvbSIsInVzZXJpZCI6ImFAYi5jb20iLCJlbWFpbCI6InVzZXIxQGRlc2lkLmNvbSIsImF1dGh6IjoiIiwibGFzdG5hbWUiOiJCIiwiZmlyc3RuYW1lIjoiQSIsImNvdW50cnkiOiIiLCJjb21wYW55IjoiIiwiam9idGl0bGUiOiIiLCJzdWJpZCI6InU1TFNOSnVoVWZhSDB4UDN1WVRuSXhWT0FKNHg2REp1Y1dzcHM1Z0RvbzQiLCJpZHAiOiJvMzY1IiwiaGQiOiJzbGIuY29tIiwiZGVzaWQiOiJhQGRlc2lkLmNvbSIsImNvbnRhY3RfZW1haWwiOiJhQGIuY29tIiwicnRfaGFzaCI6InlTMXB2N2tDb2k2R1ZXdnN3OEtxeUEiLCJqdGkiOiIyNTc3YjQxNS1hMzAyLTQ1NjQtOWY0Mi0yZWQ1ZDJmN2Y5YzAifQ.2Y3pKm0cZLFCY8E4J4Mltd84JMDweFytIpvYmNzztw8";
    private static final String userB = "user2@desid.com";
    private static final String jwtB = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMkBkZXNpZC5jb20iLCJpc3MiOiJwcmV2aWV3LmNvbSIsImF1ZCI6InRlc3QuY29tIiwiaWF0IjoxNTkwNjAwODI0LCJleHAiOjE2MDQ0Mjk4ODYsInByb3ZpZGVyIjoiYS5jb20iLCJjbGllbnQiOiJ0ZXN0LmNvbSIsInVzZXJpZCI6ImJAYi5jb20iLCJlbWFpbCI6InVzZXIyQGRlc2lkLmNvbSIsImF1dGh6IjoiIiwibGFzdG5hbWUiOiJCIiwiZmlyc3RuYW1lIjoiQSIsImNvdW50cnkiOiIiLCJjb21wYW55IjoiIiwiam9idGl0bGUiOiIiLCJzdWJpZCI6InU1TFNOSnVoVWZhSDB4UDN1WVRuSXhWT0FKNHg2REp1Y1dzcHM1Z0RvbzQiLCJpZHAiOiJvMzY1IiwiaGQiOiJzbGIuY29tIiwiZGVzaWQiOiJ1c2VyMkBkZXNpZC5jb20iLCJjb250YWN0X2VtYWlsIjoiYUBiLmNvbSIsInJ0X2hhc2giOiJ5UzFwdjdrQ29pNkdWV3ZzdzhLcXlBIiwianRpIjoiMGFhM2NlZWYtMGRlNS00ODI5LWE0ZjItYWNkY2ZjMjMxNzZiIn0.uALyL2IHZ_kTK-d3kmQ5stjxcXifMUWHMp1drdPGJ2Q";
    private static final String userC = "user3@desid.com";
    private static final String jwtC = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJwcmV2aWV3LmNvbSIsImlhdCI6MTU5MjQyODA4OCwiZXhwIjoxNjA0NDI5OTM2LCJhdWQiOiJ0ZXN0LmNvbSIsInN1YiI6InVzZXIzQGRlc2lkLmNvbSIsImxhc3RuYW1lIjoiSm9obm55IiwiZmlyc3QiOiJSb2NrZXQiLCJlbWFpbCI6InVzZXIzQGRlc2lkLmNvbSIsImRlc2lkIjoidXNlcjNAZGVzaWQuY29tIiwianRpIjoiY2QyNTA0ZWQtYTQwNS00ZWJiLWIzZTUtNTQxMThjYWE3ZDA4In0.BUoe7Hb-DvIz1oGGLWv8Z-caTkVNOQMgeB3deAhTpc0";
    private static final String datafier = "a@b.com";
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
        when(config.getInitialGroups()).thenCallRealMethod();
        when(config.getGroupsOfServicePrincipal()).thenCallRealMethod();
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
        performCreateGroupRequest(dto, datafierJWT, datafier);
        dto = new CreateGroupDto("users", "Root users group");
        performCreateGroupRequest(dto, datafierJWT, datafier);
        performAddMemberRequest(new AddMemberDto(userA, Role.MEMBER), rootUserGroup, datafierJWT, datafier);
        performAddMemberRequest(new AddMemberDto(userB, Role.MEMBER), rootUserGroup, datafierJWT, datafier);
        performAddMemberRequest(new AddMemberDto(userC, Role.MEMBER), rootUserGroup, datafierJWT, datafier);

        //create users group
        dto = new CreateGroupDto("users.myusers.operators", "My users group");
        performCreateGroupRequest(dto, jwtA, userA);

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListGroupRequest(jwtA, userA));

        //create data groups
        dto = new CreateGroupDto("data.mydata1.operators", "My data group");
        performCreateGroupRequest(dto, jwtA, userA);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA, userA));

        updateGroupMetadata();

        //add data groups to users group
        dto = new CreateGroupDto("data.mydata2.operators", "My data group");
        performCreateGroupRequest(dto, jwtA, userA);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA, userA));

        String dataGroup1 = "data.mydata1.operators@common.contoso.com";
        String dataGroup2 = "data.mydata2.operators@common.contoso.com";
        String userGroup1 = "users.myusers.operators@common.contoso.com";

        addDataGroup1ToDataGroup2(dataGroup1, dataGroup2);

        addUserGroupToDataGroups(userGroup1, dataGroup1, dataGroup2);

        addNewUserToUsersOperators(userB, jwtB, userGroup1);

        //create new root hierarchy
        dto = new CreateGroupDto("users.myusers2.operators", "My data group");
        performCreateGroupRequest(dto, jwtC, userC);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC, userC));

        //add it to users operators
        String userGroup2 = "users.myusers2.operators@common.contoso.com";
        performAddMemberRequest(new AddMemberDto(userGroup2, Role.MEMBER), userGroup1, jwtA, userA);

        assertMembersEquals(new String[]{userA,
                userB,
                "users.data.root@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListMemberRequest(userGroup1, jwtA, userA));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC, userC));

        //list groups by memberEmail
        listGroupsByMemberEmail();

        //remove data group A from Data group B
        performRemoveMemberRequest(dataGroup2, dataGroup1, jwtA, userA);

        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, jwtA, userA));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtB, userB));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC, userC));

        //delete data group B
        performDeleteGroupRequest(dataGroup2, jwtA, userA);

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA, userA));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtB, userB));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC, userC));

        //delete user group 1
        performDeleteGroupRequest(userGroup1, jwtA, userA);
        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com"}, performListMemberRequest(dataGroup1, jwtA, userA));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA, userA));
        assertGroupsEquals(new String[]{"users@common.contoso.com"}, performListGroupRequest(jwtB, userB));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC, userC));

        //add user b as OWNER of user group 2 and delete it
        performAddMemberRequest(new AddMemberDto(userB, Role.OWNER), userGroup2, jwtC, userC);
        performDeleteGroupRequest(userGroup2, jwtB, userB);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA, userA));
        assertGroupsEquals(new String[]{"users@common.contoso.com"}, performListGroupRequest(jwtB, userB));
        assertGroupsEquals(new String[]{"users@common.contoso.com"}, performListGroupRequest(jwtC, userC));

        testGroupUpdateApi();
    }

    private void addNewUserToUsersOperators(String newUser, String newUserJwt, String userGroup) throws Exception {
        performAddMemberRequest(new AddMemberDto(newUser, Role.MEMBER), userGroup, jwtA, userA);

        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com",
                userB}, performListMemberRequest(userGroup, jwtA, userA));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(newUserJwt, newUser));
    }

    private void addUserGroupToDataGroups(String userGroup, String dataGroup1, String dataGroup2) throws Exception {
        performAddMemberRequest(new AddMemberDto(userGroup, Role.MEMBER), dataGroup1, jwtA, userA);
        performAddMemberRequest(new AddMemberDto(userGroup, Role.MEMBER), dataGroup2, jwtA, userA);

        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, jwtA, userA));

        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup1, jwtA, userA));
    }

    private void addDataGroup1ToDataGroup2(String dataGroup1, String dataGroup2) throws Exception {
        performAddMemberRequest(new AddMemberDto(dataGroup1, Role.MEMBER), dataGroup2, jwtA, userA);
        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, jwtA, userA));
        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com"}, performListMemberRequest(dataGroup1, jwtA, userA));
    }

    private void listGroupsByMemberEmail() throws Exception {
        // when argument passed is NONE
        assertGroupsEquals(new String[]{"users@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupsOnBehalfOfRequest(userA, String.valueOf(GroupType.NONE)));

        // when argument passed is DATA
        assertGroupsEquals(new String[]{"data.mydata2.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupsOnBehalfOfRequest(userA, String.valueOf(GroupType.DATA)));

        //when argument is USER
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListGroupsOnBehalfOfRequest(userA, String.valueOf(GroupType.USER)));

    }

    private void updateGroupMetadata() throws Exception {
        List<String> appIds = Arrays.asList("App1", "App2");
        List<String> audience = Collections.singletonList("test.com");
        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(appIds)),
                "data.mydata1.operators@common.contoso.com", jwtA, userA, appIds);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListGroupRequestWithAppId(jwtA, userA, audience));

        //update group metadata with audience
        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(audience)),
                "data.mydata1.operators@common.contoso.com", jwtA, userA, audience);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequestWithAppId(jwtA, userA, audience));

        //update group metadata with empty list
        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(Collections.emptyList())),
                "data.mydata1.operators@common.contoso.com", jwtA, userA, audience);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequestWithAppId(jwtA, userA, audience));
    }

    private void testGroupUpdateApi() throws Exception {
        // there aren't new groups
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com"
        }, performListGroupRequest(jwtA, userA));

        checkThatUsersGroupCannotBeRenamedByExistingName();

        // data group was created
        String dataGroupName = "data.testdata.operators";
        String dataGroupEmail = dataGroupName + "@common.contoso.com";
        performCreateGroupRequest(new CreateGroupDto(dataGroupName, "data group"), jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail
        }, performListGroupRequest(jwtA, userA));

        // users group 1 was created
        String usersGroup1Name = "users.testusers1.operators";
        String usersGroup1Email = usersGroup1Name + "@common.contoso.com";
        performCreateGroupRequest(
                new CreateGroupDto(usersGroup1Name, "users group 1 to be updated by a new email"), jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email
        }, performListGroupRequest(jwtA, userA));

        // users group 2 was created
        String usersGroup2Name = "users.testusers2.operators";
        String usersGroup2Email = usersGroup2Name + "@common.contoso.com";
        performCreateGroupRequest(new CreateGroupDto(usersGroup2Name, "users group 2"), jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email,
                usersGroup2Email
        }, performListGroupRequest(jwtA, userA));

        // users group 1 was added to data group
        performAddMemberRequest(new AddMemberDto(usersGroup1Email, Role.MEMBER), dataGroupEmail, jwtA, userA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                usersGroup1Email
        }, performListMemberRequest(dataGroupEmail, jwtA, userA));

        // users group 2 was added to users group 1
        performAddMemberRequest(new AddMemberDto(usersGroup2Email, Role.MEMBER), usersGroup1Email, jwtA, userA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                usersGroup2Email
        }, performListMemberRequest(usersGroup1Email, jwtA, userA));

        // users group 1 email was updated
        String newUsersGroup1Name = "users.testusers3.operators";
        String newUsersGroup1Email = newUsersGroup1Name + "@common.contoso.com";

        checkThatUsersGroupCannotBeRenamedToBootstrapGroup(usersGroup1Email);
        checkBootstrappedGroupCannotBeRenamed(newUsersGroup1Name);

        performUpdateGroupRequest(Collections.singletonList(getRenameGroupOperation(newUsersGroup1Name)),
                usersGroup1Email, datafierJWT, datafier, Collections.singletonList("test.com"));
        usersGroup1Email = newUsersGroup1Email;
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email, // updated email
                usersGroup2Email
        }, performListGroupRequestWithAppId(jwtA, userA, Collections.singletonList("test.com")));

        // data group has a member with updated email
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                usersGroup1Email // updated email
        }, performListMemberRequest(dataGroupEmail, jwtA, userA));

        // users group 2 still is a member of users group 1 even after the updated email
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                usersGroup2Email
        }, performListMemberRequest(usersGroup1Email, jwtA, userA)); // updated email

        // clean-up
        performDeleteGroupRequest(dataGroupEmail, datafierJWT, datafier);
        performDeleteGroupRequest(usersGroup1Email, datafierJWT, datafier);
        performDeleteGroupRequest(usersGroup2Email, datafierJWT, datafier);
    }

    private void checkThatUsersGroupCannotBeRenamedByExistingName() throws Exception {
        // users group 1 was created
        String usersGroup1Name = "users.testusers1.operators";
        String usersGroup1Email = usersGroup1Name + "@common.contoso.com";
        performCreateGroupRequest(
                new CreateGroupDto(usersGroup1Name, "users group 1 to be updated by a new email"), jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com",
                usersGroup1Email
        }, performListGroupRequest(jwtA, userA));

        // users group 2 was created
        String usersGroup2Name = "users.testusers2.operators";
        String usersGroup2Email = usersGroup2Name + "@common.contoso.com";
        performCreateGroupRequest(new CreateGroupDto(usersGroup2Name, "users group 2"), jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.mydata1.operators@common.contoso.com",
                "users@common.contoso.com",
                usersGroup1Email,
                usersGroup2Email
        }, performListGroupRequest(jwtA, userA));

        // bad request when users group 1 email is updated by users group 2 name
        mockMvc.perform(patch("/groups/{group_email}", usersGroup1Email)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(Collections.singletonList(getRenameGroupOperation(usersGroup2Name))))
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + datafierJWT)
                .header(DpsHeaders.USER_ID, datafier)
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
        }, performListGroupRequest(jwtA, userA));

        // clean-up
        performDeleteGroupRequest(usersGroup1Email, datafierJWT, datafier);
        performDeleteGroupRequest(usersGroup2Email, datafierJWT, datafier);
    }

    private void checkThatUsersGroupCannotBeRenamedToBootstrapGroup(final String existingGroupEmail) throws Exception {
        String nameOfBootstrappedGroup = "users.datalake.ops";

        // bad request when users group 1 email is updated by users group 2 name
        mockMvc.perform(patch("/groups/{group_email}", existingGroupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(Collections.singletonList(getRenameGroupOperation(nameOfBootstrappedGroup))))
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + datafierJWT)
                .header(DpsHeaders.USER_ID, datafier)
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
                performListGroupRequest(jwtA, userA));
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
                .header(DpsHeaders.USER_ID, datafier)
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
                performListGroupRequest(jwtA, userA));
    }

    private ListGroupResponseDto performListGroupsOnBehalfOfRequest(String memberEmail, String groupType) throws Exception {
        ResultActions result = mockMvc.perform(get("/members/{member_email}/groups", memberEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + datafierJWT)
                .header(DpsHeaders.USER_ID, datafier)
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .queryParam("type", groupType));
        return objectMapper.readValue(
                result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                ListGroupResponseDto.class);
    }

    private void performCreateGroupRequest(CreateGroupDto dto, String jwt, String userId) throws Exception {
        mockMvc.perform(post("/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .header(DpsHeaders.USER_ID, userId)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    private ListGroupResponseDto performListGroupRequest(String jwt, String userId) throws Exception {
        ResultActions result = mockMvc.perform(get("/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.USER_ID, userId)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"));
        return objectMapper.readValue(
                result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                ListGroupResponseDto.class);
    }

    private void performAddMemberRequest(AddMemberDto dto, String groupEmail, String jwt, String userId) throws Exception {
        mockMvc.perform(post("/groups/{group_email}/members", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .header(DpsHeaders.USER_ID, userId)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    private ListMemberResponseDto performListMemberRequest(String groupEmail, String jwt, String userId) throws Exception {
        ResultActions result = mockMvc.perform(get("/groups/{group_email}/members", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.USER_ID, userId)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                .andExpect(status().isOk());

        return new Gson().fromJson(
                result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                ListMemberResponseDto.class);
    }

    private void performRemoveMemberRequest(String groupEmail, String memberEmail, String jwt, String userId) throws Exception {
        mockMvc.perform(delete("/groups/{group_email}/members/{member_email}", groupEmail, memberEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.USER_ID, userId)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                .andExpect(status().isNoContent());
    }

    private void performDeleteGroupRequest(String groupEmail, String jwt, String userId) throws Exception {
        mockMvc.perform(delete("/groups/{group_email}", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.USER_ID, userId)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                .andExpect(status().isNoContent());
    }

    private void performUpdateGroupRequest(List<UpdateGroupOperation> operations, String groupEmail, String jwt, String userId, List<String> appIds) throws Exception {
        mockMvc.perform(patch("/groups/{group_email}", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .header(DpsHeaders.USER_ID, userId)
                .header(DpsHeaders.APP_ID, appIds)
                .content(objectMapper.writeValueAsString(operations)))
                .andExpect(status().isOk());
    }

    private ListGroupResponseDto performListGroupRequestWithAppId(String jwt, String userId, List<String> appIds) throws Exception {
        ResultActions result = mockMvc.perform(get("/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.USER_ID, userId)
                .header(DpsHeaders.APP_ID, appIds)
                .header(DpsHeaders.DATA_PARTITION_ID, "common"));
        return objectMapper.readValue(
                result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                ListGroupResponseDto.class);
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
