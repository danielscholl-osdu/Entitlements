package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupRepoDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.spi.creategroup.CreateGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CreateGroupService {

    @Value("${app.quota.users.data.root:5000}")
    private int dataRootGroupQuota;

    private final CreateGroupRepo createGroupRepo;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final GroupCacheService groupCacheService;
    private final JaxRsDpsLog log;
    private final DefaultGroupsService defaultGroupsService;

    public EntityNode run(EntityNode groupNode, CreateGroupServiceDto createGroupServiceDto) {
        log.info(String.format("requested by %s", createGroupServiceDto.getRequesterId()));
        EntityNode requesterNode = EntityNode.createMemberNodeForRequester(createGroupServiceDto.getRequesterId(), createGroupServiceDto.getPartitionId());
        Set<ParentReference> allExistingParents = groupCacheService.getFromPartitionCache(requesterNode.getNodeId(), createGroupServiceDto.getPartitionId());
        if (allExistingParents.size() >= EntityNode.MAX_PARENTS) {
            log.error(String.format("Identity %s already belong to %d groups", createGroupServiceDto.getRequesterId(), allExistingParents.size()));
            throw new AppException(HttpStatus.PRECONDITION_FAILED.value(), HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), String.format("%s's group quota hit. Identity can't belong to more than %d groups", createGroupServiceDto.getRequesterId(), EntityNode.MAX_PARENTS));
        }
        if (groupNode.isDataGroup() && defaultGroupsService.isNotDefaultGroupName(groupNode.getName())) {
            EntityNode dataRootGroupNode = retrieveGroupRepo.groupExistenceValidation(String.format(EntityNode.ROOT_DATA_GROUP_EMAIL_FORMAT, createGroupServiceDto.getPartitionDomain()), createGroupServiceDto.getPartitionId());
            Set<ParentReference> allExistingParentsOfRootDataGroup = retrieveGroupRepo.loadAllParents(dataRootGroupNode).getParentReferences();
            if (allExistingParentsOfRootDataGroup.size() >= dataRootGroupQuota) {
                log.error(String.format("Identity %s already belong to %d groups", dataRootGroupNode.getNodeId(), allExistingParentsOfRootDataGroup.size()));
                throw new AppException(HttpStatus.PRECONDITION_FAILED.value(), HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), String.format("%s's group quota hit. Identity can't belong to more than %d groups",
                        dataRootGroupNode.getNodeId(), dataRootGroupQuota));
            }
            log.debug(String.format("Creating a group with root group node: %s", dataRootGroupNode.getName()));
            CreateGroupRepoDto createGroupRepoDto = CreateGroupRepoDto.builder()
                    .requesterNode(requesterNode)
                    .dataRootGroupNode(dataRootGroupNode)
                    .addDataRootGroup(true)
                    .partitionId(createGroupServiceDto.getPartitionId()).build();
            createGroup(groupNode, createGroupRepoDto);
        } else {
            log.debug("Creating a group with no root group node");
            CreateGroupRepoDto createGroupRepoDto = CreateGroupRepoDto.builder()
                    .requesterNode(requesterNode)
                    .dataRootGroupNode(null)
                    .addDataRootGroup(false)
                    .partitionId(createGroupServiceDto.getPartitionId()).build();
            createGroup(groupNode, createGroupRepoDto);
        }
        return groupNode;
    }

    private void createGroup(EntityNode groupNode, CreateGroupRepoDto createGroupRepoDto) {
        Set<String> impactedUsers = createGroupRepo.createGroup(groupNode, createGroupRepoDto);
        groupCacheService.refreshListGroupCache(impactedUsers, createGroupRepoDto.getPartitionId());
    }
}
