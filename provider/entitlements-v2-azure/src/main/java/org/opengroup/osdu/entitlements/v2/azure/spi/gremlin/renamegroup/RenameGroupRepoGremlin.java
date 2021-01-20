package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.renamegroup;


import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.renamegroup.RenameGroupRepo;
import org.opengroup.osdu.entitlements.v2.util.GroupCreationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Set;

@Repository
public class RenameGroupRepoGremlin implements RenameGroupRepo {
    @Autowired
    private GraphTraversalSourceUtilService graphTraversalSourceUtilService;


    @Override
    public Set<String> run(EntityNode groupNode, String newGroupName) {
        String existingNodeId = groupNode.getNodeId();
        String partitionDomain = existingNodeId.split("@")[1];

        String newNodeId = GroupCreationUtil.createGroupEmail(newGroupName, partitionDomain);

        graphTraversalSourceUtilService.updateProperty(existingNodeId, VertexPropertyNames.NAME, newGroupName);
        graphTraversalSourceUtilService.updateProperty(existingNodeId, VertexPropertyNames.NODE_ID, newNodeId);

        return new HashSet<>();
    }
}
