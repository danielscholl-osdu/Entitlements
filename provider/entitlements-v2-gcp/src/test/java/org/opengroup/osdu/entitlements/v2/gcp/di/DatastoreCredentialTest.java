package org.opengroup.osdu.entitlements.v2.gcp.di;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.SignJwtRequest;
import com.google.api.services.iam.v1.model.SignJwtResponse;
import com.google.auth.oauth2.AccessToken;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GoogleCredential.class, GoogleNetHttpTransport.class, NetHttpTransport.class})
public class DatastoreCredentialTest {
    private static final String SIGNER_SERVICE_ACCOUNT = "tokencreator@serviceaccount.com";
    private static final String SIGNED_JWT_VALUE = "SIGNED JWT";

    private TenantInfo tenant;

    private DatastoreCredential sut;

    @Before
    public void setup() throws Exception {
        mockStatic(GoogleCredential.class);
        mockStatic(GoogleNetHttpTransport.class);

        this.tenant = new TenantInfo();
        this.tenant.setServiceAccount(SIGNER_SERVICE_ACCOUNT);

        NetHttpTransport transport = mock(NetHttpTransport.class);
        when(GoogleNetHttpTransport.newTrustedTransport()).thenReturn(transport);

        GoogleCredential credential = mock(GoogleCredential.class);
        when(GoogleCredential.getApplicationDefault()).thenReturn(credential);

        this.sut = new DatastoreCredential(this.tenant);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_returnAccessToken_when_refreshingTokenSuccessfullyForUser() throws Exception {
        SignJwtResponse signJwtResponse = new SignJwtResponse();
        signJwtResponse.setSignedJwt(SIGNED_JWT_VALUE);

        Iam.Projects.ServiceAccounts.SignJwt signJwt = mock(Iam.Projects.ServiceAccounts.SignJwt.class);
        when(signJwt.execute()).thenReturn(signJwtResponse);

        ArgumentCaptor<String> serviceAccountCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SignJwtRequest> payloadCaptor = ArgumentCaptor.forClass(SignJwtRequest.class);

        Iam.Projects.ServiceAccounts serviceAccounts = mock(Iam.Projects.ServiceAccounts.class);
        when(serviceAccounts.signJwt(serviceAccountCaptor.capture(), payloadCaptor.capture())).thenReturn(signJwt);

        Iam.Projects projects = mock(Iam.Projects.class);
        when(projects.serviceAccounts()).thenReturn(serviceAccounts);

        Iam iam = mock(Iam.class);
        when(iam.projects()).thenReturn(projects);

        this.sut.setIam(iam);

        AccessToken resultingAccessToken = this.sut.refreshAccessToken();
        assertEquals(SIGNED_JWT_VALUE, resultingAccessToken.getTokenValue());
        assertTrue(
                resultingAccessToken.getExpirationTime().getTime() > DateUtils.addSeconds(new Date(), 3590).getTime());
        assertTrue(
                resultingAccessToken.getExpirationTime().getTime() < DateUtils.addSeconds(new Date(), 3750).getTime());
        assertEquals(String.format("projects/-/serviceAccounts/%s", SIGNER_SERVICE_ACCOUNT),
                serviceAccountCaptor.getValue());

        Map<String, Object> payloadMap = new ObjectMapper().readValue(payloadCaptor.getValue().getPayload(),
                HashMap.class);
        assertEquals(4, payloadMap.size());
        assertEquals(SIGNER_SERVICE_ACCOUNT, payloadMap.get("iss"));
        assertEquals(SIGNER_SERVICE_ACCOUNT, payloadMap.get("sub"));
        assertEquals("https://datastore.googleapis.com/google.datastore.v1.Datastore", payloadMap.get("aud"));
        assertTrue(System.currentTimeMillis() / 1000 >= (int) payloadMap.get("iat"));
    }

    @Test(expected = RuntimeException.class)
    public void should_returnNull_when_refreshingTokenHasException() {
        AccessToken resultingAccessToken = this.sut.refreshAccessToken();
        assertNull(resultingAccessToken);
    }
}