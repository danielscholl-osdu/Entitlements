package org.opengroup.osdu.entitlements.v2.aws.spi;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.renamegroup.RenameGroupRepo;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class RenameGroupRepoMongoDB extends BasicEntitlementsHelper implements RenameGroupRepo {

    @Override
    public Set<String> run(EntityNode groupNode, String newGroupName) {

        groupHelper.renameGroup(new IdDoc(groupNode.getNodeId(), groupNode.getDataPartitionId()), newGroupName);

        //TODO: return IDS then cash will work
        return new HashSet<>();
    }
}
