package org.opengroup.osdu.entitlements.v2.azure.spi.addmember;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.azure.config.CacheConfig;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenTreeDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class AddMemberRepoGremlinTest {

    @Autowired
    private GremlinConnector gremlinConnector;

    @Autowired
    private AddMemberRepo addMemberRepo;

    @MockBean
    private AuditLogger auditLogger;

    @MockBean
    private CacheConfig cacheConfig;

    @MockBean
    private RetrieveGroupRepo retrieveGroupRepo;

    @After
    public void cleanup() {
        gremlinConnector.getGraphTraversalSource().V().drop().iterate();
        gremlinConnector.getGraphTraversalSource().E().drop().iterate();
    }

    @Test
    public void shouldAddMemberSuccessfully() {
        EntityNode group = EntityNode.builder().nodeId("groupId").dataPartitionId("dp").build();
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString()).property(VertexPropertyNames.NODE_ID, group.getNodeId())
                .property(VertexPropertyNames.DATA_PARTITION_ID, group.getDataPartitionId()).next();
        EntityNode member = EntityNode.builder().nodeId("memberId").dataPartitionId("dp").type(NodeType.USER).build();
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder().memberNode(member)
                .partitionId("dp").role(Role.OWNER).build();
        ChildrenTreeDto childrenTreeDto = ChildrenTreeDto.builder().childrenUserIds(Collections.singletonList("memberId")).build();
        when(retrieveGroupRepo.loadAllChildrenUsers(member)).thenReturn(childrenTreeDto);

        Set<String> impactedUsers = addMemberRepo.addMember(group, addMemberRepoDto);

        List<Vertex> members = graphTraversalSource.V()
                .has(VertexPropertyNames.NODE_ID, group.getNodeId())
                .outE()
                .has(EdgePropertyNames.ROLE, Role.OWNER.getValue())
                .inV()
                .toList();
        assertEquals(1, members.size());
        assertEquals(1, impactedUsers.size());
        assertTrue(impactedUsers.contains("memberId"));
        assertEquals(member.getNodeId(), members.iterator().next().value(VertexPropertyNames.NODE_ID));
        List<Edge> edges = graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, group.getNodeId()).bothE().toList();
        assertEquals(2, edges.size());
        Edge childEdge = edges.stream().filter(edge -> "child".equals(edge.label())).findFirst().orElse(null);
        Edge parentEdge = edges.stream().filter(edge -> "parent".equals(edge.label())).findFirst().orElse(null);
        assertEquals("memberId", childEdge.inVertex().value("nodeId"));
        assertEquals("groupId", childEdge.outVertex().value("nodeId"));
        assertEquals("OWNER", childEdge.value("role"));
        assertEquals("memberId", parentEdge.outVertex().value("nodeId"));
        assertEquals("groupId", parentEdge.inVertex().value("nodeId"));
        Mockito.verify(auditLogger).addMember(AuditStatus.SUCCESS, "groupId", "memberId", Role.OWNER);
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForAddMemberWithoutRole() {
        EntityNode group = EntityNode.builder().nodeId("groupId").dataPartitionId("dp").build();
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString()).property(VertexPropertyNames.NODE_ID, group.getNodeId())
                .property(VertexPropertyNames.DATA_PARTITION_ID, group.getDataPartitionId()).next();
        EntityNode member = EntityNode.builder().nodeId("memberId").dataPartitionId("dp").type(NodeType.USER).build();
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder().memberNode(member)
                .partitionId("dp").build();
        ChildrenTreeDto childrenTreeDto = ChildrenTreeDto.builder().childrenUserIds(Collections.singletonList("memberId")).build();
        when(retrieveGroupRepo.loadAllChildrenUsers(member)).thenReturn(childrenTreeDto);

        try {
            addMemberRepo.addMember(group, addMemberRepoDto);
            fail("should throw exception");
        } catch (IllegalArgumentException illegalArgumentException) {
            assertEquals("Role parameter is required to add a member", illegalArgumentException.getMessage());
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }

    }

    @Test
    public void shouldReturnEmptySet() {
        Deque<Operation> executedCommandsDeque = new LinkedList<>();
        EntityNode entityNode = EntityNode.builder().build();
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder().partitionId("dp").role(Role.OWNER).build();

        Set<String> impactedUsers = addMemberRepo.addMember(executedCommandsDeque, entityNode, addMemberRepoDto);

        assertTrue(impactedUsers.isEmpty());
    }
}
