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
# THIS SCRIPT MUST BE RUN FROM THE ROOT FOLDER OF THE ENTITLEMENTS V2 SERVICE

set -e

OUTPUT_DIR="${OUTPUT_DIR:-dist}"

echo "--Copying Entitlements V2 Boostrap Scripts to ${OUTPUT_DIR}--"

rm -rf "${OUTPUT_DIR}/devops"

mkdir -p "${OUTPUT_DIR}/devops/aws"

cp -r devops/aws/ "${OUTPUT_DIR}/devops/"

mv ${OUTPUT_DIR}/devops/aws/chart ${OUTPUT_DIR}/
