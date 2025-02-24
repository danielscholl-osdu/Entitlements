package org.opengroup.osdu.entitlements.v2.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class EntityNodeTest {

    @Test
    public void should_returnUniqueIdentifier() {
        EntityNode sut = EntityNode.builder().nodeId("member@xxx.com").name("member@xxx.com").type(NodeType.USER).dataPartitionId("dp").build();

        assertEquals("member@xxx.com-dp", sut.getUniqueIdentifier());
    }

    @Test
    public void should_createRequesterNode_withUserType() {
        assertThat(EntityNode.createMemberNodeForRequester("callerdesid", "dp").getType()).isEqualTo(NodeType.USER);
        assertThat(EntityNode.createMemberNodeForRequester("callerdesid", "dp").getNodeId()).isEqualTo("callerdesid");
    }

    @Test
    public void should_createNodeFromGroupEmail() {
        EntityNode node = EntityNode.createNodeFromGroupEmail("users.sharing_12345@dp.domain.com");

        assertThat(node.getNodeId()).isEqualTo("users.sharing_12345@dp.domain.com");
        assertThat(node.getName()).isEqualTo("users.sharing_12345");
        assertThat(node.getDataPartitionId()).isEqualTo("dp");
        assertThat(node.getType()).isEqualTo(NodeType.GROUP);
    }

    @Test
    public void should_createNodeFromParentReference() {
        ParentReference ref = ParentReference.builder().id("group@dp.domain.com").name("group").description("a group").dataPartitionId("dp").build();
        EntityNode node = EntityNode.createNodeFromParentReference(ref);

        assertThat(node.getNodeId()).isEqualTo("group@dp.domain.com");
        assertThat(node.getName()).isEqualTo("group");
        assertThat(node.getDescription()).isEqualTo("a group");
        assertThat(node.getDataPartitionId()).isEqualTo("dp");
    }

    @Test
    public void should_convertToMemberNodeList_givenMemberNodeJsonList() {
        List<String> memberNodeJsonList = Arrays.asList(
                "{\"nodeId\":\"g1@domain.com\",\"type\":\"GROUP\",\"appIds\":[\"app1\"]}",
                "{\"nodeId\":\"g1@domain.com\",\"type\":\"GROUP\",\"appIds\":[]}",
                "{\"nodeId\":\"g2@domain.com\",\"type\":\"GROUP\",\"appIds\":[\"app1\",\"app2\"]}",
                "{\"nodeId\":\"g3@domain.com\",\"type\":\"GROUP\",\"appIds\":[]}"
        );

        assertThat(EntityNode.convertMemberNodeListFromListOfJson(memberNodeJsonList).size()).isEqualTo(4);
    }

    @Test
    public void should_returnTrue_whenIsGroup() {
        EntityNode sut = EntityNode.builder().nodeId("g1@dp.domain.com").name("g1").type(NodeType.GROUP).dataPartitionId("dp").build();

        assertThat(sut.isGroup()).isTrue();
        assertThat(sut.isUser()).isFalse();
    }

    @Test
    public void should_returnFalse_whenIsUser() {
        EntityNode sut = EntityNode.builder().nodeId("member@xxx.com").name("member@xxx.com").type(NodeType.USER).dataPartitionId("dp").build();

        assertThat(sut.isGroup()).isFalse();
        assertThat(sut.isUser()).isTrue();
    }

    @Test
    public void should_returnTrue_whenIsDataGroup() {
        EntityNode sut = EntityNode.builder().nodeId("data.x@dp.domain.com").type(NodeType.GROUP).name("data.x").dataPartitionId("dp").build();

        assertThat(sut.isDataGroup()).isTrue();
        assertThat(sut.isUserGroup()).isFalse();
        assertThat(sut.isServiceGroup()).isFalse();
    }

    @Test
    public void should_returnFalse_whenDataIsNotFollowedByPeriodGroup() {
        EntityNode sut = EntityNode.builder().nodeId("datax@dp.domain.com").type(NodeType.GROUP).name("datax").dataPartitionId("dp").build();

        assertThat(sut.isDataGroup()).isFalse();
    }

    @Test
    public void should_returnTrue_whenIsUsersGroup() {
        EntityNode sut = EntityNode.builder().nodeId("users.x@dp.domain.com").type(NodeType.GROUP).name("users.x").dataPartitionId("dp").build();

        assertThat(sut.isDataGroup()).isFalse();
        assertThat(sut.isUserGroup()).isTrue();
        assertThat(sut.isServiceGroup()).isFalse();

        sut = EntityNode.builder().nodeId("user.x@dp.domain.com").type(NodeType.GROUP).name("user.x").dataPartitionId("dp").build();

        assertThat(sut.isDataGroup()).isFalse();
        assertThat(sut.isUserGroup()).isTrue();
        assertThat(sut.isServiceGroup()).isFalse();

        sut = EntityNode.builder().nodeId("users.x@dp.domain.com").type(NodeType.GROUP).name("users.x").dataPartitionId("dp").build();

        assertThat(sut.isDataGroup()).isFalse();
        assertThat(sut.isUserGroup()).isTrue();
        assertThat(sut.isServiceGroup()).isFalse();

        sut = EntityNode.builder().nodeId("users@dp.domain.com").type(NodeType.GROUP).name("users").dataPartitionId("dp").build();

        assertThat(sut.isDataGroup()).isFalse();
        assertThat(sut.isUserGroup()).isTrue();
        assertThat(sut.isServiceGroup()).isFalse();
    }

    @Test
    public void should_returnFalse_whenIsNotUserGroup() {
        EntityNode sut = EntityNode.builder().nodeId("users.sharing_12345@dp.domain.com").type(NodeType.GROUP).name("users.sharing_12345").dataPartitionId("dp").build();

        assertThat(sut.isDataGroup()).isFalse();
        assertThat(sut.isUserGroup()).isFalse();
        assertThat(sut.isServiceGroup()).isFalse();
    }

    @Test
    public void should_returnTrue_whenIsOtherGroup() {
        EntityNode sut = EntityNode.builder().nodeId("fd.viewers@dp.domain.com").type(NodeType.GROUP).name("fd.viewers").dataPartitionId("dp").build();

        assertThat(sut.isOtherGroup()).isTrue();
        assertThat(sut.isDataGroup()).isFalse();
        assertThat(sut.isUserGroup()).isFalse();
        assertThat(sut.isServiceGroup()).isFalse();
    }

    @Test
    public void should_returnTrue_ifItIsAllowedGroupForCrossPartition() {
        EntityNode sut = EntityNode.builder().nodeId("data.xx.viewers@dp.domain.com").type(NodeType.GROUP).name("data.xx.viewers").dataPartitionId("dp").build();
        assertThat(sut.crossPartitionAllowedGroup()).isTrue();
        sut = EntityNode.builder().nodeId("users@dp.domain.com").type(NodeType.GROUP).name("users").dataPartitionId("dp").build();
        assertThat(sut.crossPartitionAllowedGroup()).isTrue();
        sut = EntityNode.builder().nodeId("users.datalake.viewers@dp.domain.com").type(NodeType.GROUP).name("data.xx.viewers").dataPartitionId("dp").build();
        assertThat(sut.crossPartitionAllowedGroup()).isTrue();
    }

    @Test
    public void should_returnFalse_ifItIsNotAllowedGroupForCrossPartition() {
        EntityNode sut = EntityNode.builder().nodeId("service.xx.viewers@dp.domain.com").type(NodeType.GROUP).name("service.xx.viewers").dataPartitionId("dp").build();
        assertThat(sut.crossPartitionAllowedGroup()).isFalse();
        sut = EntityNode.builder().nodeId("users.department@dp.domain.com").type(NodeType.GROUP).name("users.department").dataPartitionId("dp").build();
        assertThat(sut.crossPartitionAllowedGroup()).isFalse();
    }

    @Test
    public void should_returnTrue_ifDatalakePermissionGroup() {
        EntityNode sut = EntityNode.builder().nodeId("users.datalake.viewers@dp.domain.com").type(NodeType.GROUP).name("users.datalake.viewers").dataPartitionId("dp").build();
        assertThat(sut.isDEPermissionGroup()).isTrue();
    }

    @Test
    public void should_returnFalse_ifNotDatalakePermissionGroup() {
        EntityNode sut = EntityNode.builder().nodeId("users.department@dp.domain.com").type(NodeType.GROUP).name("users.department").dataPartitionId("dp").build();
        assertThat(sut.isDEPermissionGroup()).isFalse();
    }

    @Test
    public void should_returnFalse_whenUsersNotFollowedByPeriodGroup() {
        EntityNode sut = EntityNode.builder().nodeId("userx@dp.domain.com").name("userxs").type(NodeType.GROUP).dataPartitionId("dp").build();

        assertThat(sut.isUserGroup()).isFalse();
    }

    @Test
    public void should_returnTrue_whenIsServiceGroup() {
        EntityNode sut = EntityNode.builder().nodeId("service.x@dp.domain.com").name("service.x").type(NodeType.GROUP).dataPartitionId("dp").build();

        assertThat(sut.isDataGroup()).isFalse();
        assertThat(sut.isUserGroup()).isFalse();
        assertThat(sut.isServiceGroup()).isTrue();
    }

    @Test
    public void should_returnTrue_whenIsRootUserGroup() {
        EntityNode sut = EntityNode.builder().nodeId("users@dp.domain.com").type(NodeType.GROUP).name("users").dataPartitionId("dp").build();

        assertThat(sut.isRootUsersGroup()).isTrue();
    }

    @Test
    public void should_returnFalse_whenNotRootUserGroup() {
        EntityNode sut = EntityNode.builder().nodeId("users.x@dp.domain.com").type(NodeType.GROUP).name("users.x").dataPartitionId("dp").build();

        assertThat(sut.isRootUsersGroup()).isFalse();
    }

    @Test
    public void should_returnTrue_whenUsersDataRootGroup() {
        EntityNode sut = EntityNode.builder().nodeId("users.data.root@dp.domain.com").type(NodeType.GROUP).name("users.data.root").dataPartitionId("dp").build();

        assertThat(sut.isUsersDataRootGroup()).isTrue();
    }

    @Test
    public void should_returnFalse_whenNotUsersDataRootGroup() {
        EntityNode sut = EntityNode.builder().nodeId("users.data.root.test@dp.domain.com").type(NodeType.GROUP).name("users.data.root.test").dataPartitionId("dp").build();

        assertThat(sut.isUsersDataRootGroup()).isFalse();
    }

}
