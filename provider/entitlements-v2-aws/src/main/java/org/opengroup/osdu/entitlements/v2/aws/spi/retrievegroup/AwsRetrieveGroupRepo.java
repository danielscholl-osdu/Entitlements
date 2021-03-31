// Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package org.opengroup.osdu.entitlements.v2.aws.spi.retrievegroup;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.model.*;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class AwsRetrieveGroupRepo implements RetrieveGroupRepo {


    @Override
    public EntityNode groupExistenceValidation(String groupId, String partitionId) {
        return getEntityNode(groupId, partitionId).orElseThrow(() ->
                new AppException(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), ""));
    }

    @Override
    public Optional<EntityNode> getEntityNode(String entityEmail, String partitionId) {
        //To-be-implemented
        return null;
    }

    @Override
    public Set<EntityNode> getEntityNodes(String partitionId, List<String> nodeIds) {
        return new HashSet<>();
    }

    @Override
    public Map<String, Set<String>> getUserPartitionAssociations(Set<String> userIds) {
        return new HashMap<>();
    }

    @Override
    public Set<EntityNode> getAllGroupNodes(String partitionId, String partitionDomain) {
        return new HashSet<>();
    }

    @Override
    public Boolean hasDirectChild(EntityNode groupNode, ChildrenReference childrenReference) {
        //To-be-implemented
        return false;
    }

    @Override
    public List<ParentReference> loadDirectParents(String partitionId, String... nodeIds) {
        //To-be-implemented
        return null;
    }

    @Override
    public ParentTreeDto loadAllParents(EntityNode memberNode) {
        //To-be-implemented
        return null;
    }

    @Override
    public List<ChildrenReference> loadDirectChildren(String partitionId, String... nodeIds) {//To-be-implemented
        return null;
    }

    @Override
    public ChildrenTreeDto loadAllChildrenUsers(EntityNode node) {
        //To-be-implemented
        return null;
    }

    public Set<ParentReference> filterParentsByAppId(Set<ParentReference> parentReferences, String partitionId, String appId) {
        //To-be-implemented
        return null;
    }

    @Override
    public Set<String> getGroupOwners(String partitionId, String nodeId) {
        return new HashSet<>();
    }

    @Override
    public Map<String, Integer> getAssociationCount(List<String> userIds) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Integer> getAllUserPartitionAssociations() {
        return new HashMap<>();
    }

    private int calculateMaxDepth() {
        // TODO: 584695 Implement this method
        return 0;
    }

}
