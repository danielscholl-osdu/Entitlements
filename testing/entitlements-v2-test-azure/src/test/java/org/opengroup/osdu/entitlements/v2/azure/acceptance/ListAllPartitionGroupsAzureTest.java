package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.ListAllPartitionGroupsTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class ListAllPartitionGroupsAzureTest extends ListAllPartitionGroupsTest {

    public ListAllPartitionGroupsAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }
}
