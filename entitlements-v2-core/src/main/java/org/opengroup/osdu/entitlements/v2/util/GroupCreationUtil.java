package org.opengroup.osdu.entitlements.v2.util;

public class GroupCreationUtil {
    public static String createGroupEmail(String groupName, String partitionDomain) {
        return String.format("%s@%s", groupName, partitionDomain);
    }
}
