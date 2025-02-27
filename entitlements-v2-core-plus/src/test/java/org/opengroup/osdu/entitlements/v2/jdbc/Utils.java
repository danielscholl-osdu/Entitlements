/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.jdbc;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.SecureRandom;
import java.util.Date;
import lombok.SneakyThrows;

public class Utils {

  public static final String VALID_MOCK_SECRET_STRING;
  public static final String NOT_VALID_MOCK_SECRET_STRING;

  // generate a secret string at runtime to avoid a secret scan triggers
  static {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i <= 127; ++i) {
      sb.append((char) i);
    }
    VALID_MOCK_SECRET_STRING = sb.toString();
    NOT_VALID_MOCK_SECRET_STRING = sb.substring(0, sb.length() / 2);
  }

  @SneakyThrows
  public static String generateJWT(String secret){
    SecureRandom random = new SecureRandom();
    byte[] sharedSecret = new byte[32];
    random.nextBytes(sharedSecret);

    JWSSigner signer = new MACSigner(secret);

    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .subject("testUserName@example.com")
            .issuer("testIssuerId")
            .audience("testClientId")
            .claim("FirstName", "testUserFirstName")
            .claim("Surname", "testUserSecondName")
            .claim("email", "testUserName@example.com")
            .issueTime(new Date())
            .expirationTime(new Date(new Date().getTime() + 60 * 1000))
            .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
    signedJWT.sign(signer);
    return signedJWT.serialize();
  }
}
