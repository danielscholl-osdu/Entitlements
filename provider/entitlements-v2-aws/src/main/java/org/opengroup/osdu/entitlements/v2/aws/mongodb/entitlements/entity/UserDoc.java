package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity;



import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.NodeRelationDoc;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Document("User")
public class UserDoc extends BaseDoc {

    private Set<NodeRelationDoc> memberOf = new HashSet<>();

    public Set<NodeRelationDoc> getAllParents() {
        return memberOf;
    }

    public void setAllParents(Set<NodeRelationDoc> memberOf) {
        this.memberOf = memberOf;
    }
}
