package org.opengroup.osdu.entitlements.v2.acceptance.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;

@Slf4j
public class CustomHttpClientResponseHandler implements HttpClientResponseHandler<CloseableHttpResponse> {

    @Override
    public CloseableHttpResponse handleResponse(ClassicHttpResponse classicHttpResponse) {
        HttpEntity entity = classicHttpResponse.getEntity();
        if(classicHttpResponse.getCode() != HttpStatus.SC_NO_CONTENT) {
            String body = "";
            try {
                body = EntityUtils.toString(entity);
            } catch (IOException | ParseException e) {
                log.error("unable to parse response");
            }
            HttpEntity newEntity = new StringEntity(body, ContentType.parse(entity.getContentType()));
            classicHttpResponse.setEntity(newEntity);
        }
        return (CloseableHttpResponse) classicHttpResponse;
    }
}
