package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.ListGroupOnBehalfOfTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class ListGroupOnBehalfOfAzureTest extends ListGroupOnBehalfOfTest {

    public ListGroupOnBehalfOfAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }

    @Test
    public void should200ForGetGroupsOnBehalfOfWithRoleEnabled() {
        test200ForGetGroupsOnBehalfOfWithRoleEnabled();
    }
}
