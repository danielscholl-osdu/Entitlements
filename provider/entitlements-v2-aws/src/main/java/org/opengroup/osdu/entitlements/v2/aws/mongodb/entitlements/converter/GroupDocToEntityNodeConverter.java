package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.converter;


import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class GroupDocToEntityNodeConverter implements Converter<GroupDoc, EntityNode> {
    @Override
    public EntityNode convert(@NonNull GroupDoc nodeDoc) {
        EntityNode result = new EntityNode();
        result.setNodeId(nodeDoc.getId().getNodeId());
        result.setAppIds(nodeDoc.getAppIds());
        result.setName(nodeDoc.getName());
        result.setType(NodeType.GROUP);
        result.setDescription(nodeDoc.getDescription());
        result.setDataPartitionId(nodeDoc.getId().getDataPartitionId());
        return result;
    }
}
