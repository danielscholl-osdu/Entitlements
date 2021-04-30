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

if [ -n $USE_SELF_SIGNED_SSL_CERT ];
then    
    export SSL_KEY_PASSWORD=$RANDOM$RANDOM$RANDOM;
    export SSL_KEY_STORE_PASSWORD=$SSL_KEY_PASSWORD;
    export SSL_KEY_STORE_DIR=/tmp/certs;
    export SSL_KEY_STORE_NAME=osduonaws.p12;
    export SSL_KEY_STORE_PATH=$SSL_KEY_STORE_DIR/$SSL_KEY_STORE_NAME;
    export SSL_KEY_ALIAS=osduonaws;
    
    ./ssl.sh;
fi

java $JAVA_OPTS -jar /app.jar