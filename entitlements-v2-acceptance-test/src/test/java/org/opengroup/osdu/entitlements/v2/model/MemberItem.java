package org.opengroup.osdu.entitlements.v2.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberItem {
    private String email;
    private String role;
    private String memberType;
    private String dataPartitionId;
}
