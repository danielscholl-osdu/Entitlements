package org.opengroup.osdu.entitlements.v2.acceptance.model.response;

import lombok.Data;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;

import java.util.HashSet;
import java.util.Set;

@Data
public class ListGroupResponse {
    public String desId;
    public String memberEmail;
    public Set<GroupItem> groups = new HashSet<>();
}
