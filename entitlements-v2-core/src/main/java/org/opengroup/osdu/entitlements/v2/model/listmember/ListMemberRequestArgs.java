package org.opengroup.osdu.entitlements.v2.model.listmember;

import org.opengroup.osdu.entitlements.v2.model.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ListMemberRequestArgs {
    @Builder.Default
    private Role role = null;
    @Builder.Default
    private Boolean includeType = Boolean.FALSE;
}
