package org.opengroup.osdu.entitlements.v2.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.status.IEventPublisher;
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
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeAction;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeEvent;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeType;
import org.opengroup.osdu.entitlements.v2.service.util.ReflectionTestUtil;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AddMemberService.class, System.class})
public class AddMemberServiceTests {

    @Mock
    private AppProperties config;
    @Mock
    private RetrieveGroupRepo retrieveGroupRepo;
    @Mock
    private GroupCacheService groupCacheService;
    @Mock
    private AddMemberRepo addMemberRepo;
    @Mock
    private JaxRsDpsLog log;
    @Mock
    private PermissionService permissionService;
    @Mock
    private RequestInfo requestInfo;
    @Mock
    private DpsHeaders headers;
    @Mock
    private IEventPublisher publisher;
    @InjectMocks
    private AddMemberService addMemberService;

    private static final Map<String, String> headersMap = Collections.singletonMap("testKey", "testValue");

    @Before
    public void setup() {
        PowerMockito.mockStatic(System.class);
        when(config.getDomain()).thenReturn("contoso.com");
        when(requestInfo.getHeaders()).thenReturn(headers);
        when(headers.getHeaders()).thenReturn(headersMap);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(1291371330000L);
        ReflectionTestUtil.setFieldValueForClass(addMemberService, "eventPublishingEnabled", true);
        ReflectionTestUtil.setFieldValueForClass(addMemberService, "eventPublisher", publisher);
    }

    @Test
    public void should_createUserMemberNode_andPublishEntitlementsChangeEvent_ifItDoesNotExist() {
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
        when(retrieveGroupRepo.loadAllParents(groupNode)).thenReturn(parentTreeDto);

        AddMemberDto addMemberDto = new AddMemberDto("member@contoso.com", Role.MEMBER);
        AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                .groupEmail("data.x@common.contoso.com")
                .requesterId("requesterid")
                .partitionId("common")
                .build();
        Set<String> allImpactUsers = new HashSet<>(Arrays.asList("member@xxx.com"));
        when(addMemberRepo.addMember(any(), any())).thenReturn(allImpactUsers);

        EntitlementsChangeEvent [] event = {
                EntitlementsChangeEvent.builder()
                .kind(EntitlementsChangeType.groupChanged)
                .group("data.x@common.contoso.com")
                .user("member@contoso.com")
                .action(EntitlementsChangeAction.add)
                .modifiedBy("requesterid")
                .modifiedOn(1291371330000L).build()
        };

        addMemberService.run(addMemberDto, addMemberServiceDto);

        ArgumentCaptor<AddMemberRepoDto> captor = ArgumentCaptor.forClass(AddMemberRepoDto.class);
        verify(addMemberRepo).addMember(eq(groupNode), captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.MEMBER);
        assertThat(captor.getValue().getPartitionId()).isEqualTo("common");
        verify(groupCacheService).refreshListGroupCache(allImpactUsers, "common");
        verify(publisher).publish(event, headersMap);
    }

