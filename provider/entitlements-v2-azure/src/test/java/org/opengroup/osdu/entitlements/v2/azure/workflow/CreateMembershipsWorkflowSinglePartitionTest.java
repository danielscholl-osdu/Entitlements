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
import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.opengroup.osdu.entitlements.v2.api.CreateGroupApi;
import org.opengroup.osdu.entitlements.v2.api.DeleteGroupApi;
import org.opengroup.osdu.entitlements.v2.api.DeleteMemberApi;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.azure.AzureAppProperties;
import org.opengroup.osdu.entitlements.v2.azure.config.CacheConfig;
import org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses.HitsNMissesMetricService;
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
import org.redisson.api.RLock;
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
    private RedisAzureCache<String, ParentReferences> redisGroupCache;
    @MockBean
    private CacheConfig cacheConfig;
    @Mock
    private RLock cacheLock;
    @MockBean
    private Retry retry;
    @MockBean
    private HitsNMissesMetricService metricService;
    @MockBean
    private IEventPublisher eventPublisher;

    @Before
    public void before() throws InterruptedException {
        Mockito.when(config.getDomain()).thenReturn("contoso.com");
        Mockito.when(config.getProjectId()).thenReturn("evd-ddl-us-services");
        Mockito.when(config.getInitialGroups()).thenCallRealMethod();
        Mockito.when(config.getGroupsOfServicePrincipal()).thenCallRealMethod();
        Mockito.when(config.getProtectedMembers()).thenCallRealMethod();
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setProjectId("evd-ddl-us-common");
        tenantInfo.setDataPartitionId("common");
        tenantInfo.setServiceAccount("service_principal.com");
        Mockito.when(tenantFactory.getTenantInfo("common")).thenReturn(tenantInfo);
        when(authService.isAuthorized(any(), any())).thenReturn(true);
        when(redisGroupCache.getLock(any())).thenReturn(cacheLock);
        when(cacheLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
    }

    @Test
    public void shouldRunWorkflowSuccessfully() throws Exception {
        performBootstrappingOfGroupsAndUsers();

        //running second time to prove the the api is idempotent
        performBootstrappingOfGroupsAndUsers();

        testProtectionAgainstBootstrapGroupsRemoval();

        testProtectionAgainstServicePrincipalUserRemoval();

        String rootUserGroup = "users@common.contoso.com";
        performAddMemberRequest(new AddMemberDto(userA, Role.MEMBER), rootUserGroup, servicePrincipal);
        performAddMemberRequest(new AddMemberDto(userB, Role.MEMBER), rootUserGroup, servicePrincipal);
        performAddMemberRequest(new AddMemberDto(userC, Role.MEMBER), rootUserGroup, servicePrincipal);

        //create users group
        performCreateGroupRequest("users.myusers.operators", userA);
        assertGroupsEquals(new String[]{
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListGroupRequest(userA));

        //create data group
        performCreateGroupRequest("data.mydata1.operators", userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(userA));

        testProtectionAgainstUsersDataRootGroupRemoval("data.mydata1.operators");

        updateGroupMetadata();

        //add data groups to users group
        performCreateGroupRequest("data.mydata2.operators", userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(userA));

        String dataGroup1 = "data.mydata1.operators@common.contoso.com";
        String dataGroup2 = "data.mydata2.operators@common.contoso.com";
        String userGroup1 = "users.myusers.operators@common.contoso.com";

        addDataGroup1ToDataGroup2(dataGroup1, dataGroup2);

        addUserGroupToDataGroups(userGroup1, dataGroup1, dataGroup2);

        addNewUserToUsersOperators(userB, userGroup1);

        //create new root hierarchy
        performCreateGroupRequest("users.myusers2.operators", userC);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(userC));

        //add it to users operators
        String userGroup2 = "users.myusers2.operators@common.contoso.com";
        performAddMemberRequest(new AddMemberDto(userGroup2, Role.MEMBER), userGroup1, userA);

        assertMembersEquals(new String[]{userA,
                userB,
                "users.myusers2.operators@common.contoso.com"}, performListMemberRequest(userGroup1, userA));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(userC));

        //list groups by memberEmail
        listGroupsByMemberEmail();

        removeDataGroup1FromDataGroup2(dataGroup1, dataGroup2);
        deleteDataGroup2(dataGroup2);
        deleteUserGroup1(userGroup1, dataGroup1);

        performAddMemberRequest(new AddMemberDto(userB, Role.OWNER), userGroup2, userC);
        performDeleteGroupRequest(userGroup2, userB);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(userA));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com"}, performListGroupRequest(userB));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com"}, performListGroupRequest(userC));

        testGroupUpdateApi();

        testDeleteMemberApi();
    }

    private void testProtectionAgainstUsersDataRootGroupRemoval(String dataGroup) {
        try {
            performRemoveMemberRequest(dataGroup + "@common.contoso.com",
                    "users.data.root@common.contoso.com", servicePrincipal)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json("{\"code\":400,\"reason\":\"Bad Request\",\"message\":\"Users" +
                            " data root group hierarchy is enforced, member users.data.root cannot be removed\"}"));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    private void removeDataGroup1FromDataGroup2(String dataGroup1, String dataGroup2) {
        try {
            performRemoveMemberRequest(dataGroup2, dataGroup1, userA).andExpect(status().isNoContent());
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
            throw new RuntimeException(e);
        }
        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, userA));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(userB));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(userC));
    }


    private void testProtectionAgainstBootstrapGroupsRemoval() {
        validateBadRequestOnBootstrapGroupRemoval("data.default.viewers", "users");
        validateBadRequestOnBootstrapGroupRemoval("data.default.owners", "users");
        validateBadRequestOnBootstrapGroupRemoval("service.storage.viewer", "users.datalake.viewers");
        validateBadRequestOnBootstrapGroupRemoval("service.storage.viewer", "users.datalake.editors");
        validateBadRequestOnBootstrapGroupRemoval("service.storage.viewer", "users.datalake.admins");
        validateBadRequestOnBootstrapGroupRemoval("service.storage.viewer", "users.datalake.ops");
    }

    private void testProtectionAgainstServicePrincipalUserRemoval() {
        try {
            performRemoveMemberRequest("users.datalake.ops@common.contoso.com",
                    servicePrincipal, servicePrincipal)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json("{\"code\":400,\"reason\":\"Bad Request\",\"message\":\"Key service" +
                            " accounts hierarchy is enforced, service_principal.com cannot" +
                            " be removed from group users.datalake.ops@common.contoso.com\"}"));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    private void validateBadRequestOnBootstrapGroupRemoval(String protectedGroupName, String protectedMemberName) {
        String expectedResponseBody = String.format("{\"code\":400,\"reason\":\"Bad Request\",\"message\":\"Bootstrap group hierarchy" +
                " is enforced, member %s cannot be removed from group %s\"}", protectedMemberName, protectedGroupName);
        try {
            performRemoveMemberRequest(protectedGroupName + "@common.contoso.com",
                    protectedMemberName + "@common.contoso.com", servicePrincipal)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json(expectedResponseBody));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    private void deleteDataGroup2(String dataGroup2) {
        performDeleteGroupRequest(dataGroup2, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(userA));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(userB));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(userC));
    }

    private void deleteUserGroup1(String userGroup1, String dataGroup1) {
        performDeleteGroupRequest(userGroup1, userA);
        assertMembersEquals(new String[]{userA,
                "users.data.root@common.contoso.com"}, performListMemberRequest(dataGroup1, userA));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(userA));
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com"}, performListGroupRequest(userB));
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers2.operators@common.contoso.com"}, performListGroupRequest(userC));
    }

    private void addDataGroup1ToDataGroup2(String dataGroup1, String dataGroup2) {
        performAddMemberRequest(new AddMemberDto(dataGroup1, Role.MEMBER), dataGroup2, userA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, userA));
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com"}, performListMemberRequest(dataGroup1, userA));
    }

    private void addUserGroupToDataGroups(String userGroup, String dataGroup1, String dataGroup2) {
        performAddMemberRequest(new AddMemberDto(userGroup, Role.MEMBER), dataGroup1, userA);
        performAddMemberRequest(new AddMemberDto(userGroup, Role.MEMBER), dataGroup2, userA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup2, userA));

        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListMemberRequest(dataGroup1, userA));
    }

    private void addNewUserToUsersOperators(String newUser, String userGroup) {
        performAddMemberRequest(new AddMemberDto(newUser, Role.MEMBER), userGroup, userA);
        assertMembersEquals(new String[]{
                userA,
                userB}, performListMemberRequest(userGroup, userA));

        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "users@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata2.operators@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequest(newUser));
    }

    private void updateGroupMetadata() throws Exception {
        List<String> appIds = Arrays.asList("App1", "App2");
        List<String> audience = Collections.singletonList("test.com");
        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(appIds)),
                "data.mydata1.operators@common.contoso.com", userA, appIds);

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListGroupRequestWithAppId(userA, audience));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com"}, performListGroupsOnBehalfOfRequest(userA, "NONE", "test.com"));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupsOnBehalfOfRequest(userA, "NONE", "App1"));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupsOnBehalfOfRequest(userA, "NONE", "App2"));
        //update group metadata with audience

        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(audience)),
                "data.mydata1.operators@common.contoso.com", userA, audience);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequestWithAppId(userA, audience));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupsOnBehalfOfRequest(userA, "NONE", "test.com"));

        //update group metadata with empty list
        performUpdateGroupRequest(Collections.singletonList(getUpdateAppIdsOperation(Collections.emptyList())),
                "data.mydata1.operators@common.contoso.com", userA, audience);
        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupRequestWithAppId(userA, audience));

        assertGroupsEquals(new String[]{
                "users@common.contoso.com",
                "users.myusers.operators@common.contoso.com",
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "data.mydata1.operators@common.contoso.com"}, performListGroupsOnBehalfOfRequest(userA, "NONE", "test.com"));
    }

    private void testGroupUpdateApi() throws Exception {
        // there aren't new groups
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com"
        }, performListGroupRequest(userA));

        checkThatUsersGroupCannotBeRenamedByExistingName();

        // data group was created
        String dataGroupName = "data.testdata.operators";
        String dataGroupEmail = dataGroupName + "@common.contoso.com";
        performCreateGroupRequest(dataGroupName, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail
        }, performListGroupRequest(userA));

        // users group 1 was created to be updated by a new email
        String usersGroup1Name = "users.testusers1.operators";
        String usersGroup1Email = usersGroup1Name + "@common.contoso.com";
        performCreateGroupRequest(usersGroup1Name, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email}, performListGroupRequest(userA));

        // users group 2 was created
        String usersGroup2Name = "users.testusers2.operators";
        String usersGroup2Email = usersGroup2Name + "@common.contoso.com";
        performCreateGroupRequest(usersGroup2Name, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email,
                usersGroup2Email}, performListGroupRequest(userA));

        // users group 1 was added to data group
        performAddMemberRequest(new AddMemberDto(usersGroup1Email, Role.MEMBER), dataGroupEmail, userA);
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                usersGroup1Email}, performListMemberRequest(dataGroupEmail, userA));

        // users group 2 was added to users group 1
        performAddMemberRequest(new AddMemberDto(usersGroup2Email, Role.MEMBER), usersGroup1Email, userA);
        assertMembersEquals(new String[]{
                userA,
                usersGroup2Email
        }, performListMemberRequest(usersGroup1Email, userA));

        // users group 1 email was updated
        String newUsersGroup1Name = "users.testusers3.operators";
        String newUsersGroup1Email = newUsersGroup1Name + "@common.contoso.com";

        checkThatUsersGroupCannotBeRenamedToBootstrapGroup(usersGroup1Email);
        checkBootstrappedGroupCannotBeRenamed(newUsersGroup1Name);

        performUpdateGroupRequest(Collections.singletonList(getRenameGroupOperation(newUsersGroup1Name)),
                usersGroup1Email, servicePrincipal, Collections.singletonList("test.com"));
        usersGroup1Email = newUsersGroup1Email;
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                dataGroupEmail,
                usersGroup1Email, // updated email
                usersGroup2Email}, performListGroupRequestWithAppId(userA, Collections.singletonList("test.com")));

        // data group has a member with updated email
        assertMembersEquals(new String[]{
                userA,
                "users.data.root@common.contoso.com",
                usersGroup1Email // updated email
        }, performListMemberRequest(dataGroupEmail, userA));

        // users group 2 still is a member of users group 1 even after the updated email
        assertMembersEquals(new String[]{
                userA,
                usersGroup2Email}, performListMemberRequest(usersGroup1Email, userA)); // updated email

        // clean-up
        performDeleteGroupRequest(dataGroupEmail, servicePrincipal);
        performDeleteGroupRequest(usersGroup1Email, servicePrincipal);
        performDeleteGroupRequest(usersGroup2Email, servicePrincipal);
    }

    private void checkThatUsersGroupCannotBeRenamedByExistingName() throws Exception {
        // users group 1 was created
        String usersGroup1Name = "users.testusers1.operators";
        String usersGroup1Email = usersGroup1Name + "@common.contoso.com";

        //"users group 1 to be updated by a new email"
        performCreateGroupRequest(usersGroup1Name, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                usersGroup1Email}, performListGroupRequest(userA));

        // users group 2 was created
        String usersGroup2Name = "users.testusers2.operators";
        String usersGroup2Email = usersGroup2Name + "@common.contoso.com";
        performCreateGroupRequest(usersGroup2Name, userA);
        assertGroupsEquals(new String[]{
                "data.default.owners@common.contoso.com",
                "data.mydata1.operators@common.contoso.com",
                "data.default.viewers@common.contoso.com",
                "users@common.contoso.com",
                usersGroup1Email,
                usersGroup2Email}, performListGroupRequest(userA));

        // bad request when users group 1 email is updated by users group 2 name
        mockMvc.perform(patch("/groups/{group_email}", usersGroup1Email)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(Collections.singletonList(getRenameGroupOperation(usersGroup2Name))))
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
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
                usersGroup2Email}, performListGroupRequest(userA));

        // clean-up
        performDeleteGroupRequest(usersGroup1Email, servicePrincipal);
        performDeleteGroupRequest(usersGroup2Email, servicePrincipal);
    }

    private void checkThatUsersGroupCannotBeRenamedToBootstrapGroup(final String existingGroupEmail) throws Exception {
        String nameOfBootstrappedGroup = "users.datalake.ops";

        // bad request when users group 1 email is updated by users group 2 name
        mockMvc.perform(patch("/groups/{group_email}", existingGroupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(objectMapper.writeValueAsString(Collections.singletonList(getRenameGroupOperation(nameOfBootstrappedGroup))))
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
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
                "users@common.contoso.com"}, performListGroupRequest(userA));
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
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
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
                "users@common.contoso.com"}, performListGroupRequest(userA));
    }

    private void testDeleteMemberApi() throws Exception {
        String rootUserGroup = "users@common.contoso.com";

        String permissionGroupName = "users.test.editors";
        String permissionGroup = permissionGroupName + "@common.contoso.com";
        performCreateGroupRequest(permissionGroupName, servicePrincipal);

        assertTrue(performListGroupRequest(USER_FOR_DELETION).getGroups().isEmpty());
        performAddMemberRequest(new AddMemberDto(USER_FOR_DELETION, Role.MEMBER), rootUserGroup, servicePrincipal);
        performAddMemberRequest(new AddMemberDto(USER_FOR_DELETION, Role.MEMBER), permissionGroup, servicePrincipal);

        assertGroupsEquals(new String[]{
                rootUserGroup,
                permissionGroup,
                "data.default.owners@common.contoso.com",
                "data.default.viewers@common.contoso.com"
        }, performListGroupRequest(USER_FOR_DELETION));
        performDeleteMemberRequest(USER_FOR_DELETION, servicePrincipal);
        assertTrue(performListGroupRequest(USER_FOR_DELETION).getGroups().isEmpty());

        performDeleteGroupRequest(permissionGroup, servicePrincipal);
    }

    private void performCreateGroupRequest(String groupName, String userId) {
        try {
            CreateGroupDto dto = new CreateGroupDto(groupName, groupName + ": description");
            mockMvc.perform(MockMvcRequestBuilders.post("/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                    .header(DpsHeaders.USER_ID, userId)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common")
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(MockMvcResultMatchers.status().isCreated());
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
    }

    private void performDeleteGroupRequest(String groupEmail, String userId) {
        try {
            mockMvc.perform(MockMvcRequestBuilders.delete("/groups/{group_email}", groupEmail)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                    .header(DpsHeaders.USER_ID, userId)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common"))
                    .andExpect(MockMvcResultMatchers.status().isNoContent());
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
    }

    private void performAddMemberRequest(AddMemberDto dto, String groupEmail, String userId) {
        try {
            mockMvc.perform(MockMvcRequestBuilders.post("/groups/{group_email}/members", groupEmail)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                    .header(DpsHeaders.USER_ID, userId)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common")
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
    }

    private ListGroupResponseDto performListGroupRequest(String userId) {
        try {
            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer token")
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
                    .header(DpsHeaders.AUTHORIZATION, "Bearer token")
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

    private ListGroupResponseDto performListGroupsOnBehalfOfRequest(String memberId, String groupType, String appId) {
        try {
            ResultActions result = mockMvc.perform(get("/members/{member_email}/groups", memberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                    .header(DpsHeaders.USER_ID, servicePrincipal)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common")
                    .queryParam("type", groupType)
                    .queryParam("appid", appId));
            return objectMapper.readValue(
                    result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                    ListGroupResponseDto.class);
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
        }
        return ListGroupResponseDto.builder().build();
    }

    private ListMemberResponseDto performListMemberRequest(String groupEmail, String userId) {
        try {
            ResultActions result = mockMvc.perform(get("/groups/{group_email}/members", groupEmail)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer token")
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

    private ResultActions performRemoveMemberRequest(String groupEmail, String memberEmail, String userId) {
        try {
            return mockMvc.perform(delete("/groups/{group_email}/members/{member_email}", groupEmail, memberEmail)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                    .header(DpsHeaders.USER_ID, userId)
                    .header(DpsHeaders.DATA_PARTITION_ID, "common"));
        } catch (Exception e) {
            Assert.fail("Exception shouldn't take place here");
            throw new RuntimeException(e);
        }
    }

    private void performUpdateGroupRequest(List<UpdateGroupOperation> operations, String groupEmail, String userId, List<String> appIds) throws Exception {
        mockMvc.perform(patch("/groups/{group_email}", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .header(DpsHeaders.USER_ID, userId)
                .header(DpsHeaders.APP_ID, appIds)
                .content(objectMapper.writeValueAsString(operations)))
                .andExpect(status().isOk());
    }

    private ListGroupResponseDto performListGroupRequestWithAppId(String userId, List<String> appIds) {
        try {
            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8.name())
                    .header(DpsHeaders.AUTHORIZATION, "Bearer token")
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

    private void performDeleteMemberRequest(String memberEmail, String userId) throws Exception {
        mockMvc.perform(delete("/members/{member_email}", memberEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
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
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.USER_ID, servicePrincipal)
                .header(DpsHeaders.DATA_PARTITION_ID, "common");
        mockMvc.perform(request).andDo(MockMvcResultHandlers.print()).andExpect(status().isOk());
        assertGroupsEquals(new String[]{"users@common.contoso.com",
                "users.datalake.editors@common.contoso.com", "service.storage.viewer@common.contoso.com",
                "service.workflow.creator@common.contoso.com", "service.search.user@common.contoso.com",
                "service.legal.user@common.contoso.com", "service.file.viewers@common.contoso.com",
                "service.schema-service.admin@common.contoso.com", "service.policy.user@common.contoso.com",
                "service.plugin.user@common.contoso.com", "service.messaging.user@common.contoso.com",
                "service.schema-service.editors@common.contoso.com", "service.legal.admin@common.contoso.com",
                "service.schema-service.viewers@common.contoso.com", "users.data.root@common.contoso.com",
                "service.workflow.viewer@common.contoso.com", "users.datalake.ops@common.contoso.com",
                "users.datalake.admins@common.contoso.com", "service.legal.editor@common.contoso.com",
                "service.entitlements.admin@common.contoso.com", "data.default.owners@common.contoso.com",
                "service.policy.admin@common.contoso.com", "service.file.editors@common.contoso.com",
                "service.entitlements.user@common.contoso.com", "service.search.admin@common.contoso.com",
                "service.storage.admin@common.contoso.com", "users.datalake.viewers@common.contoso.com",
                "service.storage.creator@common.contoso.com", "service.workflow.admin@common.contoso.com",
                "data.default.viewers@common.contoso.com", "service.dataset.editors@common.contoso.com",
                "service.dataset.viewers@common.contoso.com", "service.secret.editor@common.contoso.com",
                "service.secret.admin@common.contoso.com", "service.secret.viewer@common.contoso.com",
                "service.edsdms.user@common.contoso.com"},
            performListGroupRequest(servicePrincipal));
    }
}
