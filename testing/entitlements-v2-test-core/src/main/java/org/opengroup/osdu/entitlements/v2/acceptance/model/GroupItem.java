package org.opengroup.osdu.entitlements.v2.acceptance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupItem {
    private String name;
    private String email;
    private String description;
}
