/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.service;

import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequestScope
@Component
public class IBMGroupCache {
    private Map<String, Set<ParentReference>> groupMap = new ConcurrentHashMap<>();

    public Set<ParentReference> getGroupCache(String requesterId) {
        return this.groupMap.get(requesterId);
    }

    public void addGroupCache(String requesterId, Set<ParentReference> parents) {
        this.groupMap.put(requesterId, parents);
    }
}
