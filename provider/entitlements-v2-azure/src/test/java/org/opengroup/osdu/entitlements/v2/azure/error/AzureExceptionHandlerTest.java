package org.opengroup.osdu.entitlements.v2.azure.error;


import org.apache.tinkerpop.gremlin.driver.exception.NoHostAvailableException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class AzureExceptionHandlerTest {

    @Mock
    private JaxRsDpsLog jaxRsDpsLog;

    @InjectMocks
    private AzureExceptionHandler azureExceptionHandler;

    @Test
    public void shouldReturnNoHostAvailableException(){
        NoHostAvailableException exception = new NoHostAvailableException();

        ResponseEntity<AppError> response = azureExceptionHandler.handleNoHostAvailableException(exception);

        assertEquals(503, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        AppError body = response.getBody();
        assertEquals(503, body.getCode());
        assertEquals("No available upstream host, please contact support team", body.getMessage());
        Mockito.verify(jaxRsDpsLog).error(exception.getMessage(), exception);

    }
}
