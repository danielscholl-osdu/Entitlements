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
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountServiceDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountResponseDto;
import org.opengroup.osdu.entitlements.v2.spi.memberscount.MembersCountRepo;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;

import java.util.Set;

@Component
@Primary
public class RetrieveMembersCountMongoDB extends BasicEntitlementsHelper implements MembersCountRepo {

    @Override
    public MembersCountResponseDto getMembersCount(MembersCountServiceDto membersCountServiceDto) {
        IdDoc groupIdToGetAllChildren = new IdDoc(membersCountServiceDto.getGroupId(), membersCountServiceDto.getPartitionId());
        Set<ChildrenReference> groupChildren = groupHelper.getDirectChildren(groupIdToGetAllChildren);
        Set<ChildrenReference> userChildren = userHelper.getDirectChildren(groupIdToGetAllChildren);
        groupChildren.addAll(userChildren);
        return MembersCountResponseDto.builder()
                .groupEmail(membersCountServiceDto.getGroupId())
                .membersCount(groupChildren.size())
                .build();
    }
    
}
