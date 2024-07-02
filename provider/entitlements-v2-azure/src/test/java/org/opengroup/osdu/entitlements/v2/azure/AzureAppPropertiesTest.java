package org.opengroup.osdu.entitlements.v2.azure;

import com.azure.security.keyvault.secrets.SecretClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class AzureAppPropertiesTest {

    @InjectMocks
    private AzureAppProperties azureAppProperties;

    @Mock
    private SecretClient secretClient;


    @Test
    public void shouldGetGroupsOfInitialUsers() {
        List<String> groupsOfInitialUsers = azureAppProperties.getGroupsOfInitialUsers();
        assertNotNull(groupsOfInitialUsers);
    }

}