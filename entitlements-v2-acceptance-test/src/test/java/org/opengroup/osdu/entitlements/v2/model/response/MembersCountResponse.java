package org.opengroup.osdu.entitlements.v2.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MembersCountResponse {
    int membersCount;
    String groupEmail;
}
