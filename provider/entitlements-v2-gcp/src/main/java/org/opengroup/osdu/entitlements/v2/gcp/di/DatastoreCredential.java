package org.opengroup.osdu.entitlements.v2.gcp.di;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts.SignJwt;
import com.google.api.services.iam.v1.model.SignJwtRequest;
import com.google.api.services.iam.v1.model.SignJwtResponse;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.time.DateUtils;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;

public class DatastoreCredential extends GoogleCredentials {

    private static final long serialVersionUID = 8344377091688956815L;
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private transient Iam iam;

    private final transient TenantInfo tenant;

    public DatastoreCredential(TenantInfo tenant) {
        this.tenant = tenant;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public AccessToken refreshAccessToken() {

        try {
            SignJwtRequest signJwtRequest = new SignJwtRequest();
            signJwtRequest.setPayload(this.getPayload());

            String serviceAccountName = String.format("projects/-/serviceAccounts/%s", this.tenant.getServiceAccount());

            SignJwt signJwt = this.getIam().projects().serviceAccounts().signJwt(serviceAccountName, signJwtRequest);

            SignJwtResponse signJwtResponse = signJwt.execute();
            String signedJwt = signJwtResponse.getSignedJwt();

            return new AccessToken(signedJwt, DateUtils.addSeconds(new Date(), 3600));

        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "An error occurred when accessing third-party APIs", e);
        }
    }

    protected void setIam(Iam iam) {
        this.iam = iam;
    }

    private String getPayload() {
        JsonObject payload = new JsonObject();
        payload.addProperty("iss", this.tenant.getServiceAccount());
        payload.addProperty("sub", this.tenant.getServiceAccount());
        payload.addProperty("aud", "https://datastore.googleapis.com/google.datastore.v1.Datastore");
        payload.addProperty("iat", System.currentTimeMillis() / 1000);

        return payload.toString();
    }

    private Iam getIam() throws GeneralSecurityException, IOException {
        if (this.iam == null) {
            Iam.Builder builder = new Iam.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY,
                    new HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault()))
                    .setApplicationName("DPS Storage Service");
            this.iam = builder.build();
        }
        return this.iam;
    }
}
