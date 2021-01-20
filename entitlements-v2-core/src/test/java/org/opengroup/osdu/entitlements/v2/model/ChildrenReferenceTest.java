package org.opengroup.osdu.entitlements.v2.model;

import org.opengroup.osdu.entitlements.v2.util.JsonConverter;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChildrenReferenceTest {

    @Test
    public void shouldCreateChildrenReferenceGivenEntityNode() {
        EntityNode node = EntityNode.builder().nodeId("group@dp.domain.com").name("group").dataPartitionId("dp").description("a group").type(NodeType.GROUP).appIds(new HashSet<>(Collections.singleton("app1"))).build();
        ChildrenReference childrenReference = ChildrenReference.createChildrenReference(node, Role.MEMBER);
        assertEquals("group@dp.domain.com", childrenReference.getId());
        assertEquals(NodeType.GROUP, childrenReference.getType());
        assertEquals(Role.MEMBER, childrenReference.getRole());
        assertEquals("dp", childrenReference.getDataPartitionId());

    }

    @Test
    public void shouldReturnTrueIfIsGroup() {
        ChildrenReference childrenReference = ChildrenReference.builder().id("data.x@dp.domain.com").type(NodeType.GROUP).role(Role.MEMBER).dataPartitionId("dp").build();
        assertTrue(childrenReference.isGroup());
        assertFalse(childrenReference.isUser());
    }

    @Test
    public void shouldReturnTrueIfIsUser() {
        ChildrenReference childrenReference = ChildrenReference.builder().id("member@xxx.com").type(NodeType.USER).role(Role.MEMBER).dataPartitionId("dp").build();
        assertTrue(childrenReference.isUser());
        assertFalse(childrenReference.isGroup());
    }

    @Test
    public void shouldReturnTrueIfIsOwner() {
        ChildrenReference childrenReference = ChildrenReference.builder().id("member@xxx.com").type(NodeType.USER).role(Role.OWNER).dataPartitionId("dp").build();
        assertTrue(childrenReference.isOwner());
    }

    @Test
    public void shouldReturnTrueIfRootDataGroup() {
        ChildrenReference childrenReference = ChildrenReference.builder().id("users.data.root@dp.domain.com").type(NodeType.USER).role(Role.MEMBER).dataPartitionId("dp").build();
        assertTrue(childrenReference.isUsersDataRootGroup());
    }

    @Test
    public void shouldFollowAttributesOrder() {
        EntityNode node = EntityNode.builder().nodeId("group@dp.domain.com").name("group").dataPartitionId("dp").description("a group").type(NodeType.GROUP).appIds(new HashSet<>(Collections.singleton("app1"))).build();
        ChildrenReference childrenReference = ChildrenReference.createChildrenReference(node, Role.MEMBER);
        String childrenRefJson = JsonConverter.toJson(childrenReference);
        assertEquals("{\"role\":\"MEMBER\",\"id\":\"group@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}", childrenRefJson);
    }
}
