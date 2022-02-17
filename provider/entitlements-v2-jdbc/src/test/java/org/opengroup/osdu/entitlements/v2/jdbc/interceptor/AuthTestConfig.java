package org.opengroup.osdu.entitlements.v2.jdbc.interceptor;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import java.util.Collections;
import java.util.Map;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.jdbc.config.IDTokenValidatorConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.config.EntOpenIDProviderConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntitlementsOpenIdProviderConfigurationProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.config.security.ExternalAuthConfiguration;
import org.opengroup.osdu.entitlements.v2.jdbc.config.security.InternalAuthConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;

@EnableConfigurationProperties
@ComponentScan(
    value = {
      "org.opengroup.osdu.entitlements.v2.jdbc.config",
      "org.opengroup.osdu.entitlements.v2.jdbc.interceptor"
    },
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          value = {
            IDTokenValidatorConfig.class,
            ExternalAuthConfiguration.class,
            InternalAuthConfiguration.class,
            EntOpenIDProviderConfig.class
          })
    })
@Configuration
public class AuthTestConfig {

  public static final String CORRECT_TOKEN =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ0ZXN0SXNzdWVySWQiLCJpYXQiOjE2MzY5NzI0NTAsImV4cCI6MTY2ODUwODQ1MCwiYXVkIjoidGVzdENsaWVudElkIiwic3ViIjoidGVzdFVzZXJOYW1lQGV4YW1wbGUuY29tIiwiRmlyc3ROYW1lIjoidGVzdFVzZXJGaXJzdE5hbWUiLCJTdXJuYW1lIjoidGVzdFVzZXJTZWNvbmROYW1lIiwiZW1haWwiOiJ0ZXN0VXNlck5hbWVAZXhhbXBsZS5jb20iLCJSb2xlIjpbIk1hbmFnZXIiLCJQcm9qZWN0IEFkbWluaXN0cmF0b3IiXX0.KM7ewU34QH55UMCoF-cZjmlEdr8MphTp2np8XKIKiGQ";
  public static final String TOKEN_WITH_NOT_CORRECT_SECRET =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ0ZXN0SXNzdWVySWQiLCJpYXQiOjE2Mzg5NzExNDksImV4cCI6MTY3MDUwNzE0OSwiYXVkIjoidGVzdENsaWVudElkIiwic3ViIjoidGVzdFVzZXJOYW1lQGV4YW1wbGUuY29tIn0.pvVPfim8r5TNyWX8gyzu46CvqLNsuuPP5Ltkt_RgNRc";
  public static final String IAP_EMAIL_PREFIX = "accounts.google.com:";
  public static final String MATCHING_USER_EMAIL = "testUserName@example.com";
  public static final String NOT_MATCHING_USER_EMAIL = "testUserName2@example.com";

  @Bean
  @Primary
  @ConditionalOnProperty(value = "gcp-authentication-mode", havingValue = "INTERNAL")
  public Map<String, IDTokenValidator> getIapValidatorsMap() {
    return Collections.singletonMap(
        "testClientId",
        new IDTokenValidator(
            new Issuer("testIssuerId"),
            new ClientID("testClientId"),
            JWSAlgorithm.HS256,
            new Secret("qwertyuiopasdfghjklzxcvbnm123456")));
  }

  @Bean
  @Primary
  @ConditionalOnExpression(value = "'${gcp-authentication-mode}' != 'INTERNAL'")
  public Map<String, IDTokenValidator> getOpenIdValidatorsMap(
      EntOpenIDProviderConfig entOpenIDProviderConfig, EntitlementsOpenIdProviderConfigurationProperties properties) {
    return Collections.singletonMap(
        "testClientId",
        new IDTokenValidator(
            new Issuer("testIssuerId"),
            new ClientID("testClientId"),
            JWSAlgorithm.HS256,
            new Secret("qwertyuiopasdfghjklzxcvbnm123456")));
  }

  @Bean
  @Primary
  public DpsHeaders getDpsHeaders() {
    return new DpsHeaders();
  }

  @Bean
  @Primary
  public EntOpenIDProviderConfig getEntOpenIDProviderConfig() {
    return new EntOpenIDProviderConfig();
  }
}
