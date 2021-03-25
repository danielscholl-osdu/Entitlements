package org.opengroup.osdu.entitlements.v2.acceptance.model.request;

import lombok.Builder;
import lombok.Data;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupType;

@Data
@Builder
public class GetGroupsRequestData {
    private String memberEmail;
    private GroupType type;
    private String appId;
}
