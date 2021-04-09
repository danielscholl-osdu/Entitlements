package org.opengroup.osdu.entitlements.v2.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.configuration.AppPropertiesTestConfiguration;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.listmember.ListMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
@Import(AppPropertiesTestConfiguration.class)
public class ListMemberServiceTests {

    @MockBean
    private RetrieveGroupRepo retrieveGroupRepo;
    @MockBean
    private ListMemberRepo listMemberRepo;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private AppProperties appProperties;
    @MockBean
    private AuthorizationService authorizationService;

    @Autowired
    private ListMemberService listMemberService;

    @Before
    public void setup() {
        when(requestInfo.getTenantInfo()).thenReturn(new TenantInfo());
    }

    @Test
    public void shouldCallListMemberRepoAndReturnTheResults() {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId("dp").build();
        when(retrieveGroupRepo.groupExistenceValidation("data.x@dp.domain.com", "dp")).thenReturn(groupNode);
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("dp").build();
        when(retrieveGroupRepo.getEntityNode("requesterid", "dp")).thenReturn(Optional.of(requesterNode));
        when(retrieveGroupRepo.groupExistenceValidation("data.x@dp.domain.com", "dp")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.TRUE);
        List<ChildrenReference> members = Arrays.asList(
                ChildrenReference.builder().id("shadowid1").dataPartitionId("dp").type(NodeType.USER).role(Role.MEMBER).build(),
                ChildrenReference.builder().id("shadowid2").dataPartitionId("dp").type(NodeType.USER).role(Role.MEMBER).build()
        );
        ListMemberServiceDto listMemberServiceDto = ListMemberServiceDto.builder()
                .groupId("data.x@dp.domain.com")
                .requesterId("requesterid")
                .partitionId("dp").build();
        when(listMemberRepo.run(listMemberServiceDto)).thenReturn(members);

        List<ChildrenReference> retMembers = listMemberService.run(listMemberServiceDto);

        verify(listMemberRepo).run(listMemberServiceDto);
        assertEquals(2, retMembers.size());
        List<String> allReturnIds = retMembers.stream().map(ChildrenReference::getId).collect(Collectors.toList());
        assertTrue(allReturnIds.contains("shadowid1"));
        assertTrue(allReturnIds.contains("shadowid2"));
    }

    @Test
    public void shouldThrow401IfCallerDoesNotOwnTheGroup() {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId("dp").build();
        when(retrieveGroupRepo.groupExistenceValidation("data.x@dp.domain.com", "dp")).thenReturn(groupNode);
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("dp").build();
        when(retrieveGroupRepo.getEntityNode("requesterid", "dp")).thenReturn(Optional.of(requesterNode));
        when(retrieveGroupRepo.groupExistenceValidation("data.y@dp.domain.com", "dp")).thenReturn(groupNode);
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.FALSE);
        when(authorizationService.isAuthorized(any(), eq(AppProperties.ADMIN))).thenThrow(AppException.createUnauthorized(""));
        ListMemberServiceDto listMemberServiceDto = ListMemberServiceDto.builder()
                .groupId("data.x@dp.domain.com")
                .requesterId("requesterid")
                .partitionId("dp").build();
        try {
            listMemberService.run(listMemberServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            assertThat(ex.getError().getCode()).isEqualTo(401);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldReturnResultsIfCallerDoesNotBelongToTheGroupAndCallerIsAdmin() {
        String requesterId = "requesterid";
        String partition = "dp";
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId(partition).build();
        when(retrieveGroupRepo.groupExistenceValidation("data.x@dp.domain.com", partition)).thenReturn(groupNode);
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("dp").build();
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.FALSE);
        when(authorizationService.isAuthorized(any(), eq(AppProperties.ADMIN))).thenReturn(true);
        ListMemberServiceDto listMemberServiceDto = ListMemberServiceDto.builder()
                .groupId("data.x@dp.domain.com")
                .requesterId(requesterId)
                .partitionId(partition).build();
        try {
            listMemberService.run(listMemberServiceDto);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }
}
