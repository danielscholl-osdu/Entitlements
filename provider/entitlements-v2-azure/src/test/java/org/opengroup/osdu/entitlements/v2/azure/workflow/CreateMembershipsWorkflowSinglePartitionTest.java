package org.opengroup.osdu.entitlements.v2.azure.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import io.github.resilience4j.retry.Retry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.cache.ICache;
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
import org.opengroup.osdu.entitlements.v2.model.GroupType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberResponseDto;
import org.opengroup.osdu.entitlements.v2.model.listmember.MemberDto;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupOperation;
import org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses.HitsNMissesMetricService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private static final String SERVICE_P_JWT = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzZXJ2aWNlX3ByaW5jaXBhbC5jb20i" +
            "LCJpc3MiOiJwcmV2aWV3LmNvbSIsImF1ZCI6InRlc3QuY29tIiwiaWF0IjoxNTkwNjAwODI0LCJleHAiOjE1OTM3MTM4MjQsInByb3ZpZGVy" +
            "IjoiYS5jb20iLCJjbGllbnQiOiJ0ZXN0LmNvbSIsInVzZXJpZCI6ImRhdGFmaWVyQGF6dXJlLmNvbSIsImVtYWlsIjoic2VydmljZV9wcmlu" +
            "Y2lwYWwuY29tIiwiYXV0aHoiOiIiLCJsYXN0bmFtZSI6IkIiLCJmaXJzdG5hbWUiOiJBIiwiY291bnRyeSI6IiIsImNvbXBhbnkiOiIiLCJq" +
            "b2J0aXRsZSI6IiIsInN1YmlkIjoidTVMU05KdWhVZmFIMHhQM3VZVG5JeFZPQUo0eDZESnVjV3NwczVnRG9vNCIsImlkcCI6Im8zNjUiLCJo" +
            "ZCI6InNsYi5jb20iLCJkZXNpZCI6Inh4eC5jb20iLCJjb250YWN0X2VtYWlsIjoieHh4LmNvbSIsInJ0X2hhc2giOiJ5UzFwdjdrQ29pNkdW" +
            "V3ZzdzhLcXlBIiwianRpIjoiYjM0OTE5ZDUtZGMyOC00ZGM1LTkwODgtNmRjODU4NWQ2ZWJlIn0.rwjGCPfqVFM3MMUlbfTm7YS9nMOKBnv6" +
            "vKRugfmojWw";
    private static final String jwtA = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMUBkZXNpZC5jb20iLCJpc3MiOiJw" +
            "cmV2aWV3LmNvbSIsImF1ZCI6InRlc3QuY29tIiwiaWF0IjoxNTkwNjAwODI0LCJleHAiOjE2MDQ0Mjk4MzEsInByb3ZpZGVyIjoiYS5jb20i" +
            "LCJjbGllbnQiOiJ0ZXN0LmNvbSIsInVzZXJpZCI6ImFAYi5jb20iLCJlbWFpbCI6InVzZXIxQGRlc2lkLmNvbSIsImF1dGh6IjoiIiwibGFz" +
            "dG5hbWUiOiJCIiwiZmlyc3RuYW1lIjoiQSIsImNvdW50cnkiOiIiLCJjb21wYW55IjoiIiwiam9idGl0bGUiOiIiLCJzdWJpZCI6InU1TFNO" +
            "SnVoVWZhSDB4UDN1WVRuSXhWT0FKNHg2REp1Y1dzcHM1Z0RvbzQiLCJpZHAiOiJvMzY1IiwiaGQiOiJzbGIuY29tIiwiZGVzaWQiOiJ5eXku" +
            "Y29tIiwiY29udGFjdF9lbWFpbCI6ImFAYi5jb20iLCJydF9oYXNoIjoieVMxcHY3a0NvaTZHVld2c3c4S3F5QSIsImp0aSI6IjI1NzdiNDE1" +
            "LWEzMDItNDU2NC05ZjQyLTJlZDVkMmY3ZjljMCJ9.onrgBSuPWKfcvXViuSknMHsqArJMkm2ZA2S-cYEEHM8";
    private static final String jwtB = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMkBkZXNpZC5jb20iLCJpc3MiOiJw" +
            "cmV2aWV3LmNvbSIsImF1ZCI6InRlc3QuY29tIiwiaWF0IjoxNTkwNjAwODI0LCJleHAiOjE2MDQ0Mjk4ODYsInByb3ZpZGVyIjoiYS5jb20i" +
            "LCJjbGllbnQiOiJ0ZXN0LmNvbSIsInVzZXJpZCI6ImJAYi5jb20iLCJlbWFpbCI6InVzZXIyQGRlc2lkLmNvbSIsImF1dGh6IjoiIiwibGFz" +
            "dG5hbWUiOiJCIiwiZmlyc3RuYW1lIjoiQSIsImNvdW50cnkiOiIiLCJjb21wYW55IjoiIiwiam9idGl0bGUiOiIiLCJzdWJpZCI6InU1TFNO" +
            "SnVoVWZhSDB4UDN1WVRuSXhWT0FKNHg2REp1Y1dzcHM1Z0RvbzQiLCJpZHAiOiJvMzY1IiwiaGQiOiJzbGIuY29tIiwiZGVzaWQiOiJ1c2Vy" +
            "MkBkZXNpZC5jb20iLCJjb250YWN0X2VtYWlsIjoiYUBiLmNvbSIsInJ0X2hhc2giOiJ5UzFwdjdrQ29pNkdWV3ZzdzhLcXlBIiwianRpIjoi" +
            "MGFhM2NlZWYtMGRlNS00ODI5LWE0ZjItYWNkY2ZjMjMxNzZiIn0.uALyL2IHZ_kTK-d3kmQ5stjxcXifMUWHMp1drdPGJ2Q";
    private static final String jwtC = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJwcmV2aWV3LmNvbSIsImlhdCI6MTU5MjQy" +
            "ODA4OCwiZXhwIjoxNjA0NDI5OTM2LCJhdWQiOiJ0ZXN0LmNvbSIsInN1YiI6InVzZXIzQGRlc2lkLmNvbSIsImxhc3RuYW1lIjoiSm9obm55" +
            "IiwiZmlyc3QiOiJSb2NrZXQiLCJlbWFpbCI6InVzZXIzQGRlc2lkLmNvbSIsImRlc2lkIjoienp6LmNvbSIsImp0aSI6ImNkMjUwNGVkLWE0" +
            "MDUtNGViYi1iM2U1LTU0MTE4Y2FhN2QwOCJ9.ZEp_kvx95yIYbK_RkK4f7oNJyCzlu4C3R_siaq2U_kc";
    public static final String JWT_FOR_MEMBER_DELETION = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSl" +
            "dUIEJ1aWxkZXIiLCJpYXQiOjE1OTk2NzE0NTEsImV4cCI6MTYzMTIwNzQ1MSwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoidX" +
            "Nlci1mb3ItZGVsZXRpb25AZGVzaWQuY29tIiwiZW1haWwiOiJ1c2VyLWZvci1kZWxldGlvbkBkZXNpZC5jb20iLCJkZXNpZCI6InVzZX" +
            "ItZm9yLWRlbGV0aW9uQGRlc2lkLmNvbSJ9.JkDGIlylJUDwaZcAiYGd4VDpFZOgrabYB31DrxjLCpw";

    private static final String servicePrincipal = "service_principal.com";
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
    @MockBean
    private ICache<String, ParentReferences> redisGroupCache;
    @MockBean
    private RedissonClient redissonClient;
    @Mock
    private RLock cacheLock;
    @MockBean
    private Retry retry;
    @MockBean
    private HitsNMissesMetricService metricService;

    @Before
    public void before() throws InterruptedException {
        Mockito.when(config.getDomain()).thenReturn("contoso.com");
        Mockito.when(config.getProjectId()).thenReturn("evd-ddl-us-services");
        Mockito.when(config.getInitialGroups()).thenCallRealMethod();
        Mockito.when(config.getGroupsOfServicePrincipal()).thenCallRealMethod();
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setProjectId("evd-ddl-us-common");
        tenantInfo.setDataPartitionId("common");
        tenantInfo.setServiceAccount("service_principal.com");
        Mockito.when(tenantFactory.getTenantInfo("common")).thenReturn(tenantInfo);
        when(authService.isAuthorized(any(), any())).thenReturn(true);
        when(redissonClient.getLock(any())).thenReturn(cacheLock);
        when(cacheLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
    }

    @Test
    public void shouldRunWorkflowSuccessfully() throws Exception {
        performBootstrappingOfGroupsAndUsers();

        //running second time to prove the the api is idempotent
        performBootstrappingOfGroupsAndUsers();

        String rootUserGroup = "users@common.contoso.com";
        performAddMemberRequest(new AddMemberDto(userA, Role.MEMBER), rootUserGroup, SERVICE_P_JWT, servicePrincipal);
        performAddMemberRequest(new AddMemberDto(userB, Role.MEMBER), rootUserGroup, SERVICE_P_JWT, servicePrincipal);
        performAddMemberRequest(new AddMemberDto(userC, Role.MEMBER), rootUserGroup, SERVICE_P_JWT, servicePrincipal);

        //create users group
        performCreateGroupRequest("users.myusers.operators", jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListGroupRequest(jwtA, userA));

        //create data group
        performCreateGroupRequest("data.mydata1.operators", jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA, userA));

        updateGroupMetadata();

        //add data groups to users group
        performCreateGroupRequest("data.mydata2.operators", jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
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
        performCreateGroupRequest("users.myusers2.operators", jwtC, userC);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC, userC));

        //add it to users operators
        String userGroup2 = "users.myusers2.operators@common.contoso.com";
        performAddMemberRequest(new AddMemberDto(userGroup2, Role.MEMBER), userGroup1, jwtA, userA);

        assertMembersEquals(new String[]{userA,
                userB,
                "users.data.root@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListMemberRequest(userGroup1, jwtA, userA));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC, userC));

        //list groups by memberEmail
        listGroupsByMemberEmail();

        removeDataGroup1FromDataGroup2(dataGroup1, dataGroup2);
        deleteDataGroup2(dataGroup2);
        deleteUserGroup1(userGroup1, dataGroup1);

        performAddMemberRequest(new AddMemberDto(userB, Role.OWNER), userGroup2, jwtC, userC);
        performDeleteGroupRequest(userGroup2, jwtB, userB);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA, userA));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com"}, performListGroupRequest(jwtB, userB));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com"}, performListGroupRequest(jwtC, userC));

        testGroupUpdateApi();

        testDeleteMemberApi();
    }

    private void removeDataGroup1FromDataGroup2(String dataGroup1, String dataGroup2) {
        performRemoveMemberRequest(dataGroup2, dataGroup1, jwtA, userA);
        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, jwtA, userA));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtB, userB));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC, userC));
    }

    private void deleteDataGroup2(String dataGroup2) {
        performDeleteGroupRequest(dataGroup2, jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA, userA));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtB, userB));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC, userC));
    }

    private void deleteUserGroup1(String userGroup1, String dataGroup1) {
        performDeleteGroupRequest(userGroup1, jwtA, userA);
        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com"}, performListMemberRequest(dataGroup1, jwtA, userA));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(jwtA, userA));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com"}, performListGroupRequest(jwtB, userB));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(jwtC, userC));
    }

    private void addDataGroup1ToDataGroup2(String dataGroup1, String dataGroup2) {
        performAddMemberRequest(new AddMemberDto(dataGroup1, Role.MEMBER), dataGroup2, jwtA, userA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, jwtA, userA));
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com"}, performListMemberRequest(dataGroup1, jwtA, userA));
    }

    private void addUserGroupToDataGroups(String userGroup, String dataGroup1, String dataGroup2) {
        performAddMemberRequest(new AddMemberDto(userGroup, Role.MEMBER), dataGroup1, jwtA, userA);
        performAddMemberRequest(new AddMemberDto(userGroup, Role.MEMBER), dataGroup2, jwtA, userA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, jwtA, userA));

        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup1, jwtA, userA));
    }

    private void addNewUserToUsersOperators(String newUser, String newUserJwt, String userGroup) {
        performAddMemberRequest(new AddMemberDto(newUser, Role.MEMBER), userGroup, jwtA, userA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                userB}, performListMemberRequest(userGroup, jwtA, userA));

        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(newUserJwt, newUser));
    }

    private void updateGroupMetadata() throws Exception {
        List<String> appIds = Arrays.asList("App1", "App2");
        List<String> audience = Collections.singletonList("test.com");
        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(appIds)),
                "data.mydata1.operators@common.contoso.com", jwtA, userA, appIds);

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListGroupRequestWithAppId(jwtA, userA, audience));

        //update group metadata with audience

        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(audience)),
                "data.mydata1.operators@common.contoso.com", jwtA, userA, audience);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequestWithAppId(jwtA, userA, audience));

        //update group metadata with empty list
        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(Collections.emptyList())),
                "data.mydata1.operators@common.contoso.com", jwtA, userA, audience);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequestWithAppId(jwtA, userA, audience));
    }

    private void testGroupUpdateApi() throws Exception {
        // there aren't new groups
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com"
        }, performListGroupRequest(jwtA, userA));

        checkThatUsersGroupCannotBeRenamedByExistingName();

        // data group was created
        String dataGroupName = "data.testdata.operators";
        String dataGroupEmail = dataGroupName + "@common.contoso.com";
        performCreateGroupRequest(dataGroupName, jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail
        }, performListGroupRequest(jwtA, userA));

        // users group 1 was created to be updated by a new email
        String usersGroup1Name = "users.testusers1.operators";
        String usersGroup1Email = usersGroup1Name + "@common.contoso.com";
        performCreateGroupRequest(usersGroup1Name, jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email}, performListGroupRequest(jwtA, userA));

        // users group 2 was created
        String usersGroup2Name = "users.testusers2.operators";
        String usersGroup2Email = usersGroup2Name + "@common.contoso.com";
        performCreateGroupRequest(usersGroup2Name, jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email,
                usersGroup2Email}, performListGroupRequest(jwtA, userA));

        // users group 1 was added to data group
        performAddMemberRequest(new AddMemberDto(usersGroup1Email, Role.MEMBER), dataGroupEmail, jwtA, userA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                usersGroup1Email}, performListMemberRequest(dataGroupEmail, jwtA, userA));

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
                usersGroup1Email, SERVICE_P_JWT, servicePrincipal, Collections.singletonList("test.com"));
        usersGroup1Email = newUsersGroup1Email;
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email, // updated email
                usersGroup2Email}, performListGroupRequestWithAppId(jwtA, userA, Collections.singletonList("test.com")));

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
                usersGroup2Email}, performListMemberRequest(usersGroup1Email, jwtA, userA)); // updated email

        // clean-up
        performDeleteGroupRequest(dataGroupEmail, SERVICE_P_JWT, servicePrincipal);
        performDeleteGroupRequest(usersGroup1Email, SERVICE_P_JWT, servicePrincipal);
        performDeleteGroupRequest(usersGroup2Email, SERVICE_P_JWT, servicePrincipal);
    }

    private void checkThatUsersGroupCannotBeRenamedByExistingName() throws Exception {
        // users group 1 was created
        String usersGroup1Name = "users.testusers1.operators";
        String usersGroup1Email = usersGroup1Name + "@common.contoso.com";

        //"users group 1 to be updated by a new email"
        performCreateGroupRequest(usersGroup1Name, jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                usersGroup1Email}, performListGroupRequest(jwtA, userA));

        // users group 2 was created
        String usersGroup2Name = "users.testusers2.operators";
        String usersGroup2Email = usersGroup2Name + "@common.contoso.com";
        performCreateGroupRequest(usersGroup2Name, jwtA, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                usersGroup1Email,
                usersGroup2Email}, performListGroupRequest(jwtA, userA));

        // bad request when users group 1 email is updated by users group 2 name
        mockMvc.perform(patch("/groups/{group_email}", usersGroup1Email)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(Collections.singletonList(getRenameGroupOperation(usersGroup2Name))))
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + SERVICE_P_JWT)
                .header(DpsHeaders.USER_ID, servicePrincipal)
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
                usersGroup2Email}, performListGroupRequest(jwtA, userA));

        // clean-up
        performDeleteGroupRequest(usersGroup1Email, SERVICE_P_JWT, servicePrincipal);
        performDeleteGroupRequest(usersGroup2Email, SERVICE_P_JWT, servicePrincipal);
    }

    private void checkThatUsersGroupCannotBeRenamedToBootstrapGroup(final String existingGroupEmail) throws Exception {
        String nameOfBootstrappedGroup = "users.datalake.ops";

        // bad request when users group 1 email is updated by users group 2 name
        mockMvc.perform(patch("/groups/{group_email}", existingGroupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(Collections.singletonList(getRenameGroupOperation(nameOfBootstrappedGroup))))
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + SERVICE_P_JWT)
                .header(DpsHeaders.USER_ID, servicePrincipal)
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
                "users@common.contoso.com"}, performListGroupRequest(jwtA, userA));
    }

    private void listGroupsByMemberEmail() throws Exception {
        // when argument passed is NONE
        assertGroupsEquals(new String[]{"data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupsOnBehalfOfRequest(userA, String.valueOf(GroupType.NONE)));

        // when argument passed is DATA
        assertGroupsEquals(new String[]{"data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupsOnBehalfOfRequest(userA, String.valueOf(GroupType.DATA)));

        //when argument is USER
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListGroupsOnBehalfOfRequest(userA, String.valueOf(GroupType.USER)));

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
                .header(DpsHeaders.USER_ID, servicePrincipal)
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
                "users@common.contoso.com"}, performListGroupRequest(jwtA, userA));
    }

    private void testDeleteMemberApi() throws Exception {
        String rootUserGroup = "users@common.contoso.com";

        String permissionGroupName = "users.test.editors";
        String permissionGroup = permissionGroupName + "@common.contoso.com";
        performCreateGroupRequest(permissionGroupName, SERVICE_P_JWT, servicePrincipal);

        assertTrue(performListGroupRequest(JWT_FOR_MEMBER_DELETION, USER_FOR_DELETION).getGroups().isEmpty());
        performAddMemberRequest(new AddMemberDto(USER_FOR_DELETION, Role.MEMBER), rootUserGroup, SERVICE_P_JWT, servicePrincipal);
        performAddMemberRequest(new AddMemberDto(USER_FOR_DELETION, Role.MEMBER), permissionGroup, SERVICE_P_JWT, servicePrincipal);

        assertGroupsEquals(new String[]{
                rootUserGroup,
                permissionGroup,
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com"
        }, performListGroupRequest(JWT_FOR_MEMBER_DELETION, USER_FOR_DELETION));
        performDeleteMemberRequest(USER_FOR_DELETION, SERVICE_P_JWT, servicePrincipal);
        assertTrue(performListGroupRequest(JWT_FOR_MEMBER_DELETION, USER_FOR_DELETION).getGroups().isEmpty());

        performDeleteGroupRequest(permissionGroup, SERVICE_P_JWT, servicePrincipal);
    }

    private void performCreateGroupRequest(String groupName, String jwt, String userId) {
        try {
            CreateGroupDto dto = new CreateGroupDto(groupName, groupName + ": description");
            mockMvc.perform(MockMvcRequestBuilders.post("/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header(DpsHeaders.USER_ID, userId)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common")
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(MockMvcResultMatchers.status().isCreated());
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
    }

    private void performDeleteGroupRequest(String groupEmail, String jwt, String userId) {
        try {
            mockMvc.perform(MockMvcRequestBuilders.delete("/groups/{group_email}", groupEmail)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header(DpsHeaders.USER_ID, userId)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                    .andExpect(MockMvcResultMatchers.status().isNoContent());
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
    }

    private void performAddMemberRequest(AddMemberDto dto, String groupEmail, String jwt, String userId) {
        try {
            mockMvc.perform(MockMvcRequestBuilders.post("/groups/{group_email}/members", groupEmail)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header(DpsHeaders.USER_ID, userId)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common")
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
    }

    private ListGroupResponseDto performListGroupRequest(String jwt, String userId) {
        try {
            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header(DpsHeaders.USER_ID, userId)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common"));
            return objectMapper.readValue(
                    result.andExpect(MockMvcResultMatchers.status().isOk()).andReturn().getResponse().getContentAsString(),
                    ListGroupResponseDto.class);
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
        return ListGroupResponseDto.builder().build();
    }

    private ListGroupResponseDto performListGroupsOnBehalfOfRequest(String memberId, String groupType) {
        try {
            ResultActions result = mockMvc.perform(get("/members/{member_email}/groups", memberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer " + SERVICE_P_JWT)
                    .header(DpsHeaders.USER_ID, servicePrincipal)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common")
                    .queryParam("type", groupType));
            return objectMapper.readValue(
                    result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                    ListGroupResponseDto.class);
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
        return ListGroupResponseDto.builder().build();
    }

    private ListMemberResponseDto performListMemberRequest(String groupEmail, String jwt, String userId) {
        try {
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
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
        return ListMemberResponseDto.builder().build();
    }

    private void performRemoveMemberRequest(String groupEmail, String memberEmail, String jwt, String userId) {
        try {
            mockMvc.perform(delete("/groups/{group_email}/members/{member_email}", groupEmail, memberEmail)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header(DpsHeaders.USER_ID, userId)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                    .andExpect(status().isNoContent());
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
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

    private ListGroupResponseDto performListGroupRequestWithAppId(String jwt, String userId, List<String> appIds) {
        try {
            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header(DpsHeaders.USER_ID, userId)
                    .header(DpsHeaders.APP_ID, appIds)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common"));
            return objectMapper.readValue(
                    result.andExpect(MockMvcResultMatchers.status().isOk()).andReturn().getResponse().getContentAsString(),
                    ListGroupResponseDto.class);
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
        return ListGroupResponseDto.builder().build();
    }

    private void performDeleteMemberRequest(String memberEmail, String jwt, String userId) throws Exception {
        mockMvc.perform(delete("/members/{member_email}", memberEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(DpsHeaders.USER_ID, userId)
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
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + SERVICE_P_JWT)
                .header(DpsHeaders.USER_ID, servicePrincipal)
                .header(DpsHeaders.DATA_PARTITION_ID, "common");
        mockMvc.perform(request).andDo(MockMvcResultHandlers.print()).andExpect(status().isOk());
        assertGroupsEquals(new String[]{"users@common.contoso.com",
                "users.datalake.editors@common.contoso.com", "service.storage.viewer@common.contoso.com",
                "service.workflow.creator@common.contoso.com", "service.search.user@common.contoso.com",
                "service.legal.user@common.contoso.com", "service.file.viewers@common.contoso.com",
                "service.schema-service.admin@common.contoso.com", "service.plugin.user@common.contoso.com",
                "service.messaging.user@common.contoso.com", "service.schema-service.editors@common.contoso.com",
                "service.legal.admin@common.contoso.com", "service.schema-service.viewers@common.contoso.com",
                "users.data.root@common.contoso.com", "service.workflow.viewer@common.contoso.com",
                "users.datalake.ops@common.contoso.com", "users.datalake.admins@common.contoso.com",
                "service.legal.editor@common.contoso.com", "service.entitlements.admin@common.contoso.com",
                "data.default.owners@common.contoso.com", "service.file.editors@common.contoso.com",
                "service.entitlements.user@common.contoso.com", "service.search.admin@common.contoso.com",
                "service.storage.admin@common.contoso.com", "users.datalake.viewers@common.contoso.com",
                "service.storage.creator@common.contoso.com", "service.workflow.admin@common.contoso.com",
                "data.default.viewers@common.contoso.com"}, performListGroupRequest(SERVICE_P_JWT, servicePrincipal));
    }
}
