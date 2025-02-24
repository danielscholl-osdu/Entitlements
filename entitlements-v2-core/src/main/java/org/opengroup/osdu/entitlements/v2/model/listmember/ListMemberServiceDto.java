package org.opengroup.osdu.entitlements.v2.model.listmember;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;

@Data
@Generated
@Builder
public class ListMemberServiceDto {
    private String groupId;
    private String requesterId;
    private String partitionId;
}
