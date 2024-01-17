package org.opengroup.osdu.entitlements.v2.spi.memberscount;

import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountResponseDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountServiceDto;

public interface MembersCountRepo {
    MembersCountResponseDto getMembersCount(MembersCountServiceDto membersCountServiceDto);
}
