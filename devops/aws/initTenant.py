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
import json


parser = argparse.ArgumentParser()
parser.add_argument('-u', help='The complete URL to the Entitlements V2 Service.', default=None)
parser.add_argument('-t', help='The tenant for which groups are being provisioned.', default='opendes')
parser.add_argument('-c', help='The common data partition name for which groups are being provisioned.', default='common')
parser.add_argument('-p', help='Path to where the groups json is stored.', default=None)
parser.add_argument('-i', help='Integration test user email', default='admin@testing.com')
parser.add_argument('-l', help='Integration test limited access user email', default='noaccess@testing.com')
parser.add_argument('-d', help='Groups domain', default='contoso.com')
arguments = parser.parse_args()
if arguments.u is not None:
    url = arguments.u
if arguments.t is not None:
    tenant = arguments.t
if arguments.c is not None:
    common_tenant = arguments.c
if arguments.p is not None:
    groups_json_path = arguments.p
if arguments.i is not None:
    integration_test_user=arguments.i
if arguments.l is not None:
    integration_test_limited_access_user=arguments.l
if arguments.d is not None:
    group_domain=arguments.d
token = os.environ.get('BEARER_TOKEN')
initTenantUrl=url+'tenant-provisioning'

#call init API to provision groups for Service Principal for opendes tenant
print(token)
print(initTenantUrl)
headers = {
            'data-partition-id': tenant,
            'Content-Type': 'application/json',
            'Authorization': token
        }
print(headers)
method = 'POST'
response = requests.request(method,initTenantUrl, headers=headers)
print(response.status_code)
if response.status_code==200:
    print('The Entitlements V2 bootstrapping for ServicePrincipal successful for tenant:'+tenant)
else:
    print('The Entitlements V2 bootstrapping for ServicePrincipal failed for tenant:'+tenant)

#call init API to provision groups for Service Principal for common tenant

headers = {
            'data-partition-id': common_tenant,
            'Content-Type': 'application/json',
            'Authorization': token
        }
method = 'POST'
response = requests.request(method,initTenantUrl, headers=headers)
print(response.status_code)
if response.status_code==200:
    print('The Entitlements V2 bootstrapping for ServicePrincipal successful for tenant:'+common_tenant)
else:
    print('The Entitlements V2 bootstrapping for ServicePrincipal failed for tenant:'+common_tenant)


#call Add Member API to provision groups for Integration test user for opendes partition
headers = {
            'data-partition-id': tenant,
            'Content-Type': 'application/json',
            'Authorization': token
        }
method = 'POST'
data = {'email': integration_test_user, 'role':'MEMBER'}
f = open(groups_json_path+'/groups_integration_test_user_opendes.json')
groups_data = json.load(f)
f.close()

for i in groups_data:
    arr = groups_data[i]
    for group_name in arr:
        group_email= group_name+'@'+tenant+'.'+group_domain
        addMemberUrl=url+'groups/'+group_email+'/members'
        response = requests.request(method,addMemberUrl, headers=headers, data=json.dumps(data))
        if response.status_code==200:
            print('The Entitlements V2 bootstrapping for Integration test user successful for group_email:'+group_email)
        elif response.status_code==409:
                print('The Entitlements V2 group for Integration test user already exists for group_email:'+group_email)
        else:
            print('The Entitlements V2 bootstrapping for Integration test user failed for group_email:'+group_email+'Error response code: '+str(response.status_code))


#call Add Member API to provision groups for Integration test user for common data partition
headers = {
            'data-partition-id': common_tenant,
            'Content-Type': 'application/json',
            'Authorization': token
        }
method = 'POST'
data = {'email': integration_test_user, 'role':'MEMBER'}
f = open(groups_json_path+'/groups_integration_test_user_common.json')
groups_data = json.load(f)
f.close()

for i in groups_data:
    arr = groups_data[i]
    for group_name in arr:
        group_email= group_name+'@'+common_tenant+'.'+group_domain
        addMemberUrl=url+'groups/'+group_email+'/members'
        response = requests.request(method,addMemberUrl, headers=headers, data=json.dumps(data))
        if response.status_code==200:
            print('The Entitlements V2 bootstrapping for Integration test user successful for group_email:'+group_email)
        elif response.status_code==409:
                print('The Entitlements V2 group for Integration test user already exists for group_email:'+group_email)
        else:
            print('The Entitlements V2 bootstrapping for Integration test user failed for group_email:'+group_email+'Error response code: '+str(response.status_code))


#call Add Member API to provision groups for Integration test limited access user
headers = {
            'data-partition-id': tenant,
            'Content-Type': 'application/json',
            'Authorization': token
        }
method = 'POST'
data = {'email': integration_test_limited_access_user, 'role':'OWNER'}
f = open(groups_json_path+'/groups_integration_test_limited_access_user_opendes.json')
groups_data = json.load(f)
f.close()

for i in groups_data:
    arr = groups_data[i]
    for group_name in arr:
        group_email= group_name+'@'+tenant+'.'+group_domain
        addMemberUrl=url+'groups/'+group_email+'/members'
        response = requests.request(method,addMemberUrl, headers=headers, data=json.dumps(data))
        if response.status_code==200:
            print('The Entitlements V2 bootstrapping for Integration test limited access user successful for group_email:'+group_email)
        elif response.status_code==409:
                print('The Entitlements V2 group for Integration test limited access user already exists for group_email:'+group_email)
        else:
            print('The Entitlements V2 bootstrapping for IIntegration test limited access user failed for group_email:'+group_email+'Error response code: '+str(response.status_code))
