package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.updateappids;

import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.updateappids.UpdateAppIdsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class UpdateAppIdsRepoGremlin implements UpdateAppIdsRepo {

    @Autowired
    private GraphTraversalSourceUtilService graphTraversalSourceUtilService;


    @Override
    public void run(EntityNode groupNode, Set<String> allowedAppIds) {
        String existingNodeId = groupNode.getNodeId();
        graphTraversalSourceUtilService.updateProperty(existingNodeId, VertexPropertyNames.APP_IDS, allowedAppIds.toString());
    }
}
