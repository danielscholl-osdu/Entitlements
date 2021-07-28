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
import java.net.URL;
import javax.annotation.PostConstruct;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.OpenIdProviderConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@DependsOn({"openIDProviderConfig"})
@Component
public class IDTokenValidatorConfig {

	@Autowired
	private OpenIdProviderConfigurationProperties properties;

	@Autowired
	private OpenIDProviderConfig providerConfig;

	private IDTokenValidator validator;

	@PostConstruct
	public void setUp() throws MalformedURLException {
		Issuer iss = providerConfig.getProviderMetadata().getIssuer();
		URL jwkSetURL = providerConfig.getProviderMetadata().getJWKSetURI().toURL();
		ClientID clientID = new ClientID(properties.getClientId());
		JWSAlgorithm jwsAlg = JWSAlgorithm.RS256;
		validator = new IDTokenValidator(iss, clientID, jwsAlg, jwkSetURL);
	}

	public IDTokenValidator getValidator() {
		return validator;
	}
}
