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
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.IapConfigurationProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.OpenIdProviderConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IDTokenValidatorConfig {

    @Bean
    @Qualifier("opendIdValidator")
    public IDTokenValidator getOpenIdValidator(OpenIDProviderConfig openIDProviderConfig, OpenIdProviderConfigurationProperties properties)
        throws MalformedURLException {
        Issuer iss = openIDProviderConfig.getProviderMetadata().getIssuer();
        URL jwkSetURL = openIDProviderConfig.getProviderMetadata().getJWKSetURI().toURL();
        ClientID clientID = new ClientID(properties.getClientId());
        JWSAlgorithm jwsAlg = JWSAlgorithm.parse(properties.getAlgorithm());
        return new IDTokenValidator(iss, clientID, jwsAlg, jwkSetURL);
    }

    @Bean
    @Qualifier("iapValidator")
    public IDTokenValidator getIapValidator(IapConfigurationProperties properties) throws MalformedURLException {
        Issuer iss = new Issuer(properties.getIssuerUrl());
        URL jwkSetURL = URI.create(properties.getJwkUrl()).toURL();
        String clientId = properties.getAud();
        JWSAlgorithm jwsAlgorithm = JWSAlgorithm.parse(properties.getAlgorithm());
        ClientID clientID = new ClientID(clientId);
        return new IDTokenValidator(iss, clientID, jwsAlgorithm, jwkSetURL);
    }
}
