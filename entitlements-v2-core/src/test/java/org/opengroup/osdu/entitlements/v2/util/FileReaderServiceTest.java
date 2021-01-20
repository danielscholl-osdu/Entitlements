package org.opengroup.osdu.entitlements.v2.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppException;

@RunWith(MockitoJUnitRunner.class)
public class FileReaderServiceTest {

    @InjectMocks
    private FileReaderService fileReaderService;

    @Test
    public void shouldReadFileSuccessfully() {
        Assert.assertEquals("{\"key\": \"value\"}", fileReaderService.readFile("/file-reader-test.json"));
    }

    @Test
    public void shouldThrowAppExceptionOnError() {
        try {
            fileReaderService.readFile("/not-existing-file.json");
        } catch (AppException exception) {
            Assert.assertEquals(500, exception.getError().getCode());
            Assert.assertEquals("Cannot read file", exception.getError().getMessage());
            Assert.assertEquals("Internal Server Error", exception.getError().getReason());
        }
    }
}
