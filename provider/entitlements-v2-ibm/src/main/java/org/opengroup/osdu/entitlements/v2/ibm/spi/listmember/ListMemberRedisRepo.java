/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.spi.listmember;


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
public class ListMemberRedisRepo implements ListMemberRepo {

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
