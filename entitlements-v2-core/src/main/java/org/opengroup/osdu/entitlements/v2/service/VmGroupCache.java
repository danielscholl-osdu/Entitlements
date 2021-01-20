package org.opengroup.osdu.entitlements.v2.service;

import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.service.GroupCache;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequestScope
@Component
public class VmGroupCache implements GroupCache {
    private Map<String, Set<ParentReference>> groupMap = new HashMap<>();

    @Override
    public Set<ParentReference> getGroupCache(String requesterId) {
        return this.groupMap.get(requesterId);
    }

    @Override
    public void addGroupCache(String requesterId, Set<ParentReference> parents) {
        this.groupMap.put(requesterId, parents);
    }
}
