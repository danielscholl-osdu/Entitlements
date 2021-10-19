package org.opengroup.osdu.entitlements.v2.aws.util;

//TODO: Add more details to messages
public class ExceptionGenerator {

    public static IllegalArgumentException groupNotFound(String id, String tenant) {
        return new IllegalArgumentException(String.format("Group not found by id %s in tenant %s", id, tenant));
    }

    public static IllegalStateException notUpdatedOne(String id, long updateCount) {
        return new IllegalStateException(String.format("Wrong update count on query %s: expected 1, got %d", id, updateCount));
    }

    public static IllegalStateException groupNotDeleted(String id) {
        return new IllegalStateException(String.format("Group with id %s was not deleted.", id));
    }

    public static IllegalArgumentException groupIsNull() {
        return new IllegalArgumentException("Group passed as arg is null.");
    }

    public static IllegalArgumentException idIsNull() {
        return new IllegalArgumentException("Id passed as arg is null.");
    }

    public static IllegalArgumentException userIsNull() {
        return new IllegalArgumentException("User passed as arg is null.");
    }

    public static IllegalStateException deleteCountMismatch(String id, long remoteRefs, long docRefs) {
        return new IllegalStateException(String.format("Count mismatch when deleting %s: refs removed = %d, document refs length = %d.", id, remoteRefs, docRefs));
    }
}
