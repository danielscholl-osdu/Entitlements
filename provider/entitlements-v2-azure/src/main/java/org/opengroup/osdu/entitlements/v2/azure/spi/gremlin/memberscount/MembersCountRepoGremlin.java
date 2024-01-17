package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.memberscount;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountResponseDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.memberscount.DefaultMembersCountRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Primary
public class MembersCountRepoGremlin extends DefaultMembersCountRepo {
    private final RetrieveGroupRepo retrieveGroupRepo;

    @Override
    public MembersCountResponseDto getMembersCount(MembersCountServiceDto membersCountServiceDto) {
        return retrieveGroupRepo.getMembersCount(membersCountServiceDto.getPartitionId(), membersCountServiceDto.getGroupId(), membersCountServiceDto.getRole());
    }
}
