# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#!/usr/bin/env bash

#Future: Support for using Amazon Cert Manager
# if [ "$1" == "webserver" ] && [ -n $ACM_CERTIFICATE_ARN ];
# then

#   aws acm export-certificate --certificate-arn $ACM_CERTIFICATE_ARN --passphrase $(echo -n 'aws123' | openssl base64 -e) | jq -r '"\(.PrivateKey)"' > ${SSL_KEY_PATH}.enc
#   openssl rsa -in ${SSL_KEY_PATH}.enc -out $SSL_KEY_PATH -passin pass:aws123
#   aws acm get-certificate --certificate-arn $ACM_CERTIFICATE_ARN | jq -r '"\(.CertificateChain)"' > $SSL_CERT_PATH
#   aws acm get-certificate --certificate-arn $ACM_CERTIFICATE_ARN | jq -r '"\(.Certificate)"' >> $SSL_CERT_PATH

# fi

if [ -n $USE_SELF_SIGNED_SSL_CERT ];
then
    mkdir -p $SSL_KEY_STORE_DIR
    pushd $SSL_KEY_STORE_DIR
    keytool -genkeypair -alias $SSL_KEY_ALIAS -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore $SSL_KEY_STORE_NAME -validity 3650 -keypass $SSL_KEY_PASSWORD -storepass $SSL_KEY_PASSWORD -dname "CN=localhost, OU=AWS, O=Energy, L=Houston, ST=TX, C=US"
    popd
fi
