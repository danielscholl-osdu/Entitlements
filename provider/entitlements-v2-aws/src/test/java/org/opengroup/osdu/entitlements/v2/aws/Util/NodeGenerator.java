package org.opengroup.osdu.entitlements.v2.aws.Util;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.NodeRelationDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NodeGenerator extends DbUtil {

    public static EntityNode groupNode(String id) {
        return EntityNode.builder()
                .name(id)
                .nodeId(id)
                .dataPartitionId(DATA_PARTITION)
                .description(DESCRIPTION + id)
                .type(NodeType.GROUP)
                .appIds(new HashSet<>(Collections.singletonList(DEFAULT_APP)))
                .build();
    }

    public static EntityNode userNode(String email) {
        return EntityNode.builder()
                .name(email)
                .nodeId(email)
                .dataPartitionId(DATA_PARTITION)
                .description(DESCRIPTION + email)
                .type(NodeType.USER)
                .appIds(new HashSet<>(Collections.singletonList(DEFAULT_APP)))
                .build();
    }

    public static EntityNode generateGroupNode(int index) {
        return groupNode(generateGroupId(index));
    }

    public static EntityNode generateUserNode(int index) {
        return userNode(generateUserId(index));
    }

    public static NodeRelationDoc getNodeRelationDocByIdDoc(IdDoc idDoc, Role role) {
        return new NodeRelationDoc(idDoc, role);
    }

    public static Set<NodeRelationDoc> generateUniqueNodeRelationDocs(int count, Role role) {
        Set<NodeRelationDoc> nodeRelationDocs = new HashSet<>();
        for (int i = 0; i < count; i++) {
            nodeRelationDocs.add(getNodeRelationDocByIdDoc(new IdDoc(String.valueOf(System.nanoTime()), String.valueOf(System.nanoTime())), role));
        }
        return nodeRelationDocs;
    }
}
