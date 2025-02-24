package org.opengroup.osdu.entitlements.v2.ibm.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import java.net.HttpURLConnection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Component
public class KeyCloakProvider {

    @Value("${ibm.keycloak.endpoint_url}")
    private String url;

    @Value("${ibm.keycloak.realm}")
    private String realm ;

    @Value("${ibm.keycloak.grant_type:password}")
    private String grantType;

    @Value("${ibm.keycloak.scope:openid}")
    private String scope;

    @Value("${ibm.keycloak.client_id}")
    private String clientId;

    @Value("${ibm.keycloak.client_secret}")
    private String clientSecret;

    @Value("${partition.keycloak.accept_http:false}")
    private boolean accept_kc_http;


    public String getToken(String user, String password) throws IOException {
        String endpoint = String.format("https://%s/auth/realms/%s/protocol/openid-connect/token", url, realm);
        if(accept_kc_http)
        {
            endpoint = String.format("http://%s/auth/realms/%s/protocol/openid-connect/token", url, realm);

        }

        URL url = new URL(endpoint);
        HttpURLConnection  con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("grant_type", grantType);
        parameters.put("client_id", clientId);
        parameters.put("client_secret", clientSecret);
        parameters.put("username", user);
        parameters.put("password", password);
        parameters.put("scope", scope);

        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.writeBytes(getParamsString(parameters));
        out.flush();
        out.close();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        con.disconnect();

        Gson gson = new Gson();
        JsonObject jobj = gson.fromJson(content.toString(), JsonObject.class);
        String token = jobj.get("access_token").getAsString();
        return token;
    }

    private static String getParamsString(Map<String, String> params)
            throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }

}
