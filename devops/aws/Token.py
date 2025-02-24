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

import os;
import boto3;
import jwt;
import requests;
import base64;
import json;

class AwsToken(object):

    def get_aws_id_token(self):
      if os.getenv("AWS_COGNITO_REGION") is not None:      
          region = os.environ["AWS_COGNITO_REGION"]
      else:
          region = os.environ["AWS_REGION"]

      client = boto3.client('cognito-idp', region_name=region)
      environment = os.environ["RESOURCE_PREFIX"]

      ssm = boto3.client('ssm')
      secretsmanagerclient = boto3.client('secretsmanager')
      tokenUrl = ssm.get_parameter(Name='/osdu/' + environment +'/oauth-token-uri', WithDecryption=True)['Parameter']['Value']
      awsOauthCustomScope = ssm.get_parameter(Name='/osdu/' + environment +'/oauth-custom-scope', WithDecryption=True)['Parameter']['Value']
      client_credentials_clientid = ssm.get_parameter(Name='/osdu/' + environment +'/client-credentials-client-id', WithDecryption=True)['Parameter']['Value']
      client_secret_key = 'client_credentials_client_secret'
      client_secret_secretName = '/osdu/' + environment +'/client_credentials_secret'
      client_credentials_secret=''
      get_secret_value_response = secretsmanagerclient.get_secret_value(SecretId=client_secret_secretName)
      if 'SecretString' in get_secret_value_response:
        secret = json.loads(get_secret_value_response['SecretString'])
        client_credentials_secret=secret['client_credentials_client_secret']


      encodeThisString = client_credentials_clientid+':'+client_credentials_secret
      encodeThisStringBytes=encodeThisString.encode('UTF-8')
      authorizationHeaderContents=base64.b64encode(encodeThisStringBytes)
      a= authorizationHeaderContents.decode("UTF-8")

      headers = {
                  'Content-Type': 'application/x-www-form-urlencoded',
                  'Authorization': 'Basic '+a
                    }

      method = 'POST'
      url = tokenUrl+'?grant_type=client_credentials&client_id='+client_credentials_clientid+'&scope='+awsOauthCustomScope;
      response = requests.request(method,url, headers=headers)

      jsonResponse = response.json()
      for key, value in jsonResponse.items():
        if(key == 'access_token'):
            token = 'Bearer ' + value
      print(token)
      return token

if __name__ == '__main__':
    AwsToken().get_aws_id_token()