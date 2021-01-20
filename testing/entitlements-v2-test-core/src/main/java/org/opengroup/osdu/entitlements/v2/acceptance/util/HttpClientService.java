package org.opengroup.osdu.entitlements.v2.acceptance.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class HttpClientService {

    private final Client client;
    private final String baseUrl;

    public HttpClientService(ConfigurationService configurationService) {
        this.client = getClient();
        this.baseUrl = configurationService.getServiceUrl();
    }

    public ClientResponse send(RequestData requestData) throws Exception {
        WebResource webResource = client.resource(new URL(baseUrl + requestData.getRelativePath()).toString());
        final WebResource.Builder builder = webResource.getRequestBuilder();
        builder.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + requestData.getToken())
                .header("data-partition-id", requestData.getDataPartitionId());
        if (requestData.getBody() != null) {
            return builder.method(requestData.getMethod(), ClientResponse.class, requestData.getBody());
        }
        return builder.method(requestData.getMethod(), ClientResponse.class);
    }

    private Client getClient() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {}
        allowMethods("PATCH");
        return Client.create();
    }

    private void allowMethods(String... methods) {
        try {
            Field methodsField = HttpURLConnection.class.getDeclaredField("methods");

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);

            methodsField.setAccessible(true);

            String[] oldMethods = (String[]) methodsField.get(null);
            Set<String> methodsSet = new LinkedHashSet<>(Arrays.asList(oldMethods));
            methodsSet.addAll(Arrays.asList(methods));
            String[] newMethods = methodsSet.toArray(new String[0]);

            methodsField.set(null/*static field*/, newMethods);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
