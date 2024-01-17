package org.opengroup.osdu.entitlements.v2.spi.memberscount;

import org.apache.commons.lang3.NotImplementedException;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountResponseDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountServiceDto;
import org.springframework.stereotype.Component;

@Component
public class DefaultMembersCountRepo implements MembersCountRepo {
    @Override
    public MembersCountResponseDto getMembersCount(MembersCountServiceDto membersCountServiceDto) {
        throw new NotImplementedException();
    }
}
