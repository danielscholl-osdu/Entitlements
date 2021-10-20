package org.opengroup.osdu.entitlements.v2.errors;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class SpringExceptionMapperTest {

    @InjectMocks
    private SpringExceptionMapper springExceptionMapper;

    @Mock
    private JaxRsDpsLog log;

    @Test
    public void should_returnNullResponse_when_BrokenPipeIOExceptionIsCaptured() {
        IOException ioException = new IOException("Broken pipe");

        ResponseEntity response = springExceptionMapper.handleIOException(ioException);

        Assert.assertNull(response);
    }

    @Test
    public void should_returnServiceUnavailable_when_IOExceptionIsCaptured() {
        IOException ioException = new IOException("Not broken yet");

        ResponseEntity response = springExceptionMapper.handleIOException(ioException);

        Assert.assertEquals(HttpStatus.SC_SERVICE_UNAVAILABLE, response.getStatusCodeValue());
    }
}
