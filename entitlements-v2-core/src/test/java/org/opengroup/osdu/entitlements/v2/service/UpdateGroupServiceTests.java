package org.opengroup.osdu.entitlements.v2.service;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeAction;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeEvent;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeType;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupOperation;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.service.util.ReflectionTestUtil;
import org.opengroup.osdu.entitlements.v2.spi.renamegroup.RenameGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.updateappids.UpdateAppIdsRepo;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UpdateGroupService.class, System.class})
public class UpdateGroupServiceTests {

    private static final String RENAME_PATH = "/name";
    private static final String APP_IDS_PATH = "/appIds";
    private static final String TEST_EXISTING_USER_GROUP_NAME = "users.x";
    private static final String TEST_EXISTING_USER_GROUP_EMAIL = "users.x@dp.domain";
    private static final String TEST_EXISTING_DATA_GROUP_NAME = "data.x";
    private static final String TEST_EXISTING_DATA_GROUP_EMAIL = "data.x@dp.domain";
    private static final String TEST_PARTITION = "dp";
    private static final String TEST_PARTITION_DOMAIN = "dp.domain";
    private static final String USER_A = "userA";
    private static final String REPLACE_OPERATION = "replace";

    @Mock
    private RetrieveGroupRepo retrieveGroupRepo;
    @Mock
    private GroupCacheService groupCacheService;
    @Mock
    private RenameGroupRepo renameGroupRepo;
    @Mock
    private UpdateAppIdsRepo updateAppIdsRepo;
    @Mock
    private DefaultGroupsService defaultGroupsService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private RequestInfo requestInfo;
    @Mock
    private IEventPublisher publisher;
    @Mock
    private DpsHeaders headers;

    private static final Map<String, String> headersMap = Collections.singletonMap("testKey", "testValue");

    @InjectMocks
    private UpdateGroupService updateGroupService;

    @Before
    public void setup() {
        PowerMockito.mockStatic(System.class);
        when(requestInfo.getHeaders()).thenReturn(headers);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(1291371330000L);
        when(headers.getHeaders()).thenReturn(headersMap);
        ReflectionTestUtil.setFieldValueForClass(updateGroupService, "eventPublishingEnabled", true);
        ReflectionTestUtil.setFieldValueForClass(updateGroupService, "eventPublisher", publisher);
    }

