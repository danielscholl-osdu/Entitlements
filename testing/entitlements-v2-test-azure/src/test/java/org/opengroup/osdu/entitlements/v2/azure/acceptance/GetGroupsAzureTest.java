package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.GetGroupsTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class GetGroupsAzureTest extends GetGroupsTest {

    public GetGroupsAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }

    @Test
    public void should200ForGetGroupsWithRoleEnabled() {
        test200ForGetGroupsWithRoleEnabled();
    }
}

