package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.converter;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class EntityNodeToUserDocConverter implements Converter<EntityNode, UserDoc> {
    @Override
    public UserDoc convert(@NonNull EntityNode entityNodeMDB) {
        UserDoc result = new UserDoc();
        IdDoc id = new IdDoc(entityNodeMDB.getNodeId(), entityNodeMDB.getDataPartitionId());

        if (entityNodeMDB.getType() == NodeType.GROUP) {
            throw new RuntimeException("Incorrect converter was used");
        }

        result.setId(id);
        return result;
    }
}
