package org.opengroup.osdu.entitlements.v2.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberDto;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class AddMemberServiceTests {

    @MockBean
    private AppProperties config;
    @MockBean
    private RetrieveGroupRepo retrieveGroupRepo;
    @MockBean
    private AddMemberRepo addMemberRepo;
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private RequestInfo requestInfo;

    @Autowired
    private AddMemberService addMemberService;

    @Before
    public void setup() {
        when(config.getDomain()).thenReturn("contoso.com");
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setServiceAccount("datafier@test.com");
        when(requestInfo.getTenantInfo()).thenReturn(tenantInfo);
    }

    @Test
    public void should_createUserMemberNode_ifItDoesNotExist() {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@common.contoso.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("member@contoso.com", "common")).thenReturn(Optional.empty());
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.getEntityNode("requesterid", "common")).thenReturn(Optional.of(requesterNode));
        EntityNode rootDataGroupNode = EntityNode.builder().nodeId("users.data.root@common.contoso.com").name("users.data.root")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        when(retrieveGroupRepo.groupExistenceValidation("users.data.root@common.contoso.com", "common")).thenReturn(rootDataGroupNode);
        when(retrieveGroupRepo.hasDirectChild(rootDataGroupNode, ChildrenReference.createChildrenReference(requesterNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        EntityNode memberNode = EntityNode.builder().nodeId("member@contoso.com").name("member@contoso.com").type(NodeType.USER)
                .dataPartitionId("common").description("member@contoso.com").appIds(Collections.emptySet()).build();
        ParentTreeDto parentTreeDto = ParentTreeDto.builder().parentReferences(Collections.emptySet()).maxDepth(2).build();
        when(retrieveGroupRepo.loadAllParents(memberNode)).thenReturn(parentTreeDto);

        AddMemberDto addMemberDto = new AddMemberDto("member@contoso.com", Role.MEMBER);
        AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                .groupEmail("data.x@common.contoso.com")
                .requesterId("requesterid")
                .partitionId("common")
                .build();

        addMemberService.run(addMemberDto, addMemberServiceDto);

        ArgumentCaptor<AddMemberRepoDto> captor = ArgumentCaptor.forClass(AddMemberRepoDto.class);
        verify(addMemberRepo).addMember(eq(groupNode), captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.MEMBER);
        assertThat(captor.getValue().getPartitionId()).isEqualTo("common");
    }

    @Test
    public void shouldCreateUserMemberNodeIfItDoesNotExistWhenRequesterIsInternalServiceOwner() {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@common.contoso.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode requesterNode = EntityNode.builder().nodeId("datafier@test.com").name("datafier").type(NodeType.USER).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("member@xxx.com", "common")).thenReturn(Optional.empty());
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.getEntityNode("datafier@test.com", "common")).thenReturn(Optional.of(requesterNode));
        EntityNode rootDataGroupNode = EntityNode.builder().nodeId("users.data.root@common.contoso.com").name("users.data.root")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        when(retrieveGroupRepo.groupExistenceValidation("users.data.root@common.contoso.com", "common")).thenReturn(rootDataGroupNode);
        EntityNode memberNode = EntityNode.builder().nodeId("member@xxx.com").name("member@xxx.com").type(NodeType.USER)
                .dataPartitionId("common").description("member@xxx.com").appIds(Collections.emptySet()).build();
        ParentTreeDto parentTreeDto = ParentTreeDto.builder().parentReferences(Collections.emptySet()).maxDepth(2).build();
        when(retrieveGroupRepo.loadAllParents(memberNode)).thenReturn(parentTreeDto);

        AddMemberDto addMemberDto = new AddMemberDto("member@xxx.com", Role.MEMBER);
        AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                .groupEmail("data.x@common.contoso.com")
                .requesterId("datafier@test.com")
                .partitionId("common")
                .build();

        addMemberService.run(addMemberDto, addMemberServiceDto);

        ArgumentCaptor<AddMemberRepoDto> captor = ArgumentCaptor.forClass(AddMemberRepoDto.class);
        verify(addMemberRepo).addMember(eq(groupNode), captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.MEMBER);
        assertThat(captor.getValue().getPartitionId()).isEqualTo("common");
    }

    @Test
    public void should_throw404_ifMemberIsAGroup_andItDoesNotExist() {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@common.contoso.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("users.x@common.contoso.com", "common")).thenReturn(Optional.empty());
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.getEntityNode("requesterid", "common")).thenReturn(Optional.of(requesterNode));
        when(retrieveGroupRepo.groupExistenceValidation("users.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.TRUE);

        try {
            AddMemberDto addMemberDto = new AddMemberDto("users.x@common.contoso.com", Role.MEMBER);
            AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                    .groupEmail("data.x@common.contoso.com")
                    .requesterId("requesterid")
                    .partitionId("common")
                    .build();

            addMemberService.run(addMemberDto, addMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            verify(addMemberRepo, never()).addMember(any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(404);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void should_passExistingMemberNode_ifItExists() {
        EntityNode memberNode = EntityNode.builder().nodeId("member@xxx.com").name("member@xxx.com").description("member@xxx.com").type(NodeType.USER).dataPartitionId("common").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@common.contoso.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("member@xxx.com", "common")).thenReturn(Optional.of(memberNode));
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.getEntityNode("requesterid", "common")).thenReturn(Optional.of(requesterNode));
        EntityNode rootDataGroupNode = EntityNode.builder().nodeId("users.data.root@common.contoso.com").name("users.data.root")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        when(retrieveGroupRepo.groupExistenceValidation("users.data.root@common.contoso.com", "common")).thenReturn(rootDataGroupNode);
        when(retrieveGroupRepo.hasDirectChild(rootDataGroupNode, ChildrenReference.createChildrenReference(requesterNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        ParentTreeDto parentTreeDto = ParentTreeDto.builder().parentReferences(Collections.emptySet()).maxDepth(2).build();
        when(retrieveGroupRepo.loadAllParents(memberNode)).thenReturn(parentTreeDto);

        AddMemberDto addMemberDto = new AddMemberDto("member@xxx.com", Role.MEMBER);
        AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                .groupEmail("data.x@common.contoso.com")
                .requesterId("requesterid")
                .partitionId("common")
                .build();

        addMemberService.run(addMemberDto, addMemberServiceDto);

        ArgumentCaptor<AddMemberRepoDto> captor = ArgumentCaptor.forClass(AddMemberRepoDto.class);
        verify(addMemberRepo).addMember(eq(groupNode), captor.capture());
        assertThat(captor.getValue().getMemberNode()).isEqualTo(memberNode);
        assertThat(captor.getValue().getRole()).isEqualTo(Role.MEMBER);
        assertThat(captor.getValue().getPartitionId()).isEqualTo("common");
    }

    @Test
    public void should_throw401_ifCallerDoesNotOwnTheGroup() {
        EntityNode entityNode = EntityNode.builder().nodeId("member@xxx.com").name("member@xxx.com").type(NodeType.USER).dataPartitionId("common").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@common.contoso.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("member@xxx.com", "common")).thenReturn(Optional.of(entityNode));
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.getEntityNode("requesterid", "common")).thenReturn(Optional.of(requesterNode));
        when(retrieveGroupRepo.groupExistenceValidation("users.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.FALSE);

        try {
            AddMemberDto addMemberDto = new AddMemberDto("member@xxx.com", Role.MEMBER);
            AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                    .groupEmail("data.x@common.contoso.com")
                    .requesterId("requesterid")
                    .partitionId("common")
                    .build();

            addMemberService.run(addMemberDto, addMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            verify(addMemberRepo, never()).addMember(any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(401);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void should_throw409_ifAlreadyAMember() {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@common.contoso.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("member@xxx.com", "common")).thenReturn(Optional.empty());
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.getEntityNode("requesterid", "common")).thenReturn(Optional.of(requesterNode));
        EntityNode rootDataGroupNode = EntityNode.builder().nodeId("users.data.root@common.contoso.com").name("users.data.root")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        when(retrieveGroupRepo.groupExistenceValidation("users.data.root@common.contoso.com", "common")).thenReturn(rootDataGroupNode);
        when(retrieveGroupRepo.hasDirectChild(rootDataGroupNode, ChildrenReference.createChildrenReference(requesterNode, Role.MEMBER))).thenReturn(Boolean.TRUE);
        when(retrieveGroupRepo.hasDirectChild(eq(groupNode), any())).thenReturn(true);

        try {
            AddMemberDto addMemberDto = new AddMemberDto("member@xxx.com", Role.MEMBER);
            AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                    .groupEmail("data.x@common.contoso.com")
                    .requesterId("requesterid")
                    .partitionId("common")
                    .build();

            addMemberService.run(addMemberDto, addMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            verify(addMemberRepo, never()).addMember(any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(409);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void should_return400_ifAddGroupToAnotherGroupAsOwner() {
        HashSet<ParentReference> parents = mock(HashSet.class);
        when(parents.size()).thenReturn(5000);
        EntityNode memberNode = EntityNode.builder().nodeId("users.x@common.contoso.com").name("users.x")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@common.contoso.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("users.x@common.contoso.com", "common")).thenReturn(Optional.of(memberNode));
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.getEntityNode("requesterid", "common")).thenReturn(Optional.of(requesterNode));
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.TRUE);
        when(retrieveGroupRepo.loadAllParents(memberNode)).thenReturn(ParentTreeDto.builder().parentReferences(parents).maxDepth(2).build());

        try {
            AddMemberDto addMemberDto = new AddMemberDto("users.x@common.contoso.com", Role.OWNER);
            AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                    .groupEmail("data.x@common.contoso.com")
                    .requesterId("requesterid")
                    .partitionId("common")
                    .build();

            addMemberService.run(addMemberDto, addMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            verify(addMemberRepo, never()).addMember(any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(400);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void should_throw412_ifQuotaHit_whenAddUser() {
        HashSet<ParentReference> parents = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            parents.add(ParentReference.builder().id(String.valueOf(i)).dataPartitionId("common").build());
        }
        EntityNode memberNode = EntityNode.builder().nodeId("memberid@xxx.com").name("memberid@xxx.com")
                .type(NodeType.USER).dataPartitionId("common").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@common.contoso.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("common").build();
        when(retrieveGroupRepo.getEntityNode("memberid@xxx.com", "common")).thenReturn(Optional.of(memberNode));
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.getEntityNode("requesterid", "common")).thenReturn(Optional.of(requesterNode));
        when(retrieveGroupRepo.groupExistenceValidation("users.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.TRUE);
        ParentTreeDto parentTreeDto = ParentTreeDto.builder().parentReferences(parents).maxDepth(2).build();
        when(retrieveGroupRepo.loadAllParents(memberNode)).thenReturn(parentTreeDto);
        try {
            AddMemberDto addMemberDto = new AddMemberDto("memberid@xxx.com", Role.MEMBER);
            AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                    .groupEmail("data.x@common.contoso.com")
                    .requesterId("requesterid")
                    .partitionId("common")
                    .build();

            addMemberService.run(addMemberDto, addMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            verify(addMemberRepo, never()).addMember(any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(412);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void should_throw412_ifQuotaHit_whenAddGroup() {
        HashSet<ParentReference> parents = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            parents.add(ParentReference.builder().id(String.valueOf(i)).dataPartitionId("common").build());
        }
        EntityNode memberNode = EntityNode.builder().nodeId("users.x@common.contoso.com").name("users.x")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@common.contoso.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("common").build();
        when(config.getDomain()).thenReturn("contoso.com");
        when(retrieveGroupRepo.getEntityNode("users.x@common.contoso.com", "common")).thenReturn(Optional.of(memberNode));
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.getEntityNode("requesterid", "common")).thenReturn(Optional.of(requesterNode));
        when(retrieveGroupRepo.groupExistenceValidation("users.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.TRUE);
        when(retrieveGroupRepo.loadAllParents(memberNode)).thenReturn(ParentTreeDto.builder().parentReferences(parents).maxDepth(2).build());

        try {
            AddMemberDto addMemberDto = new AddMemberDto("users.x@common.contoso.com", Role.MEMBER);
            AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                    .groupEmail("data.x@common.contoso.com")
                    .requesterId("requesterid")
                    .partitionId("common")
                    .build();

            addMemberService.run(addMemberDto, addMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            verify(addMemberRepo, never()).addMember(any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(412);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void should_throw400_ifAddItself() {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@common.contoso.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("common").build();
        when(retrieveGroupRepo.groupExistenceValidation("data.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.getEntityNode("data.x@common.contoso.com", "common")).thenReturn(Optional.of(groupNode));
        when(retrieveGroupRepo.getEntityNode("requesterid", "common")).thenReturn(Optional.of(requesterNode));
        when(retrieveGroupRepo.groupExistenceValidation("users.x@common.contoso.com", "common")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.TRUE);
        when(retrieveGroupRepo.loadAllParents(groupNode)).thenReturn(ParentTreeDto.builder().parentReferences(new HashSet<>()).maxDepth(1).build());

        try {
            AddMemberDto addMemberDto = new AddMemberDto("data.x@common.contoso.com", Role.MEMBER);
            AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                    .groupEmail("data.x@common.contoso.com")
                    .requesterId("requesterid")
                    .partitionId("common")
                    .build();

            addMemberService.run(addMemberDto, addMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            verify(addMemberRepo, never()).addMember(any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(400);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }
}
