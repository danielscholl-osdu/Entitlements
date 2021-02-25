package org.opengroup.osdu.entitlements.v2.model.deletemember;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;

@Data
@Builder
@Generated
public class DeleteMemberDto {
    private String memberEmail;
    private String requesterId;
    private String partitionId;
}