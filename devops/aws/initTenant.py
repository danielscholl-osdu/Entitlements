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
import argparse
import os
import requests

parser = argparse.ArgumentParser()
parser.add_argument('-u', help='The complete URL to the Entitlements V2 Service.', default=None)
parser.add_argument('-t', help='The tenant for which groups are being provisioned.', default=None)
arguments = parser.parse_args()
if arguments.u is not None:
    url = arguments.u
if arguments.t is not None:
    tenant = arguments.t
token = os.environ.get('BEARER_TOKEN')
initTenantUrl=url+'tenant-provisioning'

#call init API to provision groups for Service Principal


headers = {
            'data-partition-id': tenant,
            'Content-Type': 'application/json',
            'Authorization': token
        }
method = 'POST'
response = requests.request(method,initTenantUrl, headers=headers)
print(response.status_code)
if response.status_code==200:
    print('The Entitlements V2 bootstrapping successful for tenant:'+tenant)
else:
    print('The Entitlements V2 bootstrapping failed for tenant:'+tenant)

