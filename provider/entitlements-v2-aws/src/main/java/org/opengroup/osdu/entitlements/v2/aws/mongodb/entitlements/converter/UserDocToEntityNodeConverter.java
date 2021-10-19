package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.converter;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class UserDocToEntityNodeConverter implements Converter<UserDoc, EntityNode> {
    @Override
    public EntityNode convert(@NonNull UserDoc nodeDoc) {
        EntityNode result = new EntityNode();
        result.setNodeId(nodeDoc.getId().getNodeId());
        result.setType(NodeType.USER);
        result.setDataPartitionId(nodeDoc.getId().getDataPartitionId());
        return result;
    }
}
