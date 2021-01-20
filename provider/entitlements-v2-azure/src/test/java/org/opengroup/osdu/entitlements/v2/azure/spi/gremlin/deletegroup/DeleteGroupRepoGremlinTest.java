package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.deletegroup;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.spi.deletegroup.DeleteGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DeleteGroupRepoGremlinTest {

    @Autowired
    private DeleteGroupRepo deleteGroupRepo;

    @Autowired
    private GremlinConnector gremlinConnector;

    @After
    public void cleanup() {
        gremlinConnector.getGraphTraversalSource().V().drop().iterate();
    }

    @Test
    public void shouldSuccessfullyDeleteGroup() {
        final String nodeId = "id";
        final GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, nodeId)
                .property(VertexPropertyNames.NAME, "name").next();
        Assert.assertTrue(graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, nodeId).hasNext());

        EntityNode entityNode = new EntityNode();
        entityNode.setNodeId(nodeId);
        deleteGroupRepo.deleteGroup(entityNode);

        Assert.assertFalse(graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, nodeId).hasNext());
    }
}
