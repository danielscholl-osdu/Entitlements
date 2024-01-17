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
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountResponseDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.memberscount.MembersCountRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class MembersCountServiceTests {

    @MockBean
    private RetrieveGroupRepo retrieveGroupRepo;
    @MockBean
    private MembersCountRepo membersCountRepo;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private AppProperties appProperties;
    @MockBean
    private AuthorizationService authorizationService;

    @Autowired
    private MembersCountService membersCountService;

    @Before
    public void setup() {
        when(requestInfo.getTenantInfo()).thenReturn(new TenantInfo());
    }

    @Test
    public void shouldCallMembersCountRepoAndReturnTheResults() {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId("dp").build();
        when(retrieveGroupRepo.groupExistenceValidation("data.x@dp.domain.com", "dp")).thenReturn(groupNode);
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("dp").build();
        when(retrieveGroupRepo.getEntityNode("requesterid", "dp")).thenReturn(Optional.of(requesterNode));
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.TRUE);
        MembersCountResponseDto membersCountResponseDto = MembersCountResponseDto.builder()
                .membersCount(10).groupEmail(groupNode.getNodeId()).build();
        MembersCountServiceDto membersCountServiceDto = MembersCountServiceDto.builder()
                .groupId("data.x@dp.domain.com")
                .requesterId("requesterid")
                .partitionId("dp").build();
        when(membersCountRepo.getMembersCount(membersCountServiceDto)).thenReturn(membersCountResponseDto);

        MembersCountResponseDto actualResponse = membersCountService.getMembersCount(membersCountServiceDto);

        verify(membersCountRepo).getMembersCount(membersCountServiceDto);

        assertEquals(10, actualResponse.getMembersCount());
        assertEquals(actualResponse.getGroupEmail(), groupNode.getNodeId());
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
        when(authorizationService.isCurrentUserAuthorized(any(), eq(AppProperties.ADMIN))).thenThrow(AppException.createUnauthorized(""));
        MembersCountServiceDto membersCountServiceDto = MembersCountServiceDto.builder()
                .groupId("data.x@dp.domain.com")
                .requesterId("requesterid")
                .partitionId("dp").build();

        AppException exception = assertThrows(AppException.class, () -> {
            membersCountService.getMembersCount(membersCountServiceDto);
        });

        assertEquals(401, exception.getError().getCode());
    }

    @Test
    public void shouldReturnResultsIfCallerDoesNotBelongToTheGroupAndCallerIsAdmin() {
        String requesterid = "requesterid";
        String partition = "dp";
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x")
                .type(NodeType.GROUP).dataPartitionId(partition).build();
        when(retrieveGroupRepo.groupExistenceValidation("data.x@dp.domain.com", partition)).thenReturn(groupNode);
        EntityNode requesterNode = EntityNode.builder().nodeId("requesterid").name("requesterid").type(NodeType.USER).dataPartitionId("dp").build();
        when(retrieveGroupRepo.hasDirectChild(groupNode, ChildrenReference.createChildrenReference(requesterNode, Role.OWNER))).thenReturn(Boolean.FALSE);
        when(authorizationService.isCurrentUserAuthorized(any(), eq(AppProperties.ADMIN))).thenReturn(true);
        MembersCountServiceDto membersCountServiceDto = MembersCountServiceDto.builder()
                .groupId("data.x@dp.domain.com")
                .requesterId(requesterid)
                .partitionId(partition).build();

        MembersCountResponseDto expectedResponseDto = MembersCountResponseDto
                .builder()
                .membersCount(3)
                .groupEmail(groupNode.getNodeId())
                .build();
        when(membersCountRepo.getMembersCount(membersCountServiceDto)).thenReturn(expectedResponseDto);

        MembersCountResponseDto responseDto = membersCountService.getMembersCount(membersCountServiceDto);

        assertEquals(expectedResponseDto.getGroupEmail(), responseDto.getGroupEmail(), groupNode.getNodeId());
        assertEquals(expectedResponseDto.getMembersCount(), responseDto.getMembersCount());
    }
}