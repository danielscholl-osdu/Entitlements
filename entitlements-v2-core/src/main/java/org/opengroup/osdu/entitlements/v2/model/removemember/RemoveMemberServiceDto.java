package org.opengroup.osdu.entitlements.v2.model.removemember;

import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;

@Data
@Generated
@Builder
public class RemoveMemberServiceDto {
    private String groupEmail;
    private String memberEmail;
    private String requesterId;
    private String partitionId;
    private ChildrenReference childrenReference;
}
