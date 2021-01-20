package org.opengroup.osdu.entitlements.v2.util;

import com.dslplatform.json.DslJson;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonConverter {

    private static final String ERROR_MESSAGE = "Unexpected error";

    private static final DslJson<Object> json = new DslJson<>();

    /**
     * R is for result type
     */
    public static <R> R fromJson(String inputJson, Class<R> manifest) {

        try (InputStream is = new ByteArrayInputStream(inputJson.getBytes(StandardCharsets.UTF_8))) {
            return json.deserialize(manifest, is);
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), ERROR_MESSAGE, e);
        }
    }

    public static <T> String toJson(T node) {

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            json.serialize(node, os);
            byte[] arr = os.toByteArray();
            return new String(arr, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), ERROR_MESSAGE, e);
        }
    }
}
