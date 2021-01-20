package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.model.deletegroup.DeleteGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.deletegroup.DeleteGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeleteGroupService {

    private final DeleteGroupRepo deleteGroupRepo;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final JaxRsDpsLog log;
    private final DefaultGroupsService defaultGroupsService;
    private final RequestInfo requestInfo;
    private final PermissionService permissionService;

    public void run(EntityNode groupNode, DeleteGroupServiceDto deleteGroupServiceDto) {
        log.info(String.format("requested by %s", deleteGroupServiceDto.getRequesterId()));
        Optional<EntityNode> existingGroupEntityNode = retrieveGroupRepo.getEntityNode(groupNode.getNodeId(), deleteGroupServiceDto.getPartitionId());
        if (!existingGroupEntityNode.isPresent()) {
            return;
        }
        EntityNode requesterNode = EntityNode.createMemberNodeForRequester(deleteGroupServiceDto.getRequesterId(), deleteGroupServiceDto.getPartitionId());
        final String serviceAccountId = requestInfo.getTenantInfo().getServiceAccount();
        if (!permissionService.hasOwnerPermissionOf(requesterNode, existingGroupEntityNode.get()) &&
                !deleteGroupServiceDto.getRequesterId().equalsIgnoreCase(serviceAccountId)) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), "Not authorized to manage members");
        }
        if (defaultGroupsService.isDefaultGroupName(groupNode.getName())) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Invalid group, bootstrap groups are not allowed to be deleted");
        }
        deleteGroupRepo.deleteGroup(existingGroupEntityNode.get());

    }
}
