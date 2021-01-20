package org.opengroup.osdu.entitlements.v2.model.addmember;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;

@Data
@Generated
@Builder
public class AddMemberServiceDto {
    private String groupEmail;
    private String requesterId;
    private String partitionId;
}
