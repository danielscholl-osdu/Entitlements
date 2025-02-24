package org.opengroup.osdu.entitlements.v2.acceptance.model.response;

import lombok.Data;

import java.util.List;

@Data
public class UpdateGroupResponse {
    private String name;
    private String email;
    private List<String> appIds;
}
