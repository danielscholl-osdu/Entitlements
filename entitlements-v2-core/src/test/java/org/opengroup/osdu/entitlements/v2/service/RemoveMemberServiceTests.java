package org.opengroup.osdu.entitlements.v2.service;

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
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeAction;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeEvent;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeType;
import org.opengroup.osdu.entitlements.v2.service.util.ReflectionTestUtil;
import org.opengroup.osdu.entitlements.v2.validation.BootstrapGroupsConfigurationService;
import org.opengroup.osdu.entitlements.v2.validation.ServiceAccountsConfigurationService;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.removemember.RemoveMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoveMemberService.class, System.class})
public class RemoveMemberServiceTests {
    @Mock
    private RetrieveGroupRepo retrieveGroupRepo;
    @Mock
    private GroupCacheService groupCacheService;
    @Mock
    private RemoveMemberRepo removeMemberRepo;
    @Mock
    private RequestInfo requestInfo;
    @Mock
    private JaxRsDpsLog logger;
    @Mock
    private ServiceAccountsConfigurationService serviceAccountsConfigurationService;
    @Mock
    private BootstrapGroupsConfigurationService bootstrapGroupsConfigurationService;
    @Mock
    private RequestInfoUtilService requestInfoUtilService;
    @Mock
    private AppProperties appProperties;
    @Mock
    private PermissionService permissionService;
    @Mock
    private IEventPublisher publisher;
    @Mock
    private DpsHeaders headers;
    @InjectMocks
    private RemoveMemberService removeMemberService;

    private static final Map<String, String> headersMap = Collections.singletonMap("testKey", "testValue");

    @Before
    public void setup() {
        PowerMockito.mockStatic(System.class);
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setServiceAccount("datafier@evd-ddl-us-common.iam.gserviceaccount.com");
        when(requestInfo.getTenantInfo()).thenReturn(tenantInfo);
        when(appProperties.getDomain()).thenReturn("contoso.com");
        when(requestInfo.getHeaders()).thenReturn(headers);
        when(headers.getHeaders()).thenReturn(headersMap);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(1291371330000L);
        ReflectionTestUtil.setFieldValueForClass(removeMemberService, "eventPublishingEnabled", true);
    }

