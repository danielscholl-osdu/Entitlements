package org.opengroup.osdu.entitlements.v2.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GroupCacheServiceTests {

    private Set<ParentReference> parents = new HashSet<>();
    private EntityNode requester;

    @Mock
    private RetrieveGroupRepo retrieveGroupRepo;
    @Mock
    private GroupCache vmGroupCache;
    @Mock
    private ParentTreeDto parentTreeDto;

    @InjectMocks
    private GroupCacheService sut;

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

        requester = EntityNode.createMemberNodeForNewUser("requesterId", "dp");
    }

    @Test
    public void shouldGetAllParentsFromRepoForTheFirstTime() {
        when(this.vmGroupCache.getGroupCache("requesterId-dp")).thenReturn(null);
        when(this.retrieveGroupRepo.loadAllParents(this.requester)).thenReturn(this.parentTreeDto);
        when(this.parentTreeDto.getParentReferences()).thenReturn(this.parents);

        Set<ParentReference> result = this.sut.getFromPartitionCache("requesterId", "dp");
        assertEquals(this.parents, result);
        verify(this.retrieveGroupRepo, times(1)).loadAllParents(this.requester);
        verify(this.vmGroupCache, times(1)).addGroupCache("requesterId-dp", this.parents);
    }

    @Test
    public void shouldGetAllParentsFromCacheForTheSecondTime() {
        when(this.vmGroupCache.getGroupCache("requesterId-dp")).thenReturn(this.parents);

        Set<ParentReference> result = this.sut.getFromPartitionCache("requesterId", "dp");
        assertEquals(this.parents, result);
        verify(this.retrieveGroupRepo, times(0)).loadAllParents(this.requester);
    }
}
