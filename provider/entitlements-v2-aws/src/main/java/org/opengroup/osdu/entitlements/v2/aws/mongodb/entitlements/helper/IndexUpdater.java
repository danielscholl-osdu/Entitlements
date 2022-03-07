// Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper;

import org.opengroup.osdu.core.aws.mongodb.helper.BasicMongoDBHelper;
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
        helper.ensureIndex(groupCollection, new Index().on("_id.dataPartitionId", Sort.Direction.ASC));
        helper.ensureIndex(groupCollection, new Index().on("_id.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(groupCollection, new Index().on("directParents", Sort.Direction.ASC));
        helper.ensureIndex(groupCollection, new Index().on("directParents.parentId", Sort.Direction.ASC));
        helper.ensureIndex(groupCollection, new Index().on("directParents.parentId.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(groupCollection, new Index().on("directParents.parentId.dataPartitionId", Sort.Direction.ASC));

        String userCollection = "User-" + dataPartitionId;
        helper.ensureIndex(userCollection, new Index().on("_id.dataPartitionId", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("_id.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("memberOf.parentId", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("memberOf.parentId.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("directParents", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("directParents.parentId", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("directParents.parentId.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(userCollection, new Index().on("directParents.parentId.dataPartitionId", Sort.Direction.ASC));
    }
}