    @Test
    public void shouldSuccessfullyRemoveMemberAndPublishRemoveMemberEntitlementsChangeEventWhenMemberExists() {
        EntityNode memberNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("member@xxx.com")
                .name("member")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.getMemberNodeForRemovalFromGroup("member@xxx.com", "common")).thenReturn(memberNode);
        EntityNode groupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("data.x@common.contoso.com")
                .name("data.x")
                .dataPartitionId("common")
                .build();
        EntityNode requesterNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("requesterid")
                .name("requesterid")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        EntityNode rootDataGroupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("users.data.root@common.contoso.com")
                .name("users.data.root")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation(
                "users.data.root@common.contoso.com", "common")).thenReturn(rootDataGroupNode);
        when(retrieveGroupRepo.hasDirectChild(
                rootDataGroupNode,
                ChildrenReference.createChildrenReference(requesterNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(retrieveGroupRepo.hasDirectChild(
                groupNode,
                ChildrenReference.createChildrenReference(memberNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(requestInfoUtilService.getDomain("common")).thenReturn("common.contoso.com");
        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .groupEmail("data.x@common.contoso.com")
                .memberEmail("member@xxx.com")
                .requesterId("requesterid")
                .partitionId("common")
                .build();
        when(removeMemberRepo.removeMember(any(), any(), any())).thenReturn(Collections.emptySet());

        EntitlementsChangeEvent[] event = {
                EntitlementsChangeEvent.builder()
                        .kind(EntitlementsChangeType.groupChanged)
                        .group("data.x@common.contoso.com")
                        .user("member@xxx.com")
                        .action(EntitlementsChangeAction.remove)
                        .modifiedBy("requesterid")
                        .modifiedOn(1291371330000L).build()
        };

        removeMemberService.removeMember(removeMemberServiceDto);

        verify(removeMemberRepo).removeMember(groupNode, memberNode, removeMemberServiceDto);
        verify(groupCacheService).refreshListGroupCache(Collections.emptySet(), "common");
        verify(publisher).publish(event, headersMap);
    }

    @Test
    public void shouldSuccessfullyRemoveMemberWhenMemberExistsAndPublishRemoveMemberEntitlementsChangeEventWhenDatafierIsExecutingTheRequest() {
        EntityNode memberNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("member@xxx.com")
                .name("member")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.getMemberNodeForRemovalFromGroup("member@xxx.com", "common")).thenReturn(memberNode);
        EntityNode groupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("data.x@common.contoso.com")
                .name("data.x")
                .dataPartitionId("common")
                .build();
        EntityNode requesterNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("datafier@evd-ddl-us-common.iam.gserviceaccount.com")
                .name("datafier@evd-ddl-us-common.iam.gserviceaccount.com")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(
                groupNode,
                ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.FALSE);
        when(retrieveGroupRepo.hasDirectChild(
                groupNode,
                ChildrenReference.createChildrenReference(memberNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(requestInfoUtilService.getDomain("common")).thenReturn("common.contoso.com");
        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .groupEmail("data.x@common.contoso.com")
                .memberEmail("member@xxx.com")
                .requesterId("datafier@evd-ddl-us-common.iam.gserviceaccount.com")
                .partitionId("common")
                .build();
        when(removeMemberRepo.removeMember(any(), any(), any())).thenReturn(Collections.emptySet());

        EntitlementsChangeEvent[] event = {
                EntitlementsChangeEvent.builder()
                        .kind(EntitlementsChangeType.groupChanged)
                        .group("data.x@common.contoso.com")
                        .user("member@xxx.com")
                        .action(EntitlementsChangeAction.remove)
                        .modifiedBy("datafier@evd-ddl-us-common.iam.gserviceaccount.com")
                        .modifiedOn(1291371330000L).build()
        };

        removeMemberService.removeMember(removeMemberServiceDto);

        verify(groupCacheService).refreshListGroupCache(Collections.emptySet(), "common");
        verify(removeMemberRepo).removeMember(groupNode, memberNode, removeMemberServiceDto);
        verify(publisher).publish(event, headersMap);
    }

    @Test
    public void shouldThrow404IfMemberIsNotFound() {
        EntityNode memberNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("member@xxx.com")
                .name("member")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.getMemberNodeForRemovalFromGroup("member@xxx.com", "common")).thenReturn(memberNode);
        EntityNode groupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("data.x@common.contoso.com")
                .name("data.x")
                .dataPartitionId("common")
                .build();
        EntityNode requesterNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("requesterid")
                .name("requesterid")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        EntityNode rootDataGroupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("users.data.root@common.contoso.com")
                .name("users.data.root")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation("users.data.root@common.contoso.com", "common")).thenReturn(rootDataGroupNode);
        when(retrieveGroupRepo.hasDirectChild(rootDataGroupNode, ChildrenReference.createChildrenReference(requesterNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(requestInfoUtilService.getDomain("common")).thenReturn("common.contoso.com");
        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .groupEmail("data.x@common.contoso.com")
                .memberEmail("member@xxx.com")
                .requesterId("requesterid")
                .partitionId("common")
                .build();

        try {
            removeMemberService.removeMember(removeMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            verify(removeMemberRepo, never()).removeMember(any(), any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(404);
            verify(publisher, times(0)).publish(any(), any());
        } catch (Exception ex) {
            fail(String.format("should now throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldThrow404IfMemberDoesNotBelongToGroup() {
        EntityNode memberNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("member@xxx.com")
                .name("member")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.getMemberNodeForRemovalFromGroup("member@xxx.com", "common")).thenReturn(memberNode);
        EntityNode groupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("data.x@common.contoso.com")
                .name("data.x")
                .dataPartitionId("common")
                .build();
        EntityNode requesterNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("requesterid")
                .name("requesterid")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        EntityNode rootDataGroupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("users.data.root@common.contoso.com")
                .name("users.data.root")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation(
                "users.data.root@common.contoso.com", "common")).thenReturn(rootDataGroupNode);
        when(retrieveGroupRepo.hasDirectChild(
                rootDataGroupNode,
                ChildrenReference.createChildrenReference(requesterNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(requestInfoUtilService.getDomain("common")).thenReturn("common.contoso.com");
        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .groupEmail("data.x@common.contoso.com")
                .memberEmail("member@xxx.com")
                .requesterId("requesterid")
                .partitionId("common")
                .build();

        try {
            removeMemberService.removeMember(removeMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            verify(removeMemberRepo, never()).removeMember(any(), any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(404);
            verify(publisher, times(0)).publish(any(), any());
        } catch (Exception ex) {
            fail(String.format("should now throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldThrow400OnRemovalOfBootstrapGroup() {
        EntityNode memberNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("users@common.contoso.com")
                .name("users")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.getMemberNodeForRemovalFromGroup("users@common.contoso.com", "common")).thenReturn(memberNode);
        EntityNode groupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("data.default.owners@common.contoso.com")
                .name("data.default.owners")
                .dataPartitionId("common")
                .build();
        EntityNode requesterNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("requesterid")
                .name("requesterid")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation("data.default.owners@common.contoso.com", "common")).thenReturn(groupNode);
        EntityNode rootDataGroupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("users.data.root@common.contoso.com")
                .name("users.data.root")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation(
                "users.data.root@common.contoso.com", "common")).thenReturn(rootDataGroupNode);
        when(retrieveGroupRepo.hasDirectChild(
                rootDataGroupNode,
                ChildrenReference.createChildrenReference(requesterNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(retrieveGroupRepo.hasDirectChild(
                groupNode,
                ChildrenReference.createChildrenReference(memberNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(requestInfoUtilService.getDomain("common")).thenReturn("common.contoso.com");
        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .groupEmail("data.default.owners@common.contoso.com")
                .memberEmail("users@common.contoso.com")
                .requesterId("requesterid")
                .partitionId("common")
                .build();
        when(bootstrapGroupsConfigurationService.isMemberProtectedFromRemoval(memberNode, groupNode)).thenReturn(true);

        try {
            removeMemberService.removeMember(removeMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            verify(removeMemberRepo, never()).removeMember(any(), any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(400);
            assertEquals("Bootstrap group hierarchy is enforced, member users cannot be removed from group data.default.owners", ex.getError().getMessage());
            verify(publisher, times(0)).publish(any(), any());
        } catch (Exception ex) {
            fail(String.format("should now throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldThrow400IfDeleteKeySvcAccFromBootstrapGroup() {
        EntityNode memberNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("datafier@evd-ddl-us-common.iam.gserviceaccount.com")
                .name("datafier")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.getMemberNodeForRemovalFromGroup(
                "datafier@evd-ddl-us-common.iam.gserviceaccount.com", "common")).thenReturn(memberNode);
        EntityNode groupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("users.data.root@common.contoso.com")
                .name("users.data.root")
                .dataPartitionId("common")
                .build();
        EntityNode requesterNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("requesterid")
                .name("requesterid")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation(
                "users.data.root@common.contoso.com", "common")).thenReturn(groupNode);
        EntityNode rootDataGroupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("users.data.root@common.contoso.com")
                .name("users.data.root")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation(
                "users.data.root@common.contoso.com", "common")).thenReturn(rootDataGroupNode);
        when(retrieveGroupRepo.hasDirectChild(
                rootDataGroupNode,
                ChildrenReference.createChildrenReference(requesterNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(retrieveGroupRepo.hasDirectChild(
                groupNode,
                ChildrenReference.createChildrenReference(memberNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(requestInfoUtilService.getDomain("common")).thenReturn("common.contoso.com");

        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .groupEmail("users.data.root@common.contoso.com")
                .memberEmail("datafier@evd-ddl-us-common.iam.gserviceaccount.com")
                .requesterId("requesterid")
                .partitionId("common")
                .build();

        when(serviceAccountsConfigurationService.isMemberProtectedServiceAccount(memberNode, groupNode)).thenReturn(true);
        try {
            removeMemberService.removeMember(removeMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            verify(removeMemberRepo, never()).removeMember(any(), any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(400);
            verify(publisher, times(0)).publish(any(), any());
        } catch (Exception ex) {
            fail(String.format("should now throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldThrow400IfDeleteUsersdatarootGroupFromAnyDataGroup() {
        EntityNode memberNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("users.data.root@common.contoso.com")
                .name("users.data.root")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.getMemberNodeForRemovalFromGroup(
                "users.data.root@common.contoso.com", "common")).thenReturn(memberNode);
        EntityNode groupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("data.test@common.contoso.com")
                .name("data.test")
                .dataPartitionId("common")
                .build();
        EntityNode requesterNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("requesterid")
                .name("requesterid")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation(
                "data.test@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.getEntityNode("requesterid", "common")).thenReturn(Optional.of(requesterNode));
        EntityNode rootDataGroupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("users.data.root@common.contoso.com")
                .name("users.data.root")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation(
                "users.data.root@common.contoso.com", "common")).thenReturn(rootDataGroupNode);
        when(retrieveGroupRepo.hasDirectChild(
                rootDataGroupNode,
                ChildrenReference.createChildrenReference(requesterNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(retrieveGroupRepo.hasDirectChild(
                groupNode,
                ChildrenReference.createChildrenReference(memberNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(requestInfoUtilService.getDomain("common")).thenReturn("common.contoso.com");

        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .groupEmail("data.test@common.contoso.com")
                .memberEmail("users.data.root@common.contoso.com")
                .requesterId("requesterid")
                .partitionId("common")
                .build();

        when(serviceAccountsConfigurationService.isMemberProtectedServiceAccount(memberNode, groupNode)).thenReturn(false);
        try {
            removeMemberService.removeMember(removeMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            verify(removeMemberRepo, never()).removeMember(any(), any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(400);
            assertEquals("Users data root group hierarchy is enforced, member users.data.root cannot be removed", ex.getError().getMessage());
            verify(publisher, times(0)).publish(any(), any());
        } catch (Exception ex) {
            fail(String.format("should now throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldSuccessfullyRemoveMember_AndNotPublishRemoveMemberEntitlementsChangeEvent_WhenPublishingDisabled() {
        ReflectionTestUtil.setFieldValueForClass(removeMemberService, "eventPublishingEnabled", false);
        EntityNode memberNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("member@xxx.com")
                .name("member")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.getMemberNodeForRemovalFromGroup("member@xxx.com", "common")).thenReturn(memberNode);
        EntityNode groupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("data.x@common.contoso.com")
                .name("data.x")
                .dataPartitionId("common")
                .build();
        EntityNode requesterNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("requesterid")
                .name("requesterid")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        EntityNode rootDataGroupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("users.data.root@common.contoso.com")
                .name("users.data.root")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation(
                "users.data.root@common.contoso.com", "common")).thenReturn(rootDataGroupNode);
        when(retrieveGroupRepo.hasDirectChild(
                rootDataGroupNode,
                ChildrenReference.createChildrenReference(requesterNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(retrieveGroupRepo.hasDirectChild(
                groupNode,
                ChildrenReference.createChildrenReference(memberNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(requestInfoUtilService.getDomain("common")).thenReturn("common.contoso.com");
        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .groupEmail("data.x@common.contoso.com")
                .memberEmail("member@xxx.com")
                .requesterId("requesterid")
                .partitionId("common")
                .build();
        when(removeMemberRepo.removeMember(any(), any(), any())).thenReturn(Collections.emptySet());

        removeMemberService.removeMember(removeMemberServiceDto);

        verify(removeMemberRepo).removeMember(groupNode, memberNode, removeMemberServiceDto);
        verify(groupCacheService).refreshListGroupCache(Collections.emptySet(), "common");
        verifyNoInteractions(publisher);
    }
}
