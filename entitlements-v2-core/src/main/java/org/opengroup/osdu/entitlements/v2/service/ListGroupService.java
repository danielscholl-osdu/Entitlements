package org.opengroup.osdu.entitlements.v2.service;

import com.google.common.base.Strings;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListGroupService {
    private final JaxRsDpsLog log;
    private final RequestInfo requestInfo;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final GroupsProvider groupsProvider;

    public Set<ParentReference> getGroups(ListGroupServiceDto listGroupServiceDto) {
        log.debug(String.format("ListGroupService#run timestamp: %d", System.currentTimeMillis()));
        String requesterId = listGroupServiceDto.getRequesterId();
        log.info(ListGroupService.class.getName(), String.format("requested by %s", requesterId));
        Set<ParentReference> groups = new HashSet<>();
        listGroupServiceDto.getPartitionIds().forEach(partitionId ->
                groups.addAll(groupsProvider.getGroupsInContext(requesterId, partitionId)));
        log.debug(String.format("ListGroupService#run cache look up done timestamp: %d", System.currentTimeMillis()));
        String serviceAccount = requestInfo.getTenantInfo().getServiceAccount();
        if (serviceAccount.equalsIgnoreCase(requesterId) || Strings.isNullOrEmpty(listGroupServiceDto.getAppId())) {
            return groups;
        }
        return filterGroupsByAppId(groups, listGroupServiceDto);
    }

    private Set<ParentReference> filterGroupsByAppId(Set<ParentReference> groups,
                                                     ListGroupServiceDto listGroupServiceDto) {
        String appId = listGroupServiceDto.getAppId();
        log.debug(String.format("Filtering groups based on caller's appId: %s", appId));
        Set<ParentReference> accessibleGroups = new HashSet<>();
        listGroupServiceDto.getPartitionIds().forEach(partitionId ->
                accessibleGroups.addAll(retrieveGroupRepo.filterParentsByAppId(groups, partitionId, appId)));
        log.debug(String.format(
                "ListGroupService#run cache app id filter done timestamp: %d", System.currentTimeMillis()));
        return accessibleGroups;
    }
}
