package org.opengroup.osdu.entitlements.v2.service;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.deletegroup.DeleteGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeEvent;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeType;
import org.opengroup.osdu.entitlements.v2.service.util.ReflectionTestUtil;
import org.opengroup.osdu.entitlements.v2.spi.deletegroup.DeleteGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DeleteGroupService.class, System.class})
public class DeleteGroupServiceTests {

    @Mock
    private DeleteGroupRepo deleteGroupRepo;
    @Mock
    private RetrieveGroupRepo retrieveGroupRepo;
    @Mock
    private GroupCacheService groupCacheService;
    @Mock
    private JaxRsDpsLog logger;
    @Mock
    private DefaultGroupsService defaultGroupsService;
    @Mock
    private RequestInfo requestInfo;
    @Mock
    private PermissionService permissionService;
    @InjectMocks
    private DeleteGroupService service;
    @Mock
    private RequestInfoUtilService requestInfoUtilService;
    @Mock
    private IEventPublisher publisher;
    @Mock
    private DpsHeaders headers;

    private static final Map<String, String> headersMap = Collections.singletonMap("testKey", "testValue");

    @Before
    public void setup() {
        PowerMockito.mockStatic(System.class);
        when(requestInfo.getTenantInfo()).thenReturn(new TenantInfo());
        when(requestInfo.getHeaders()).thenReturn(headers);
        when(headers.getHeaders()).thenReturn(headersMap);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(1291371330000L);
        ReflectionTestUtil.setFieldValueForClass(service, "eventPublishingEnabled", true);
    }

