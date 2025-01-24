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

# This script prepares the dist directory for the integration tests.
# Must be run from the root of the repostiory

# This script executes the test and copies reports to the provided output directory
# To call this script from the service working directory
# ./dist/testing/integration/build-aws/run-tests.sh "./reports/"


SCRIPT_SOURCE_DIR=$(dirname "$0")
echo "Script source location"
echo "$SCRIPT_SOURCE_DIR"
(cd "$SCRIPT_SOURCE_DIR"/../bin && ./install-deps.sh)

#### ADD REQUIRED ENVIRONMENT VARIABLES HERE ###############################################
# The following variables are automatically populated from the environment during integration testing
# see os-deploy-aws/build-aws/integration-test-env-variables.py for an updated list



export ENTITLEMENTS_URL=$ENTITLEMENT_V2_URL
export PARTITION_API=${PARTITION_BASE_URL}api/partition/v1
export SERVICE_PRINCIPAL_EMAIL=$SERVICE_PRINCIPAL_USER
export AWS_COGNITO_AUTH_FLOW=USER_PASSWORD_AUTH
export AWS_COGNITO_AUTH_PARAMS_PASSWORD=$ADMIN_PASSWORD
export AWS_COGNITO_AUTH_PARAMS_USER_NO_ACCESS=$USER_NO_ACCESS
export TENANT='opendes'
export HEADER_X_USER_ID=$SERVICE_PRINCIPAL_USER
export LOCAL_MODE="${LOCAL_MODE:=false}"
export HEADER_X_USER_ID=$SERVICE_PRINCIPAL_USER
export JAVA_HOME=$JAVA17_HOME
### RUN INTEGRATION TEST #########################################################################

mvn -ntp test -f "$SCRIPT_SOURCE_DIR"/../pom.xml
TEST_EXIT_CODE=$?

#### COPY TEST REPORTS #########################################################################

if [ -n "$1" ]
  then
    mkdir -p "$1"
    cp -R "$SCRIPT_SOURCE_DIR"/../target/surefire-reports "$1"
fi

exit $TEST_EXIT_CODE