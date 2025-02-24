package org.opengroup.osdu.entitlements.v2.model.response;

import lombok.Data;
import org.opengroup.osdu.entitlements.v2.model.MemberItem;

import java.util.List;

@Data
public class ListMemberResponse {
    private List<MemberItem> members;
}
