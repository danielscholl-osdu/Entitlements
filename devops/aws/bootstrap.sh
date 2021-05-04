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
#Required Env Variables

#AWS_BASE_URL
#AWS_DEPLOYMENTS_SUBDIR
#AWS_REGION

pip3 install -r $AWS_DEPLOYMENTS_SUBDIR/requirements.txt

echo $AWS_BASE_URL
export AWS_ENTITLEMENTSV2_SERVICE_URL=$AWS_BASE_URL/api/entitlements/v2/

if [ -z "$BEARER_TOKEN" ];
then BEARER_TOKEN=`python $AWS_DEPLOYMENTS_SUBDIR/Token.py`;
export BEARER_TOKEN=$BEARER_TOKEN
fi
export APP_KEY=""
export DATA_PARTITION=opendes

python $AWS_DEPLOYMENTS_SUBDIR/initTenant.py -u $AWS_ENTITLEMENTSV2_SERVICE_URL -t $DATA_PARTITION -p $AWS_DEPLOYMENTS_SUBDIR
