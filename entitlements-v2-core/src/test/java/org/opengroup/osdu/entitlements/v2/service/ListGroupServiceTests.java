package org.opengroup.osdu.entitlements.v2.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ListGroupServiceTests {

    @MockBean
    private TenantInfo tenantInfo;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private RetrieveGroupRepo retrieveGroupRepo;
    @MockBean
    private GroupCacheService groupCacheService;
    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private RequestInfo requestInfo;

    @Autowired
    private ListGroupService listGroupService;

    private Set<ParentReference> unfilteredParentRefs;

    @Before
    public void setup() {
        when(tenantInfo.getServiceAccount()).thenReturn("datafier@serviceaccount");
        when(requestInfo.getTenantInfo()).thenReturn(tenantInfo);
        unfilteredParentRefs = new HashSet<>();
        unfilteredParentRefs.add(ParentReference.builder().id("g1@dp.domain.com").name("g1").dataPartitionId("dp").build());
        unfilteredParentRefs.add(ParentReference.builder().id("g2@dp.domain.com").name("g2").dataPartitionId("dp").build());
        unfilteredParentRefs.add(ParentReference.builder().id("g3@dp.domain.com").name("g3").dataPartitionId("dp").build());
        unfilteredParentRefs.add(ParentReference.builder().id("g4@dp.domain.com").name("g4").dataPartitionId("dp").build());
        unfilteredParentRefs.add(ParentReference.builder().id("g5@dp1.domain.com").name("g5").dataPartitionId("dp1").build());
    }

    @Test
    public void should_getAllGroups_matchGivenPartitionIds() {
        ListGroupServiceDto listGroupServiceDto = ListGroupServiceDto
                .builder()
                .requesterId("datafier@serviceaccount")
                .partitionIds(Arrays.asList("dp", "dp1"))
                .build();

        Set<ParentReference> parents1 = unfilteredParentRefs.stream()
                .filter(p -> "dp".equals(p.getDataPartitionId()))
                .collect(Collectors.toSet());
        when(groupCacheService.getFromPartitionCache("datafier@serviceaccount", "dp")).thenReturn(parents1);

        Set<ParentReference> parents2 = unfilteredParentRefs.stream()
                .filter(p -> "dp1".equals(p.getDataPartitionId()))
                .collect(Collectors.toSet());
        when(groupCacheService.getFromPartitionCache("datafier@serviceaccount", "dp1")).thenReturn(parents2);

        Set<ParentReference> parentReferences = listGroupService.getGroups(listGroupServiceDto);

        assertEquals(5, parentReferences.size());
        verify(auditLogger).listGroup(eq(AuditStatus.SUCCESS), any());
    }

    @Test
    public void should_getAllGroupsWithoutAppIdFilter_ifCallerIsInternalServiceId() {
        List<String> partitionIds = new ArrayList<>();
        partitionIds.add("dp");

        ListGroupServiceDto listGroupServiceDto = ListGroupServiceDto
                .builder()
                .requesterId("datafier@serviceaccount")
                .partitionIds(partitionIds)
                .build();
        Set<ParentReference> parents = unfilteredParentRefs.stream()
                .filter(p -> "dp".equals(p.getDataPartitionId()))
                .collect(Collectors.toSet());
        when(groupCacheService.getFromPartitionCache("datafier@serviceaccount", "dp")).thenReturn(parents);

        Set<ParentReference> parentReferences = listGroupService.getGroups(listGroupServiceDto);

        assertEquals(4, parentReferences.size());
        verify(auditLogger).listGroup(eq(AuditStatus.SUCCESS), any());
    }

    @Test
    public void should_getAllGroupsWithoutAppIdFilter_ifGivenAppIdIsNullOrEmpty() {
        List<String> partitionIds = new ArrayList<>();
        partitionIds.add("dp");

        ListGroupServiceDto listGroupServiceDto = ListGroupServiceDto
                .builder()
                .requesterId("anycaller")
                .partitionIds(partitionIds)
                .build();

        Set<ParentReference> parents = unfilteredParentRefs.stream()
                .filter(p -> "dp".equals(p.getDataPartitionId()))
                .collect(Collectors.toSet());
        when(groupCacheService.getFromPartitionCache("anycaller", "dp")).thenReturn(parents);

        Set<ParentReference> parentReferences = listGroupService.getGroups(listGroupServiceDto);
        assertEquals(4, parentReferences.size());
        verify(auditLogger).listGroup(eq(AuditStatus.SUCCESS), any());
    }

    @Test
    public void should_filterByAppId_ifNormalCaller() {
        List<String> partitionIds = Arrays.asList("dp", "dp1");

        ListGroupServiceDto listGroupServiceDto = ListGroupServiceDto
                .builder()
                .requesterId("callerdesid")
                .appId("app1")
                .partitionIds(partitionIds)
                .build();

        EntityNode requesterNode1 = EntityNode.createMemberNodeForRequester("callerdesid", "dp");
        Set<ParentReference> parents1 = unfilteredParentRefs.stream().filter(p -> "dp".equals(p.getDataPartitionId())).collect(Collectors.toSet());
        ParentTreeDto parentTreeDto1 = ParentTreeDto.builder().parentReferences(parents1).maxDepth(2).build();
        when(retrieveGroupRepo.loadAllParents(requesterNode1)).thenReturn(parentTreeDto1);

        EntityNode requesterNode2 = EntityNode.createMemberNodeForRequester("callerdesid", "dp1");
        Set<ParentReference> parents2 = unfilteredParentRefs.stream().filter(p -> "dp1".equals(p.getDataPartitionId())).collect(Collectors.toSet());
        ParentTreeDto parentTreeDto2 = ParentTreeDto.builder().parentReferences(parents2).maxDepth(2).build();
        when(retrieveGroupRepo.loadAllParents(requesterNode2)).thenReturn(parentTreeDto2);

        Set<ParentReference> dpFilteredParentRefs = new HashSet<>();
        dpFilteredParentRefs.add(ParentReference.builder().id("g1@dp.domain.com").name("g1").dataPartitionId("dp").build());
        dpFilteredParentRefs.add(ParentReference.builder().id("g2@dp.domain.com").name("g2").dataPartitionId("dp").build());
        Set<ParentReference> dp1FilteredParentRefs = new HashSet<>();
        dp1FilteredParentRefs.add(ParentReference.builder().id("g5@dp.domain.com").name("g5").dataPartitionId("dp1").build());

        when(retrieveGroupRepo.filterParentsByAppID(any(), eq("dp"), eq(listGroupServiceDto.getAppId()))).thenReturn(dpFilteredParentRefs);
        when(retrieveGroupRepo.filterParentsByAppID(any(), eq("dp1"), eq(listGroupServiceDto.getAppId()))).thenReturn(dp1FilteredParentRefs);

        Set<ParentReference> parentReferences = listGroupService.getGroups(listGroupServiceDto);

        assertEquals(3, parentReferences.size());
        assertTrue(parentReferences.stream().anyMatch(g -> g.getId().equals("g1@dp.domain.com")));
        assertTrue(parentReferences.stream().anyMatch(g -> g.getId().equals("g2@dp.domain.com")));
        assertTrue(parentReferences.stream().anyMatch(g -> g.getId().equals("g5@dp.domain.com")));
        verify(auditLogger).listGroup(eq(AuditStatus.SUCCESS), any());
    }
}
