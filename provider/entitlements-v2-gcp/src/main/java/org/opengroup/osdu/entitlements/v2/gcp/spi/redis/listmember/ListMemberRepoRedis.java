package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.listmember;

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
public class ListMemberRepoRedis implements ListMemberRepo {

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
