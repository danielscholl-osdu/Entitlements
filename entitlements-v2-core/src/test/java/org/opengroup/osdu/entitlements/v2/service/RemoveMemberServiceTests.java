package org.opengroup.osdu.entitlements.v2.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.configuration.AppPropertiesTestConfiguration;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
@Import(AppPropertiesTestConfiguration.class)
public class RemoveMemberServiceTests {
    @MockBean
    private RetrieveGroupRepo retrieveGroupRepo;
    @MockBean
    private RemoveMemberRepo removeMemberRepo;
    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private ServiceAccountsConfigurationService serviceAccountsConfigurationService;
    @MockBean
    private BootstrapGroupsConfigurationService bootstrapGroupsConfigurationService;
    @MockBean
    private RequestInfoUtilService requestInfoUtilService;
    @MockBean
    private AppProperties appProperties;
    @Autowired
    private RemoveMemberService removeMemberService;

    @Before
    public void setup() {
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setServiceAccount("datafier@evd-ddl-us-common.iam.gserviceaccount.com");
        when(requestInfo.getTenantInfo()).thenReturn(tenantInfo);
        when(appProperties.getDomain()).thenReturn("contoso.com");
    }

    @Test
    public void shouldSuccessfullyRemoveMemberWhenMemberExists() {
        EntityNode memberNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("member@xxx.com")
                .name("member")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.getEntityNode("member@xxx.com", "common")).thenReturn(Optional.of(memberNode));
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

        removeMemberService.removeMember(removeMemberServiceDto);

        verify(removeMemberRepo).removeMember(groupNode, memberNode, removeMemberServiceDto);
    }

    @Test
    public void shouldSuccessfullyRemoveMemberWhenMemberExistsWhenDatafierIsExecutingTheRequest() {
        EntityNode memberNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("member@xxx.com")
                .name("member")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.getEntityNode("member@xxx.com", "common")).thenReturn(Optional.of(memberNode));
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

        removeMemberService.removeMember(removeMemberServiceDto);

        verify(removeMemberRepo).removeMember(groupNode, memberNode, removeMemberServiceDto);
    }

    @Test
    public void shouldThrow404IfMemberIsNotFound() {
        EntityNode memberNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("member@xxx.com")
                .name("member")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.getEntityNode("member@xxx.com", "common")).thenReturn(Optional.of(memberNode));
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
        when(retrieveGroupRepo.getEntityNode("member@xxx.com", "common")).thenReturn(Optional.of(memberNode));
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
        when(retrieveGroupRepo.getEntityNode("users@common.contoso.com", "common")).thenReturn(Optional.of(memberNode));
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
        } catch (Exception ex) {
            fail(String.format("should now throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldThrow401IfCallerDoesNotOwnTheGroup() {
        EntityNode memberNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("member@xxx.com")
                .name("member")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.getEntityNode("member@xxx.com", "common")).thenReturn(Optional.of(memberNode));
        EntityNode entityNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("member@xxx.com")
                .name("shadowid@xxx.com")
                .dataPartitionId("common")
                .build();
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
        when(retrieveGroupRepo.getEntityNode("member@xxx.com", "common")).thenReturn(Optional.of(entityNode));
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(
                groupNode,
                ChildrenReference.createChildrenReference(requesterNode, Role.MEMBER))).thenReturn(Boolean.FALSE);
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
            assertThat(ex.getError().getCode()).isEqualTo(401);
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
        when(retrieveGroupRepo.getEntityNode(
                "datafier@evd-ddl-us-common.iam.gserviceaccount.com", "common")).thenReturn(Optional.of(memberNode));
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
        } catch (Exception ex) {
            fail(String.format("should now throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldThrow400IfDeleteUsersdatarootGroupFromAnyDataOrUsersGroup() {
        EntityNode memberNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("users.data.root@common.contoso.com")
                .name("users.data.root")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.getEntityNode(
                "users.data.root@common.contoso.com", "common")).thenReturn(Optional.of(memberNode));
        EntityNode groupNode = EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId("users.test@common.contoso.com")
                .name("users.test")
                .dataPartitionId("common")
                .build();
        EntityNode requesterNode = EntityNode.builder()
                .type(NodeType.USER)
                .nodeId("requesterid")
                .name("requesterid")
                .dataPartitionId("common")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation(
                "users.test@common.contoso.com", "common")).thenReturn(groupNode);
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
                .groupEmail("users.test@common.contoso.com")
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
        } catch (Exception ex) {
            fail(String.format("should now throw exception: %s", ex.getMessage()));
        }
    }
}
