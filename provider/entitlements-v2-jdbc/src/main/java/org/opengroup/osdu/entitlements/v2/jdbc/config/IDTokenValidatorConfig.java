/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.jdbc.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.IapConfigurationProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntitlementsOpenIdProviderConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
@RequiredArgsConstructor
public class IDTokenValidatorConfig {

  private final EntOpenIDProviderConfig entOpenIDProviderConfig;
  private final EntitlementsOpenIdProviderConfigurationProperties openIdConfigurationProperties;

  @Bean
  @ConditionalOnExpression(value = "'${gcp-authentication-mode}' != 'IAP'")
  public Map<String, IDTokenValidator> getOpenIdValidatorsMap() {
    return openIdConfigurationProperties.getClientIds().stream()
        .collect(Collectors.toMap(clientId -> clientId, this::createIdTokenValidator));
  }

  private IDTokenValidator createIdTokenValidator(String clientId) {
    Issuer iss = entOpenIDProviderConfig.getProviderMetadata().getIssuer();
    URL jwkSetURL = getJwkSetURL(entOpenIDProviderConfig.getProviderMetadata().getJWKSetURI());
    ClientID clientID = new ClientID(clientId);
    JWSAlgorithm jwsAlg = JWSAlgorithm.parse(openIdConfigurationProperties.getAlgorithm());
    return new IDTokenValidator(iss, clientID, jwsAlg, jwkSetURL);
  }

  @Bean
  @ConditionalOnProperty(value = "gcp-authentication-mode", havingValue = "IAP")
  public Map<String, IDTokenValidator> getIapValidatorsMap(IapConfigurationProperties properties) {
    Issuer iss = new Issuer(properties.getIssuerUrl());
    URL jwkSetURL = getJwkSetURL(URI.create(properties.getJwkUrl()));
    String clientId = properties.getAud();
    JWSAlgorithm jwsAlgorithm = JWSAlgorithm.parse(properties.getAlgorithm());
    ClientID clientID = new ClientID(clientId);
    IDTokenValidator idTokenValidator =
        new IDTokenValidator(iss, clientID, jwsAlgorithm, jwkSetURL);
    return Collections.singletonMap(clientId, idTokenValidator);
  }

  private URL getJwkSetURL(URI jwkSetURI) {
    try {
      return jwkSetURI.toURL();
    } catch (MalformedURLException e) {
      throw new AppException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
          "Cannot read jwkSetUrl from properties");
    }
  }
}
