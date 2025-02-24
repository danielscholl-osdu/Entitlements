package org.opengroup.osdu.entitlements.v2.acceptance.util;

public interface ConfigurationService {

    String getTenantId();

    String getServiceUrl();

    String getDomain();

    String getIdOfGroup(String groupName);

    default String getMemberMailId() {
        return "testMember@test.com";
    }

    default String getOwnerMailId() {
        return "testmMemberOwner@test.com";
    }

    default String getMemberMailId_toBeDeleted(long timestamp) {
        return String.format("testMember-%s@test.com", timestamp);
    }
}