    @Test
    public void shouldCreateUserMemberNodeAndPublishAddMemberEntitlementsChangeEventIfItDoesNotExistWhenRequesterIsInternalServiceOwner() {
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
        when(retrieveGroupRepo.loadAllParents(groupNode)).thenReturn(parentTreeDto);

        AddMemberDto addMemberDto = new AddMemberDto("member@xxx.com", Role.MEMBER);
        AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                .groupEmail("data.x@common.contoso.com")
                .requesterId("datafier@test.com")
                .partitionId("common")
                .build();
        Set<String> allImpactUsers = new HashSet<>(Arrays.asList("member@xxx.com"));
        when(addMemberRepo.addMember(any(), any())).thenReturn(allImpactUsers);

        EntitlementsChangeEvent[] event = {
                EntitlementsChangeEvent.builder()
                .kind(EntitlementsChangeType.groupChanged)
                .group("data.x@common.contoso.com")
                .user("member@xxx.com")
                .action(EntitlementsChangeAction.add)
                .modifiedBy("datafier@test.com")
                .modifiedOn(1291371330000L).build()
        };

        addMemberService.run(addMemberDto, addMemberServiceDto);

        ArgumentCaptor<AddMemberRepoDto> captor = ArgumentCaptor.forClass(AddMemberRepoDto.class);
        verify(addMemberRepo).addMember(eq(groupNode), captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.MEMBER);
        assertThat(captor.getValue().getPartitionId()).isEqualTo("common");
        verify(groupCacheService).refreshListGroupCache(allImpactUsers, "common");
        verify(publisher).publish(event, headersMap);
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
            verify(publisher, times(0)).publish(any(), any());
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void should_passExistingMemberNode_andPublishAddMemberEntitlemetnsChangeEvent_ifItExists() {
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
        when(retrieveGroupRepo.loadAllParents(groupNode)).thenReturn(parentTreeDto);

        AddMemberDto addMemberDto = new AddMemberDto("member@xxx.com", Role.MEMBER);
        AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                .groupEmail("data.x@common.contoso.com")
                .requesterId("requesterid")
                .partitionId("common")
                .build();
        Set<String> allImpactUsers = new HashSet<>(Arrays.asList("member@xxx.com"));
        when(addMemberRepo.addMember(any(), any())).thenReturn(allImpactUsers);

        EntitlementsChangeEvent[] event =  {
                EntitlementsChangeEvent.builder()
                        .kind(EntitlementsChangeType.groupChanged)
                        .group("data.x@common.contoso.com")
                        .user("member@xxx.com")
                        .action(EntitlementsChangeAction.add)
                        .modifiedBy("requesterid")
                        .modifiedOn(1291371330000L).build()
        };

        addMemberService.run(addMemberDto, addMemberServiceDto);

        ArgumentCaptor<AddMemberRepoDto> captor = ArgumentCaptor.forClass(AddMemberRepoDto.class);
        verify(addMemberRepo).addMember(eq(groupNode), captor.capture());
        assertThat(captor.getValue().getMemberNode()).isEqualTo(memberNode);
        assertThat(captor.getValue().getRole()).isEqualTo(Role.MEMBER);
        assertThat(captor.getValue().getPartitionId()).isEqualTo("common");
        verify(groupCacheService).refreshListGroupCache(allImpactUsers, "common");
        verify(publisher).publish(event, headersMap);
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
            verify(publisher, times(0)).publish(any(), any());
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
            verify(publisher, times(0)).publish(any(), any());
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
            verify(publisher, times(0)).publish(any(), any());
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
            verify(publisher, times(0)).publish(any(), any());
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
            verify(publisher, times(0)).publish(any(), any());
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void should_throw400_ifCyclicMembershipExists() {
        HashSet<ParentReference> parents = new HashSet<>();
        parents.add(ParentReference.builder().id("users.x@common.contoso.com").dataPartitionId("common").build());

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
        when(retrieveGroupRepo.loadAllParents(memberNode)).thenReturn(ParentTreeDto.builder().parentReferences(Collections.emptySet()).maxDepth(2).build());
        when(retrieveGroupRepo.loadAllParents(groupNode)).thenReturn(ParentTreeDto.builder().parentReferences(parents).maxDepth(2).build());

        AddMemberDto addMemberDto = new AddMemberDto("users.x@common.contoso.com", Role.MEMBER);
        AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                .groupEmail("data.x@common.contoso.com")
                .requesterId("requesterid")
                .partitionId("common")
                .build();

        try {
            addMemberService.run(addMemberDto, addMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            verify(addMemberRepo, never()).addMember(any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(400);
            verify(publisher, times(0)).publish(any(), any());
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void should_createUserMemberNode_andNotPublishEvent_whenPublishingDisabled() {
        ReflectionTestUtil.setFieldValueForClass(addMemberService, "eventPublishingEnabled", false);
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
        when(retrieveGroupRepo.loadAllParents(groupNode)).thenReturn(parentTreeDto);

        AddMemberDto addMemberDto = new AddMemberDto("member@contoso.com", Role.MEMBER);
        AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                .groupEmail("data.x@common.contoso.com")
                .requesterId("requesterid")
                .partitionId("common")
                .build();
        Set<String> allImpactUsers = new HashSet<>(Arrays.asList("member@xxx.com"));
        when(addMemberRepo.addMember(any(), any())).thenReturn(allImpactUsers);

        addMemberService.run(addMemberDto, addMemberServiceDto);

        ArgumentCaptor<AddMemberRepoDto> captor = ArgumentCaptor.forClass(AddMemberRepoDto.class);
        verify(addMemberRepo).addMember(eq(groupNode), captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.MEMBER);
        assertThat(captor.getValue().getPartitionId()).isEqualTo("common");
        verify(groupCacheService).refreshListGroupCache(allImpactUsers, "common");
        verifyNoInteractions(publisher);
    }
}
