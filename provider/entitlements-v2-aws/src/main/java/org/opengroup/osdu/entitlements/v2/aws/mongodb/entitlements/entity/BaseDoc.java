package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity;


import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.NodeRelationDoc;
import org.springframework.data.annotation.Id;

import java.util.HashSet;
import java.util.Set;

public class BaseDoc {

    @Id
    private IdDoc id;

    private Set<NodeRelationDoc> directParents = new HashSet<>();

    public IdDoc getId() {
        return id;
    }

    public void setId(IdDoc id) {
        this.id = id;
    }

    public Set<NodeRelationDoc> getDirectParents() {
        return directParents;
    }

    public void setDirectParents(Set<NodeRelationDoc> directParents) {
        this.directParents = directParents;
    }
}
