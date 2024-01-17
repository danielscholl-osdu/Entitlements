package org.opengroup.osdu.entitlements.v2.model.memberscount;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import org.opengroup.osdu.entitlements.v2.model.Role;

@Data
@Generated
@Builder
public class MembersCountServiceDto {
    private String groupId;
    private String requesterId;
    private String partitionId;
    private Role role;
}
