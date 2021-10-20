package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.helper.BasicMongoDBHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class IndexUpdater {

    private final BasicMongoDBHelper helper;
    private HashSet<String> indexedDataPartitions = new HashSet<>();

    @Autowired
    public IndexUpdater(BasicMongoDBHelper helper) {
        this.helper = helper;
    }

    public void checkIndex(String dataPartitionId) {
        if (indexedDataPartitions.contains(dataPartitionId)) {
            return;
        }

        indexedDataPartitions.add(dataPartitionId);
        this.updateIndexes(dataPartitionId);
    }

    //TODO: recheck indexes
    private void updateIndexes(String dataPartitionId) {
        String groupCollection = "Group-" + dataPartitionId;
        String userCollection = "User-" + dataPartitionId;

        helper.ensureIndex(groupCollection, new Index().on("_id.dataPartitionId", Sort.Direction.ASC));
        helper.ensureIndex(groupCollection, new Index().on("_id.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(groupCollection, new Index().on("directParents", Sort.Direction.ASC));
        helper.ensureIndex(groupCollection, new Index().on("directParents.role", Sort.Direction.ASC));
        helper.ensureIndex(groupCollection, new Index().on("directParents.parentId", Sort.Direction.ASC));
        helper.ensureIndex(groupCollection, new Index().on("directParents.parentId.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(groupCollection, new Index().on("directParents.parentId.dataPartitionId", Sort.Direction.ASC));

        helper.ensureIndex(userCollection, new Index().on("_id.dataPartitionId", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("_id.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("memberOf", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("memberOf.role", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("memberOf.parentId", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("memberOf.parentId.dataPartitionId", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("memberOf.parentId.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("directParents", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("directParents.role", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("directParents.parentId", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("directParents.parentId.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("directParents.parentId.dataPartitionId", Sort.Direction.ASC));
    }

}
