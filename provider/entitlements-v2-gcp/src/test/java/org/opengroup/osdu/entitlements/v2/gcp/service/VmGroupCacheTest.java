package org.opengroup.osdu.entitlements.v2.gcp.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;

import java.util.HashSet;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class VmGroupCacheTest {
    private Set<ParentReference> parents = new HashSet<>();

    @InjectMocks
    private VmGroupCache sut;

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
    }

    @Test
    public void shouldGetParentsAfterAddingToCache() {
        String key = "requesterId-dp";
        Set<ParentReference> firstResult = this.sut.getGroupCache(key);
        Assert.assertEquals(null, firstResult);

        this.sut.addGroupCache(key, this.parents);
        Set<ParentReference> secondResult = this.sut.getGroupCache(key);
        Assert.assertEquals(this.parents, secondResult);
    }

    @Test
    public void shouldDeleteKeyInCache() {
        String key = "requesterId-dp";
        Set<ParentReference> firstResult = this.sut.getGroupCache(key);
        Assert.assertEquals(null, firstResult);

        this.sut.addGroupCache(key, this.parents);
        Set<ParentReference> secondResult = this.sut.getGroupCache(key);
        Assert.assertEquals(this.parents, secondResult);

        this.sut.deleteGroupCache(key);
        Assert.assertNull(this.sut.getGroupCache(key));
    }
}
