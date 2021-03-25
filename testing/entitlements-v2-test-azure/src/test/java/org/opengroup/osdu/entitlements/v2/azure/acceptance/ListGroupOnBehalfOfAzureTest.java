package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.ListGroupOnBehalfOfTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class ListGroupOnBehalfOfAzureTest extends ListGroupOnBehalfOfTest {

    public ListGroupOnBehalfOfAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }
}