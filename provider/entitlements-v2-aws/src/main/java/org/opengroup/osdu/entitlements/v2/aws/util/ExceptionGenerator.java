/**
* Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.entitlements.v2.aws.util;

public class ExceptionGenerator {
    private ExceptionGenerator() {
    }

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
