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
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntitlementsConfigurationProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.IapConfigurationProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.OpenIdProviderProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@Component
public class IDTokenValidatorFactory {

  private final EntOpenIDProviderConfig entOpenIDProviderConfig;
  private final OpenIdProviderProperties openIdConfigurationProperties;
  private final EntitlementsConfigurationProperties entitlementsConfigurationProperties;
  @Autowired(required = false)
  private IapConfigurationProperties iapConfigurationProperties;

  @Autowired
  public IDTokenValidatorFactory(EntOpenIDProviderConfig entOpenIDProviderConfig,
                                 OpenIdProviderProperties openIdConfigurationProperties,
                                 EntitlementsConfigurationProperties entitlementsConfigurationProperties) {
    this.entOpenIDProviderConfig = entOpenIDProviderConfig;
    this.openIdConfigurationProperties = openIdConfigurationProperties;
    this.entitlementsConfigurationProperties = entitlementsConfigurationProperties;
  }

  public IDTokenValidator createTokenValidator(String clientId) {
    if (entitlementsConfigurationProperties.getGcpAuthenticationMode().equals("IAP")) {
      return createIapTokenValidator(clientId);
    } else {
      return createIdTokenValidator(clientId);
    }
  }

  private IDTokenValidator createIdTokenValidator(String clientId) {
    Issuer iss = entOpenIDProviderConfig.getProviderMetadata().getIssuer();
    URL jwkSetURL = getJwkSetURL(entOpenIDProviderConfig.getProviderMetadata().getJWKSetURI());
    ClientID clientID = new ClientID(clientId);
    JWSAlgorithm jwsAlg = JWSAlgorithm.parse(openIdConfigurationProperties.getAlgorithm());
    return new IDTokenValidator(iss, clientID, jwsAlg, jwkSetURL);
  }

  private IDTokenValidator createIapTokenValidator(String clientId) {
    Issuer iss = new Issuer(iapConfigurationProperties.getIssuerUrl());
    URL jwkSetURL = getJwkSetURL(URI.create(iapConfigurationProperties.getJwkUrl()));
    JWSAlgorithm jwsAlgorithm = JWSAlgorithm.parse(iapConfigurationProperties.getAlgorithm());
    ClientID clientID = new ClientID(clientId);
    return new IDTokenValidator(iss, clientID, jwsAlgorithm, jwkSetURL);
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
