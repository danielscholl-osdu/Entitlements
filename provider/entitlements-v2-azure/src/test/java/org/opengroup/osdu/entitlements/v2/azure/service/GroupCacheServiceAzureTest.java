package org.opengroup.osdu.entitlements.v2.azure.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses.HitsNMissesMetricService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GroupCacheServiceAzureTest {

    private Set<ParentReference> parents = new HashSet<>();
    private ParentReferences parentReferences = new ParentReferences();
    private EntityNode requester;

    @Mock
    private RetrieveGroupRepo retrieveGroupRepo;
    @Mock
    private ICache<String, ParentReferences> redisGroupCache;
    @Mock
    private ParentTreeDto parentTreeDto;
    @Mock
    private HitsNMissesMetricService metricService;

    @InjectMocks
    private GroupCacheServiceAzure sut;

    @Before
    public void setup() {
        ParentReference parent1 = ParentReference.builder()
                .name("parent1")
                .id("id1")
                .description("test description")
                .dataPartitionId("dp")
                .build();
        ParentReference parent2 = ParentReference.builder()
                .name("parent2")
                .id("id2")
                .description("test description")
                .dataPartitionId("dp")
                .build();
        parents.add(parent1);
        parents.add(parent2);
        parentReferences.setParentReferencesOfUser(parents);

        requester = EntityNode.createMemberNodeForNewUser("requesterId", "dp");
    }

    @Test
    public void shouldGetAllParentsFromRepoForTheFirstTime() {
        when(this.redisGroupCache.get("requesterId-dp")).thenReturn(null);
        when(this.retrieveGroupRepo.loadAllParents(this.requester)).thenReturn(this.parentTreeDto);
        when(this.parentTreeDto.getParentReferences()).thenReturn(this.parents);

        Set<ParentReference> result = this.sut.getFromPartitionCache("requesterId", "dp");
        assertEquals(this.parents, result);
        verify(this.retrieveGroupRepo, times(1)).loadAllParents(this.requester);
        verify(this.redisGroupCache, times(1)).put("requesterId-dp", this.parentReferences);
        verify(this.metricService, times(1)).sendMissesMetric();
    }

    @Test
    public void shouldGetAllParentsFromCacheForTheSecondTime() {
        when(this.redisGroupCache.get("requesterId-dp")).thenReturn(this.parentReferences);

        Set<ParentReference> result = this.sut.getFromPartitionCache("requesterId", "dp");
        assertEquals(this.parents, result);
        verify(this.retrieveGroupRepo, times(0)).loadAllParents(this.requester);
        verify(this.metricService, times(1)).sendHitsMetric();
    }
}