    @Test
    public void shouldSuccessfullyDeleteGroupEmailAndPublishDeleteGroupEntitlementsChangeEvent() {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x.viewers@common.contoso.com").name("data.x.viewers")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("data.x.viewers@common.contoso.com", "common")).thenReturn(Optional.of(groupNode));
        EntityNode requesterNode = EntityNode.builder().nodeId("callerdesid").name("callerdesid").type(NodeType.USER).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("callerdesid", "common")).thenReturn(Optional.of(requesterNode));
        when(retrieveGroupRepo.groupExistenceValidation("data.x.viewers@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.TRUE);
        DeleteGroupServiceDto deleteGroupServiceDto = DeleteGroupServiceDto.builder()
                .requesterId("callerdesid")
                .partitionId("common").build();
        when(defaultGroupsService.isDefaultGroupName("data.x.viewers")).thenReturn(false);
        Set<String> impactedUsers = new HashSet<>(Collections.singletonList("callerdesid"));
        when(deleteGroupRepo.deleteGroup(any())).thenReturn(impactedUsers);

        EntitlementsChangeEvent[] event = {
                EntitlementsChangeEvent.builder()
                        .kind(EntitlementsChangeType.groupDeleted)
                        .group("data.x.viewers@common.contoso.com")
                        .modifiedBy("callerdesid")
                        .modifiedOn(1291371330000L).build()
        };

        this.service.run(groupNode, deleteGroupServiceDto);

        verify(deleteGroupRepo).deleteGroup(groupNode);
        verify(groupCacheService).refreshListGroupCache(impactedUsers, "common");
        verify(publisher).publish(event, headersMap);
    }

    @Test
    public void shouldReturnButDoNotCallRepoDeleteGroupIfGroupNodeDoesNotExist() {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x.viewers@common.contoso.com").name("data.x.viewers")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("data.x.viewers@common.contoso.com", "common")).thenReturn(Optional.ofNullable(null));
        DeleteGroupServiceDto deleteGroupServiceDto = DeleteGroupServiceDto.builder()
                .requesterId("callerdesid")
                .partitionId("common").build();

        this.service.run(groupNode, deleteGroupServiceDto);

        verify(deleteGroupRepo, never()).deleteGroup(any(EntityNode.class));
        verify(publisher, times(0)).publish(any(), any());
    }

    @Test
    public void shouldReturnIfCallerDoesNotBelongToTheGroupAndDatafierCallerAndPublishDeleteGroupEntitlementsChangeEvent() {
        String caller = "datafier@evd-ddl-us-common.iam.gserviceaccount.com";
        String partition = "common";

        EntityNode groupNode = EntityNode.builder().nodeId("data.x.viewers@common.contoso.com").name("users")
                .type(NodeType.GROUP).dataPartitionId(partition).build();
        when(retrieveGroupRepo.getEntityNode("data.x.viewers@common.contoso.com", partition)).thenReturn(Optional.of(groupNode));
        EntityNode requesterNode = EntityNode.builder().nodeId(caller).name(caller).type(NodeType.USER).dataPartitionId(partition).build();
        when(requestInfoUtilService.getPartitionIdList(any())).thenReturn(Collections.singletonList(partition));
        when(retrieveGroupRepo.getEntityNode(caller, partition)).thenReturn(Optional.of(requesterNode));
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setServiceAccount("datafier@evd-ddl-us-common.iam.gserviceaccount.com");
        when(requestInfo.getTenantInfo()).thenReturn(tenantInfo);
        DeleteGroupServiceDto deleteGroupServiceDto = DeleteGroupServiceDto.builder()
                .requesterId(caller)
                .partitionId(partition).build();
        when(defaultGroupsService.isDefaultGroupName("users")).thenReturn(false);

        EntitlementsChangeEvent[] event = {
                EntitlementsChangeEvent.builder()
                        .kind(EntitlementsChangeType.groupDeleted)
                        .group("data.x.viewers@common.contoso.com")
                        .modifiedBy("datafier@evd-ddl-us-common.iam.gserviceaccount.com")
                        .modifiedOn(1291371330000L).build()
        };

        try {
            this.service.run(groupNode, deleteGroupServiceDto);
        } catch (Exception e) {
            e.printStackTrace();
            fail(String.format("should not throw exception: %s", e));
        }
        verify(publisher).publish(event, headersMap);
    }

    @Test
    public void shouldThrow400IfGivenGroupIsABootstrapGroup() {
        EntityNode groupNode = EntityNode.builder().nodeId("users@common.contoso.com").name("users")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("users@common.contoso.com", "common")).thenReturn(Optional.of(groupNode));
        EntityNode requesterNode = EntityNode.builder().nodeId("callerdesid").name("callerdesid").type(NodeType.USER).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("callerdesid", "common")).thenReturn(Optional.of(requesterNode));
        when(retrieveGroupRepo.groupExistenceValidation("users@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.TRUE);
        DeleteGroupServiceDto deleteGroupServiceDto = DeleteGroupServiceDto.builder()
                .requesterId("callerdesid")
                .partitionId("common").build();
        when(defaultGroupsService.isDefaultGroupName("users")).thenReturn(true);

        try {
            this.service.run(groupNode, deleteGroupServiceDto);
            fail("Should not succeed");
        } catch (AppException e) {
            assertEquals(HttpStatus.SC_BAD_REQUEST, e.getError().getCode());
            assertEquals("Bad Request", e.getError().getReason());
            assertEquals("Invalid group, bootstrap groups are not allowed to be deleted", e.getError().getMessage());
            verify(publisher, times(0)).publish(any(), any());
        } catch (Exception e) {
            fail(String.format("should not throw exception: %s", e));
        }
    }
    @Test
    public void shouldSuccessfullyDeleteGroupEmail_AndNotPublishDeleteGroupEntitlementsChangeEvent_ifPublishingDisabled() {
        ReflectionTestUtil.setFieldValueForClass(service, "eventPublishingEnabled", false);
        EntityNode groupNode = EntityNode.builder().nodeId("data.x.viewers@common.contoso.com").name("data.x.viewers")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("data.x.viewers@common.contoso.com", "common")).thenReturn(Optional.of(groupNode));
        EntityNode requesterNode = EntityNode.builder().nodeId("callerdesid").name("callerdesid").type(NodeType.USER).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("callerdesid", "common")).thenReturn(Optional.of(requesterNode));
        when(retrieveGroupRepo.groupExistenceValidation("data.x.viewers@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.TRUE);
        DeleteGroupServiceDto deleteGroupServiceDto = DeleteGroupServiceDto.builder()
                .requesterId("callerdesid")
                .partitionId("common").build();
        when(defaultGroupsService.isDefaultGroupName("data.x.viewers")).thenReturn(false);
        Set<String> impactedUsers = new HashSet<>(Collections.singletonList("callerdesid"));
        when(deleteGroupRepo.deleteGroup(any())).thenReturn(impactedUsers);

        this.service.run(groupNode, deleteGroupServiceDto);

        verify(deleteGroupRepo).deleteGroup(groupNode);
        verify(groupCacheService).refreshListGroupCache(impactedUsers, "common");
        verifyNoInteractions(publisher);
    }
}
