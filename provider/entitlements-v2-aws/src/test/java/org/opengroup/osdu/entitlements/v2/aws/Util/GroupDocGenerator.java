package org.opengroup.osdu.entitlements.v2.aws.Util;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;

import java.util.Collections;
import java.util.HashSet;

public class GroupDocGenerator extends DbUtil {
    public static GroupDoc generateGroupDoc(String id) {
        GroupDoc groupDoc = new GroupDoc();
        groupDoc.setAppIds(new HashSet<>());
        groupDoc.setDescription(DESCRIPTION + id);
        IdDoc idDoc = new IdDoc(id, DATA_PARTITION);
        groupDoc.setId(idDoc);
        groupDoc.setName(id);
        groupDoc.setAppIds(new HashSet<>(Collections.singletonList(DEFAULT_APP)));
        return groupDoc;
    }
}
