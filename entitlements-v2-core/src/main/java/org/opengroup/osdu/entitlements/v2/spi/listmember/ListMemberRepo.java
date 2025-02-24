package org.opengroup.osdu.entitlements.v2.spi.listmember;

import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;

import java.util.List;

public interface ListMemberRepo {
    List<ChildrenReference> run(ListMemberServiceDto listMemberServiceDto);
}
