package org.opengroup.osdu.entitlements.v2.util;

import com.google.gson.Gson;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.opengroup.osdu.entitlements.v2.acceptance.AcceptanceBaseTest;


public abstract class TestUtils {

    protected static String token = null;
    protected static String noDataAccesstoken = null;
    protected static String dataRootToken = null;

    private static Gson gson = new Gson();

    public abstract String getToken() throws Exception;

    public abstract String getUserId() throws Exception;

    public abstract String getNoDataAccessToken() throws Exception;

    public abstract String getDataRootToken() throws Exception;

    private static void log(String method, String url, Map<String, String> headers, String body) {
        System.out.println(String.format("%s: %s", method, url));
        System.out.println(body);
    }
}
