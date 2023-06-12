package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.opengroup.osdu.entitlements.v2.model.deletegroup.DeleteGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeEvent;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeType;
import org.opengroup.osdu.entitlements.v2.spi.deletegroup.DeleteGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DeleteGroupService {

    private final DeleteGroupRepo deleteGroupRepo;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final GroupCacheService groupCacheService;
    private final JaxRsDpsLog log;
    private final DefaultGroupsService defaultGroupsService;
    private final PermissionService permissionService;
    private final RequestInfo requestInfo;
    private final IEventPublisher eventPublisher;

    @Value("${event-publishing.enabled:false}")
    private Boolean eventPublishingEnabled;

    public void run(EntityNode groupNode, DeleteGroupServiceDto deleteGroupServiceDto) {
        log.info(String.format("requested by %s", deleteGroupServiceDto.getRequesterId()));
        Optional<EntityNode> existingGroupEntityNode = retrieveGroupRepo.getEntityNode(groupNode.getNodeId(), deleteGroupServiceDto.getPartitionId());
        if (!existingGroupEntityNode.isPresent()) {
            return;
        }
        EntityNode requesterNode = EntityNode.createMemberNodeForRequester(deleteGroupServiceDto.getRequesterId(), deleteGroupServiceDto.getPartitionId());
        permissionService.verifyCanManageMembers(requesterNode, existingGroupEntityNode.get());
        if (defaultGroupsService.isDefaultGroupName(groupNode.getName())) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Invalid group, bootstrap groups are not allowed to be deleted");
        }
        Set<String> impactedUsers = deleteGroupRepo.deleteGroup(existingGroupEntityNode.get());
        groupCacheService.refreshListGroupCache(impactedUsers, deleteGroupServiceDto.getPartitionId());
        publishDeleteGroupEntitlementsChangeEvent(groupNode.getNodeId(), deleteGroupServiceDto.getRequesterId());
    }

    private void publishDeleteGroupEntitlementsChangeEvent(String groupEmail, String requesterId) {
        if(eventPublishingEnabled) {
            EntitlementsChangeEvent[] event =  {
                    EntitlementsChangeEvent.builder()
                            .kind(EntitlementsChangeType.groupDeleted)
                            .group(groupEmail)
                            .modifiedBy(requesterId)
                            .modifiedOn(System.currentTimeMillis()).build()
            };
            eventPublisher.publish(event, requestInfo.getHeaders().getHeaders());
        }
    }
}
