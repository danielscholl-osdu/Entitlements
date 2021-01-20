package org.opengroup.osdu.entitlements.v2.service;

import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ParentReferenceService {

    @Autowired
    private RetrieveGroupRepo retrieveGroupRepo;

    @Autowired
    private PermissionService permissionService;

    public Map<Role, List<EntityNode>> groupParentsByChildRole(final List<ParentReference> directParents, final EntityNode childGroup, final String domain) {
        final Map<Role, List<EntityNode>> resultMap = directParents.stream()
                .map(EntityNode::createNodeFromParentReference)
                .collect(Collectors.groupingBy(entityNode -> getRoleOfGroup(childGroup, entityNode, String.format(EntityNode.ROOT_DATA_GROUP_EMAIL_FORMAT, domain))));
        for (Role role : Role.values()) {
            resultMap.putIfAbsent(role, Collections.emptyList());
        }
        return resultMap;
    }

    private Role getRoleOfGroup(EntityNode group, EntityNode parentGroup, String rootDataGroupNodeId) {
        return permissionService.hasOwnerPermissionOf(group, parentGroup, rootDataGroupNodeId) ? Role.OWNER : Role.MEMBER;
    }
}
