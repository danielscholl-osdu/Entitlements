package org.opengroup.osdu.entitlements.v2.service;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.deletegroup.DeleteGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.deletegroup.DeleteGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DeleteGroupServiceTests {

    @MockBean
    private DeleteGroupRepo deleteGroupRepo;
    @MockBean
    private RetrieveGroupRepo retrieveGroupRepo;
    @MockBean
    private GroupCacheService groupCacheService;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private DefaultGroupsService defaultGroupsService;
    @MockBean
    private RequestInfo requestInfo;
    @Autowired
    private DeleteGroupService service;
    @MockBean
    private RequestInfoUtilService requestInfoUtilService;

    @Before
    public void setup() {
        when(requestInfo.getTenantInfo()).thenReturn(new TenantInfo());
    }

    @Test
    public void shouldSuccessfullyDeleteGroupEmail() {
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
    }

    @Test
    public void shouldThrow401IfCallerDoesNotOwnTheGroup() {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x.viewers@common.contoso.com").name("users")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("data.x.viewers@common.contoso.com", "common")).thenReturn(Optional.of(groupNode));
        EntityNode requesterNode = EntityNode.builder().nodeId("callerdesid").name("callerdesid").type(NodeType.USER).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("callerdesid", "common")).thenReturn(Optional.of(requesterNode));
        when(retrieveGroupRepo.groupExistenceValidation("data.x.viewers@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.FALSE);
        DeleteGroupServiceDto deleteGroupServiceDto = DeleteGroupServiceDto.builder()
                .requesterId("callerdesid")
                .partitionId("common").build();
        when(defaultGroupsService.isDefaultGroupName("users")).thenReturn(false);
        when(requestInfo.getTenantInfo()).thenReturn(new TenantInfo());
        try {
            this.service.run(groupNode, deleteGroupServiceDto);
            fail("Should not succeed");
        } catch (AppException e) {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, e.getError().getCode());
            assertEquals("Unauthorized", e.getError().getReason());
            assertEquals("Not authorized to manage members", e.getError().getMessage());
        } catch (Exception e) {
            fail(String.format("should not throw exception: %s", e));
        }
    }

    @Test
    public void shouldReturnIfCallerDoesNotBelongToTheGroupAndDatafierCaller() {
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
        try {
            this.service.run(groupNode, deleteGroupServiceDto);
        } catch (Exception e) {
            e.printStackTrace();
            fail(String.format("should not throw exception: %s", e));
        }
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
        } catch (Exception e) {
            fail(String.format("should not throw exception: %s", e));
        }
    }
}
