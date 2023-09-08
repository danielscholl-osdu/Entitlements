package org.opengroup.osdu.entitlements.v2.acceptance.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpClientService {

    private final CloseableHttpClient httpClient;
    private final String baseUrl;
    //set this mode to true if running the service and tests locally
    private Boolean localMode;
    private String header_x_user_id;

    public HttpClientService(ConfigurationService configurationService) {
        this.httpClient = createHttpClient();
        this.baseUrl = configurationService.getServiceUrl();
    }

    public CloseableHttpResponse send(RequestData requestData) throws IOException {
        ClassicHttpRequest httpRequest = createHttpRequest(requestData);
        return httpClient.execute(httpRequest, new CustomHttpClientResponseHandler());
    }

    private ClassicHttpRequest createHttpRequest(RequestData requestData) throws MalformedURLException {
        ClassicRequestBuilder classicRequestBuilder = addRequiredDetails(requestData);
        addOptionalDetails(requestData, classicRequestBuilder);
        addXUserIdHeaderForLocalMode(classicRequestBuilder);
        return classicRequestBuilder.build();
    }

    private  void addOptionalDetails(RequestData requestData, ClassicRequestBuilder classicRequestBuilder) {
        requestData.getQueryParams().forEach(classicRequestBuilder::addParameter);
        if(requestData.getBody() != null) {
            classicRequestBuilder.setEntity(requestData.getBody(), ContentType.APPLICATION_JSON);
        }
    }

    private  ClassicRequestBuilder addRequiredDetails(RequestData requestData) throws MalformedURLException {
        String resourceUrl = new URL(baseUrl + requestData.getRelativePath()).toString();
        log.info("Sending request to URL: {} HTTP Method: {}", resourceUrl, requestData.getMethod());
        return ClassicRequestBuilder.create(requestData.getMethod())
                .setUri(resourceUrl)
                .addHeader("Authorization", "Bearer " + requestData.getToken())
                .addHeader("data-partition-id", requestData.getDataPartitionId());
    }
    private  PoolingHttpClientConnectionManager createBasicHttpClientConnectionManager() {
        ConnectionConfig connConfig = ConnectionConfig.custom()
                .setConnectTimeout(1500000, TimeUnit.MILLISECONDS)
                .setSocketTimeout(1500000, TimeUnit.MILLISECONDS)
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultConnectionConfig(connConfig);
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(20);
        return connectionManager;
    }

    private CloseableHttpClient createHttpClient() {
        PoolingHttpClientConnectionManager cm = createBasicHttpClientConnectionManager();
        return HttpClientBuilder.create().setConnectionManager(cm).setConnectionManagerShared(true).build();
    }

    private void addXUserIdHeaderForLocalMode(ClassicRequestBuilder classicRequestBuilder) {
        localMode= Boolean.parseBoolean(System.getenv("LOCAL_MODE"));
        if (localMode) {
            header_x_user_id=System.getenv("HEADER_X_USER_ID");
            classicRequestBuilder.addHeader("x-user-id", header_x_user_id);
        }
    }
}
