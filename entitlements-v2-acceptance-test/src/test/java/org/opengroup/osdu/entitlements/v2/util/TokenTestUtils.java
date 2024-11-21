package org.opengroup.osdu.entitlements.v2.util;

import com.google.common.base.Strings;

public class TokenTestUtils extends TestUtils {

    public static final String INTEGRATION_TESTER_TOKEN = "PRIVILEGED_USER_TOKEN";
    public static final String NO_DATA_ACCESS_TOKEN = "NO_ACCESS_USER_TOKEN";
    public static final String INTEGRATION_TESTER_EMAIL = "INTEGRATION_TESTER_EMAIL";
    protected static String token = null;
    protected static String userId = null;
    private OpenIDTokenProvider openIDTokenProvider;

    public TokenTestUtils() {
        token = System.getProperty(INTEGRATION_TESTER_TOKEN, System.getenv(INTEGRATION_TESTER_TOKEN));
        noDataAccesstoken = System.getProperty(NO_DATA_ACCESS_TOKEN, System.getenv(NO_DATA_ACCESS_TOKEN));
        userId = System.getProperty(INTEGRATION_TESTER_EMAIL, System.getenv(INTEGRATION_TESTER_EMAIL));

        if (Strings.isNullOrEmpty(token) || Strings.isNullOrEmpty(noDataAccesstoken)) {
            openIDTokenProvider = new OpenIDTokenProvider();
            token = openIDTokenProvider.getToken().getValue();
            userId = openIDTokenProvider.getToken().getUserId();
        }
    }

    @Override
    public synchronized String getToken() throws Exception {
        if (Strings.isNullOrEmpty(token)) {
            token = openIDTokenProvider.getToken().getValue();
        }
        return token;
    }

    @Override
    public synchronized String getUserId() throws Exception {
        if (Strings.isNullOrEmpty(userId)) {
            userId = openIDTokenProvider.getToken().getUserId();
        }
        return userId;
    }

    @Override
    public synchronized String getNoDataAccessToken() throws Exception {
        if (Strings.isNullOrEmpty(noDataAccesstoken)) {
            noDataAccesstoken = openIDTokenProvider.getNoAccToken().getValue();
        }
        return noDataAccesstoken;
    }
}
