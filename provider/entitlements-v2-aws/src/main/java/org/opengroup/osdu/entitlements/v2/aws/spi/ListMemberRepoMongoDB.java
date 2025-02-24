/**
* Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.entitlements.v2.aws.spi;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.listmember.ListMemberRepo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Component
public class ListMemberRepoMongoDB extends BasicEntitlementsHelper implements ListMemberRepo {

    @Override
    //List only DIRECT children. Need to recheck is it is right requirements
    public List<ChildrenReference> run(ListMemberServiceDto request) {

        IdDoc groupIdToGetAllChildren = new IdDoc(request.getGroupId(), request.getPartitionId());

        Set<ChildrenReference> groupChildren = groupHelper.getDirectChildren(groupIdToGetAllChildren);
        for (ChildrenReference reference : groupChildren) {
            reference.setType(NodeType.GROUP);
        }

        Set<ChildrenReference> userChildren = userHelper.getDirectChildren(groupIdToGetAllChildren);
        for (ChildrenReference reference : userChildren) {
            reference.setType(NodeType.USER);
        }

        groupChildren.addAll(userChildren);
        return new ArrayList<>(groupChildren);
    }
}
