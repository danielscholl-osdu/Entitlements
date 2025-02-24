package org.opengroup.osdu.entitlements.v2.util;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class FileReaderService {

    public String readFile(String fileName) {
        try {
            return readFileToString(fileName);
        } catch (final IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Cannot read file");
        }
    }

    private String readFileToString(final String fileName) throws IOException {
        try (InputStream inputStream = FileReaderService.class.getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException();
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toString(StandardCharsets.UTF_8.toString());
        }
    }
}
