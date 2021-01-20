package org.opengroup.osdu.entitlements.v2.model;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;

@Data
@Builder
@Generated
public class GroupEmailDto {
    private String groupEmail;
    private String requesterId;
    private String partitionId;
    private String partitionDomain;
}

