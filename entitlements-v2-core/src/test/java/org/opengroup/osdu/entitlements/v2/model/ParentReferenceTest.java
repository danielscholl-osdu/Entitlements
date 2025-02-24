package org.opengroup.osdu.entitlements.v2.model;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParentReferenceTest {

    @Test
    public void should_createParentReference_givenEntityNode() {
        EntityNode node = EntityNode.builder().nodeId("group@dp.domain.com").name("group").dataPartitionId("dp").description("a group").type(NodeType.GROUP).appIds(new HashSet<>(Collections.singleton("app1"))).build();
        ParentReference parentReference = ParentReference.createParentReference(node);
        assertEquals("group@dp.domain.com", parentReference.getId());
        assertEquals("group", parentReference.getName());
        assertEquals("a group", parentReference.getDescription());
        assertEquals("dp", parentReference.getDataPartitionId());
    }

    @Test
    public void should_returnTrue_ifIsRootUserGroup() {
        ParentReference parentReference = ParentReference.builder().id("users@dp.domain.com").name("users").description("a group").dataPartitionId("dp").build();
        assertTrue(parentReference.isRootUserGroup());
        assertFalse(parentReference.isDataGroup());
        assertTrue(parentReference.isUserGroup());
        assertFalse(parentReference.isServiceGroup());
    }

    @Test
    public void should_returnTrue_ifDataGroup() {
        ParentReference parentReference = ParentReference.builder().id("data.x@dp.domain.com").name("data.x").description("a group").dataPartitionId("dp").build();
        assertTrue(parentReference.isDataGroup());
        assertFalse(parentReference.isRootUserGroup());
        assertFalse(parentReference.isUserGroup());
        assertFalse(parentReference.isServiceGroup());
    }

    @Test
    public void should_returnTrue_ifUserGroup() {
        ParentReference parentReference = ParentReference.builder().id("users.x@dp.domain.com").name("users.x").description("a group").dataPartitionId("dp").build();
        assertTrue(parentReference.isUserGroup());
        assertFalse(parentReference.isRootUserGroup());
        assertFalse(parentReference.isDataGroup());
        assertFalse(parentReference.isServiceGroup());
        parentReference = ParentReference.builder().id("users.sharing_x@dp.domain.com").name("users.sharing_x").description("a group").dataPartitionId("dp").build();
        assertFalse(parentReference.isUserGroup());
        assertFalse(parentReference.isRootUserGroup());
        assertFalse(parentReference.isDataGroup());
        assertFalse(parentReference.isServiceGroup());
    }

    @Test
    public void should_returnTrue_ifServiceGroup() {
        ParentReference parentReference = ParentReference.builder().id("service.x@dp.domain.com").name("service.x").description("a group").dataPartitionId("dp").build();
        assertFalse(parentReference.isUserGroup());
        assertFalse(parentReference.isRootUserGroup());
        assertFalse(parentReference.isDataGroup());
        assertTrue(parentReference.isServiceGroup());
    }

    @Test
    public void should_returnTrue_ifMatchGroupType() {
        ParentReference parentReference = ParentReference.builder().id("data.x@dp.domain.com").name("data.x").description("a group").dataPartitionId("dp").build();
        assertTrue(parentReference.isMatchGroupType(GroupType.DATA));
        assertFalse(parentReference.isMatchGroupType(GroupType.USER));
        assertFalse(parentReference.isMatchGroupType(GroupType.SERVICE));
        parentReference = ParentReference.builder().id("users.x@dp.domain.com").name("users.x").description("a group").dataPartitionId("dp").build();
        assertFalse(parentReference.isMatchGroupType(GroupType.DATA));
        assertTrue(parentReference.isMatchGroupType(GroupType.USER));
        assertFalse(parentReference.isMatchGroupType(GroupType.SERVICE));
        parentReference = ParentReference.builder().id("service.x@dp.domain.com").name("service.x").description("a group").dataPartitionId("dp").build();
        assertFalse(parentReference.isMatchGroupType(GroupType.DATA));
        assertFalse(parentReference.isMatchGroupType(GroupType.USER));
        assertTrue(parentReference.isMatchGroupType(GroupType.SERVICE));
        parentReference = ParentReference.builder().id("users@dp.domain.com").name("users").description("a group").dataPartitionId("dp").build();
        assertFalse(parentReference.isMatchGroupType(GroupType.DATA));
        assertTrue(parentReference.isMatchGroupType(GroupType.USER));
        assertFalse(parentReference.isMatchGroupType(GroupType.SERVICE));
    }
}
