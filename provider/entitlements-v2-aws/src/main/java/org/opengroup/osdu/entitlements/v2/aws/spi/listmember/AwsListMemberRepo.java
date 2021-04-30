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
package org.opengroup.osdu.entitlements.v2.aws.spi.listmember;


import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.spi.listmember.ListMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AwsListMemberRepo implements ListMemberRepo {

    private final AuditLogger auditLogger;
    private final RetrieveGroupRepo retrieveGroupRepo;

    @Override
    public List<ChildrenReference> run(ListMemberServiceDto listMemberServiceDto) {

        try {
            List<ChildrenReference> directChildren = retrieveGroupRepo.loadDirectChildren(listMemberServiceDto.getPartitionId(), listMemberServiceDto.getGroupId());
            auditLogger.listMember(AuditStatus.SUCCESS, listMemberServiceDto.getGroupId());
            return directChildren;
        } catch (Exception ex) {
            auditLogger.listMember(AuditStatus.FAILURE, listMemberServiceDto.getGroupId());
            throw ex;
        }
    }
}
