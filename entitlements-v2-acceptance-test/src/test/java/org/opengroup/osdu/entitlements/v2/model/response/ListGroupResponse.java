package org.opengroup.osdu.entitlements.v2.model.response;

import lombok.Data;
import org.opengroup.osdu.entitlements.v2.model.GroupItem;

import java.util.HashSet;
import java.util.Set;

@Data
public class ListGroupResponse {
    public String desId;
    public String memberEmail;
    public Set<GroupItem> groups = new HashSet<>();
}
