package org.opengroup.osdu.entitlements.v2.aws.spi;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.listmember.ListMemberRepo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Component
public class ListMemberRepoMongoDB extends BasicEntitlementsHelper implements ListMemberRepo {

    @Override
    //TODO: List only DIRECT children. Need to recheck is it is right requirements
    public List<ChildrenReference> run(ListMemberServiceDto request) {

        IdDoc groupIdToGetAllChildren = new IdDoc(request.getGroupId(), request.getPartitionId());

        Set<ChildrenReference> groupChildren = groupHelper.getDirectChildren(groupIdToGetAllChildren);
        for (ChildrenReference reference : groupChildren) {
            reference.setType(NodeType.GROUP);
        }

        Set<ChildrenReference> userChildren = userHelper.getDirectChildren(groupIdToGetAllChildren);
        for (ChildrenReference reference : userChildren) {
            reference.setType(NodeType.USER);
        }

        groupChildren.addAll(userChildren);
        return new ArrayList<>(groupChildren);
    }
}
