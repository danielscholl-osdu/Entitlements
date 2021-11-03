package org.opengroup.osdu.entitlements.v2.aws.Util;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;

import java.util.HashSet;
import java.util.Set;

public class UserDocGenerator extends DbUtil {

    public static UserDoc createUserDocById(String id) {
        UserDoc userDoc = new UserDoc();
        IdDoc idDoc = new IdDoc(id, DATA_PARTITION);
        userDoc.setId(idDoc);
        userDoc.setId(idDoc);
        return userDoc;
    }

    public static UserDoc createUserDocByIdDOC(IdDoc idDoc) {
        UserDoc userDoc = new UserDoc();
        userDoc.setId(idDoc);
        return userDoc;
    }

    public static Set<UserDoc> createUserDocsByIds(Set<IdDoc> collect) {
        Set<UserDoc> userDocs = new HashSet<>();
        for (IdDoc idDoc : collect) {
            userDocs.add(createUserDocByIdDOC(idDoc));
        }
        return userDocs;
    }

    public static Set<UserDoc> generateUniqueUserDocs(int count) {
        Set<UserDoc> userDocs = new HashSet<>();
        for (int i = 0; i < count; i++) {
            userDocs.add(generateUniqueUserDoc());
        }
        return userDocs;
    }

    public static UserDoc generateUniqueUserDoc() {
        UserDoc userDoc = new UserDoc();
        IdDoc idDoc = new IdDoc();
        idDoc.setNodeId(String.valueOf(System.nanoTime()));
        idDoc.setDataPartitionId(DATA_PARTITION);
        userDoc.setId(idDoc);
        return userDoc;
    }
}
