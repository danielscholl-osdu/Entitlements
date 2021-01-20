package org.opengroup.osdu.entitlements.v2.acceptance.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberRequestData {
    private String groupEmail;
    private String memberEmail;
    private String role;
}
