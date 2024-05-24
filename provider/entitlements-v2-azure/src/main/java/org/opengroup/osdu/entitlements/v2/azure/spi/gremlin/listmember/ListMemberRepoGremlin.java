package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.listmember;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.service.MemberCacheService;
import org.opengroup.osdu.entitlements.v2.spi.listmember.ListMemberRepo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ListMemberRepoGremlin implements ListMemberRepo {
    private final MemberCacheService memberCacheService;

    @Override
    public List<ChildrenReference> run(ListMemberServiceDto listMemberServiceDto) {

        return memberCacheService.getFromPartitionCache(listMemberServiceDto.getGroupId(), listMemberServiceDto.getPartitionId());
        
    }
}