    @Test
    public void shouldRenameGroupSuccessfullyAndPublishEntitlementsChangeEventWhenUserGroupExists() {
        String newGroupName = "users.test.x";
        EntityNode groupNode = createGroupNode(TEST_EXISTING_USER_GROUP_NAME, TEST_EXISTING_USER_GROUP_EMAIL);
        UpdateGroupOperation renameOperation = createUpdateGroupOperation(RENAME_PATH, newGroupName);
        UpdateGroupServiceDto serviceDto = createUpdateGroupServiceDto(TEST_EXISTING_USER_GROUP_EMAIL, renameOperation, null);

        Mockito.when(retrieveGroupRepo.groupExistenceValidation(TEST_EXISTING_USER_GROUP_EMAIL, TEST_PARTITION)).thenReturn(groupNode);
        Mockito.when(permissionService.hasOwnerPermissionOf(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(renameGroupRepo.run(groupNode, newGroupName)).thenReturn(Collections.emptySet());

        EntitlementsChangeEvent[] event = {
                EntitlementsChangeEvent.builder()
                        .kind(EntitlementsChangeType.groupChanged)
                        .group(TEST_EXISTING_USER_GROUP_EMAIL)
                        .updatedGroupEmail("users.test.x@dp.domain")
                        .action(EntitlementsChangeAction.replace)
                        .modifiedBy(USER_A)
                        .modifiedOn(1291371330000L).build()
        };

        UpdateGroupResponseDto responseDto = updateGroupService.updateGroup(serviceDto);
        Mockito.verify(renameGroupRepo).run(groupNode, newGroupName);
        Assert.assertNotNull(responseDto);
        Assert.assertEquals("users.test.x@dp.domain", responseDto.getEmail());
        Assert.assertEquals("users.test.x", responseDto.getName());
        Mockito.verify(groupCacheService).refreshListGroupCache(Collections.emptySet(), "dp");
        verify(publisher).publish(event, headersMap);
    }

    @Test
    public void shouldThrow400WhenTryToRenameDataGroup() {
        String newGroupName = "data.test.x";
        EntityNode groupNode = createGroupNode(TEST_EXISTING_DATA_GROUP_NAME, TEST_EXISTING_DATA_GROUP_EMAIL);
        UpdateGroupOperation renameOperation = createUpdateGroupOperation(RENAME_PATH, newGroupName);
        UpdateGroupServiceDto serviceDto = createUpdateGroupServiceDto(TEST_EXISTING_DATA_GROUP_EMAIL, renameOperation, null);

        Mockito.when(retrieveGroupRepo.groupExistenceValidation(TEST_EXISTING_DATA_GROUP_EMAIL, TEST_PARTITION)).thenReturn(groupNode);
        Mockito.when(permissionService.hasOwnerPermissionOf(Mockito.any(), Mockito.any())).thenReturn(true);

        try {
            updateGroupService.updateGroup(serviceDto);
            Assert.fail("should throw exception");
        } catch (AppException ex) {
            Mockito.verify(renameGroupRepo, Mockito.never()).run(Mockito.any(), Mockito.any());
            Assert.assertEquals(400, ex.getError().getCode());
            verify(publisher, times(0)).publish(any(), any());
        } catch (Exception ex) {
            Assert.fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldThrow400WhenNewGroupNameIsAlreadyExist() {
        String newGroupName = "users.test.x";
        String newGroupEmail = "users.test.x@dp.domain";
        EntityNode groupNode = createGroupNode(TEST_EXISTING_USER_GROUP_NAME, TEST_EXISTING_USER_GROUP_EMAIL);

        UpdateGroupOperation renameOperation = createUpdateGroupOperation(RENAME_PATH, newGroupName);
        UpdateGroupServiceDto serviceDto = createUpdateGroupServiceDto(TEST_EXISTING_USER_GROUP_EMAIL, renameOperation, null);

        Mockito.when(retrieveGroupRepo.groupExistenceValidation(TEST_EXISTING_USER_GROUP_EMAIL, TEST_PARTITION)).thenReturn(groupNode);
        Mockito.when(retrieveGroupRepo.getEntityNode(newGroupEmail, TEST_PARTITION)).thenReturn(Optional.of(groupNode));
        Mockito.when(permissionService.hasOwnerPermissionOf(Mockito.any(), Mockito.any())).thenReturn(true);

        try {
            updateGroupService.updateGroup(serviceDto);
            Assert.fail("should throw exception");
        } catch (AppException ex) {
            Mockito.verify(renameGroupRepo, Mockito.never()).run(Mockito.any(), Mockito.any());
            Assert.assertEquals(400, ex.getError().getCode());
            verify(publisher, times(0)).publish(any(), any());
        } catch (Exception ex) {
            Assert.fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldThrow400IfGivenNewGroupIsABootstrapGroup() {
        String newGroupName = "users.test.x";
        EntityNode groupNode = createGroupNode(TEST_EXISTING_USER_GROUP_NAME, TEST_EXISTING_USER_GROUP_EMAIL);
        UpdateGroupOperation renameOperation = createUpdateGroupOperation(RENAME_PATH, newGroupName);
        UpdateGroupServiceDto serviceDto = createUpdateGroupServiceDto(TEST_EXISTING_USER_GROUP_EMAIL, renameOperation, null);

        Mockito.when(retrieveGroupRepo.groupExistenceValidation(TEST_EXISTING_USER_GROUP_EMAIL, TEST_PARTITION)).thenReturn(groupNode);
        Mockito.when(defaultGroupsService.isDefaultGroupName(newGroupName)).thenReturn(true);
        Mockito.when(permissionService.hasOwnerPermissionOf(Mockito.any(), Mockito.any())).thenReturn(true);

        try {
            updateGroupService.updateGroup(serviceDto);
            Assert.fail("Should not succeed");
        } catch (AppException e) {
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getError().getCode());
            Assert.assertEquals("Bad Request", e.getError().getReason());
            Assert.assertEquals("Invalid group, group update API cannot work with bootstrapped groups", e.getError().getMessage());
            verify(publisher, times(0)).publish(any(), any());
        } catch (Exception e) {
            Assert.fail(String.format("should not throw exception: %s", e));
        }
    }

    @Test
    public void shouldThrow400IfGivenExistingGroupIsABootstrapGroup() {
        String newGroupName = "users.test.x";
        EntityNode groupNode = createGroupNode(TEST_EXISTING_USER_GROUP_NAME, TEST_EXISTING_USER_GROUP_EMAIL);
        UpdateGroupOperation renameOperation = createUpdateGroupOperation(RENAME_PATH, newGroupName);
        UpdateGroupServiceDto serviceDto = createUpdateGroupServiceDto(TEST_EXISTING_USER_GROUP_EMAIL, renameOperation, null);

        Mockito.when(retrieveGroupRepo.groupExistenceValidation(TEST_EXISTING_USER_GROUP_EMAIL, TEST_PARTITION)).thenReturn(groupNode);
        Mockito.when(defaultGroupsService.isDefaultGroupName(TEST_EXISTING_USER_GROUP_NAME)).thenReturn(true);

        try {
            updateGroupService.updateGroup(serviceDto);
            Assert.fail("Should not succeed");
        } catch (AppException e) {
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getError().getCode());
            Assert.assertEquals("Bad Request", e.getError().getReason());
            Assert.assertEquals("Invalid group, group update API cannot work with bootstrapped groups", e.getError().getMessage());
            verify(publisher, times(0)).publish(any(), any());
        } catch (Exception e) {
            Assert.fail(String.format("should not throw exception: %s", e));
        }
    }

    @Test
    public void shouldUpdateGroupAppIdsSuccessfullyWhenUserHasOwnerPermission() {
        EntityNode groupNode = createGroupNode(TEST_EXISTING_USER_GROUP_NAME, TEST_EXISTING_USER_GROUP_EMAIL);
        UpdateGroupOperation appIdsOperation = createUpdateGroupOperation(APP_IDS_PATH, "app1", "app2");
        UpdateGroupServiceDto serviceDto = createUpdateGroupServiceDto(TEST_EXISTING_USER_GROUP_EMAIL, null, appIdsOperation);

        Mockito.when(retrieveGroupRepo.groupExistenceValidation(TEST_EXISTING_USER_GROUP_EMAIL, TEST_PARTITION)).thenReturn(groupNode);
        Mockito.when(permissionService.hasOwnerPermissionOf(Mockito.any(), Mockito.any())).thenReturn(true);

        List<String> appIds = new ArrayList<>();
        appIds.add("app1");
        appIds.add("app2");
        Mockito.when(updateAppIdsRepo.updateAppIds(groupNode, new HashSet<>(appIds))).thenReturn(Collections.emptySet());

        UpdateGroupResponseDto responseDto = updateGroupService.updateGroup(serviceDto);

        Mockito.verify(updateAppIdsRepo).updateAppIds(groupNode, new HashSet<>(appIds));
        Assert.assertNotNull(responseDto);
        Assert.assertEquals(appIds, responseDto.getAppIds());
        Mockito.verify(groupCacheService).refreshListGroupCache(Collections.emptySet(), "dp");
        verify(publisher, times(0)).publish(any(), any());
    }

    @Test
    public void shouldUpdateGroupAppIdsAndRenameSuccessfullyAndPublishEntitlementsChangeEventWhenUserHasOwnerPermissionOfTheUserGroup() {
        String newGroupName = "users.test.x";
        EntityNode groupNode = createGroupNode(TEST_EXISTING_USER_GROUP_NAME, TEST_EXISTING_USER_GROUP_EMAIL);
        UpdateGroupOperation renameOperation = createUpdateGroupOperation(RENAME_PATH, newGroupName);
        UpdateGroupOperation appIdsOperation = createUpdateGroupOperation(APP_IDS_PATH, "app1", "app2");
        UpdateGroupServiceDto serviceDto = createUpdateGroupServiceDto(TEST_EXISTING_USER_GROUP_EMAIL, renameOperation, appIdsOperation);
        Set<String> impactedUsers = new HashSet<>(Collections.singletonList(USER_A));
        List<String> appIds = new ArrayList<>();
        appIds.add("app1");
        appIds.add("app2");

        Mockito.when(retrieveGroupRepo.groupExistenceValidation(TEST_EXISTING_USER_GROUP_EMAIL, TEST_PARTITION)).thenReturn(groupNode);
        Mockito.when(permissionService.hasOwnerPermissionOf(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(renameGroupRepo.run(groupNode, newGroupName)).thenReturn(impactedUsers);
        Mockito.when(updateAppIdsRepo.updateAppIds(groupNode, new HashSet<>(appIds))).thenReturn(impactedUsers);

        EntitlementsChangeEvent[] event = {
                EntitlementsChangeEvent.builder()
                        .kind(EntitlementsChangeType.groupChanged)
                        .group(TEST_EXISTING_USER_GROUP_EMAIL)
                        .updatedGroupEmail("users.test.x@dp.domain")
                        .action(EntitlementsChangeAction.replace)
                        .modifiedBy(USER_A)
                        .modifiedOn(1291371330000L).build()
        };

        UpdateGroupResponseDto responseDto = updateGroupService.updateGroup(serviceDto);

        Mockito.verify(renameGroupRepo).run(groupNode, newGroupName);
        Mockito.verify(updateAppIdsRepo).updateAppIds(groupNode, new HashSet<>(appIds));
        Assert.assertNotNull(responseDto);
        Assert.assertEquals(appIds, responseDto.getAppIds());
        Assert.assertEquals("users.test.x@dp.domain", responseDto.getEmail());
        Assert.assertEquals("users.test.x", responseDto.getName());
        Mockito.verify(groupCacheService).refreshListGroupCache(impactedUsers, "dp");
        verify(publisher).publish(event, headersMap);
    }

    @Test
    public void shouldRenameGroupSuccessfully_AndNotPublishEntitlementsChangeEvent_whenPublishingDisabled() {
        ReflectionTestUtil.setFieldValueForClass(updateGroupService, "eventPublishingEnabled", false);
        String newGroupName = "users.test.x";
        EntityNode groupNode = createGroupNode(TEST_EXISTING_USER_GROUP_NAME, TEST_EXISTING_USER_GROUP_EMAIL);
        UpdateGroupOperation renameOperation = createUpdateGroupOperation(RENAME_PATH, newGroupName);
        UpdateGroupServiceDto serviceDto = createUpdateGroupServiceDto(TEST_EXISTING_USER_GROUP_EMAIL, renameOperation, null);

        Mockito.when(retrieveGroupRepo.groupExistenceValidation(TEST_EXISTING_USER_GROUP_EMAIL, TEST_PARTITION)).thenReturn(groupNode);
        Mockito.when(permissionService.hasOwnerPermissionOf(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(renameGroupRepo.run(groupNode, newGroupName)).thenReturn(Collections.emptySet());

        EntitlementsChangeEvent event = EntitlementsChangeEvent.builder()
                .kind(EntitlementsChangeType.groupChanged)
                .group(TEST_EXISTING_USER_GROUP_EMAIL)
                .updatedGroupEmail("users.test.x@dp.domain")
                .action(EntitlementsChangeAction.replace)
                .modifiedBy(USER_A)
                .modifiedOn(1291371330000L).build();

        UpdateGroupResponseDto responseDto = updateGroupService.updateGroup(serviceDto);
        Mockito.verify(renameGroupRepo).run(groupNode, newGroupName);
        Assert.assertNotNull(responseDto);
        Assert.assertEquals("users.test.x@dp.domain", responseDto.getEmail());
        Assert.assertEquals("users.test.x", responseDto.getName());
        Mockito.verify(groupCacheService).refreshListGroupCache(Collections.emptySet(), "dp");
        verifyNoInteractions(publisher);
    }

    private EntityNode createGroupNode(String groupName, String groupEmail) {
        EntityNode groupNode = EntityNode.builder()
                .nodeId(groupEmail)
                .name(groupName)
                .type(NodeType.GROUP)
                .dataPartitionId(TEST_PARTITION)
                .appIds(new HashSet<>())
                .build();
        return groupNode;
    }

    private UpdateGroupOperation createUpdateGroupOperation(String path, String... value) {
        List<String> operationValue = new ArrayList<>();
        for (String v : value) {
            operationValue.add(v);
        }

        UpdateGroupOperation operation = UpdateGroupOperation.builder()
                .operation(REPLACE_OPERATION)
                .path(path)
                .value(operationValue)
                .build();

        return operation;
    }

    private UpdateGroupServiceDto createUpdateGroupServiceDto(String groupEmail, UpdateGroupOperation renameOperation, UpdateGroupOperation appIdsOperation) {
        UpdateGroupServiceDto serviceDto = UpdateGroupServiceDto.builder()
                .existingGroupEmail(groupEmail)
                .partitionDomain(TEST_PARTITION_DOMAIN)
                .partitionId(TEST_PARTITION)
                .requesterId(USER_A)
                .renameOperation(renameOperation)
                .appIdsOperation(appIdsOperation)
                .build();
        return serviceDto;
    }
}
