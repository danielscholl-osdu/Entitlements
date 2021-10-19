package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.converter;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class EntityNodeToGroupDocConverter implements Converter<EntityNode, GroupDoc> {
    @Override
    public GroupDoc convert(@NonNull EntityNode entityNodeMDB) {
        GroupDoc result = new GroupDoc();
        IdDoc id = new IdDoc(entityNodeMDB.getNodeId(), entityNodeMDB.getDataPartitionId());

        if (entityNodeMDB.getType() == NodeType.USER) {
            throw new RuntimeException("Incorrect converter was used");
        }

        result.setId(id);
        result.setAppIds(entityNodeMDB.getAppIds());
        result.setDescription(entityNodeMDB.getDescription());
        result.setName(entityNodeMDB.getName());
        return result;
    }
}
