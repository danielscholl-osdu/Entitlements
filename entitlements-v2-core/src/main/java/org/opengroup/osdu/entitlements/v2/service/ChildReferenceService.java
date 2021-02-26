package org.opengroup.osdu.entitlements.v2.service;

import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChildReferenceService {

    /**
     * @return - child entityNodes grouped by role
     */
    public Map<Role, List<EntityNode>> groupChildrenByRole(final List<ChildrenReference> children, final String partitionDomain) {
        final List<String> idsOfOwners = children.stream()
                .filter(childrenReference -> childrenReference.getRole() == Role.OWNER)
                .map(ChildrenReference::getId)
                .collect(Collectors.toList());
        final Map<Role, List<EntityNode>> resultMap = children.stream()
                .map(childrenReference -> convertToEntityNode(childrenReference, partitionDomain))
                .collect(Collectors.toList())
                .stream()
                .collect(Collectors.groupingBy(entityNode -> getRole(idsOfOwners, entityNode.getNodeId())));
        for (Role role : Role.values()) {
            resultMap.putIfAbsent(role, Collections.emptyList());
        }
        return resultMap;
    }

    private Role getRole(final List<String> idsOfOwners, final String id) {
        return idsOfOwners.contains(id) ? Role.OWNER : Role.MEMBER;
    }

    private EntityNode convertToEntityNode(ChildrenReference childrenReference, String partitionDomain) {
        // TODO: This logic was moved from EntityNode to keep working logic.
        String email = childrenReference.getId();
        if (email.endsWith(partitionDomain) && !email.contains("desid")) {
            return EntityNode.createNodeFromGroupEmail(email);
        } else {
            return EntityNode.createMemberNodeForNewUser(email, childrenReference.getDataPartitionId());
        }
    }
}
