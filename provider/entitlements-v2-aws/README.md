# Entitlements V2 service
The OSDU on AWS energy data platform supports API and data authorization via the Entitlements service.
The users are assigned to service and data groups through which users gain access to APIs and data. The Entitlements service provides CRUD operations to manage groups and its members.
Entitlements Service supports hierarchical groups using a graph model.
entitlement-v2-aws stores groups/users in MongoDB.

## Running Locally

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites


* JDK 8 (https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html)
* Lombok 1.16 or later
* Maven
* Install MongoDB on your machine by following this [link](https://docs.mongodb.com/v4.0/installation/)
* You may install mongo compass which is a MongoDB GUI using this [link](https://www.mongodb.com/products/compass)
* Navigate to where the mongo sever is installed and start the server using:
``C:\Program Files\MongoDB\Server\5.0\bin>mongod``

The server will start on the default port 27017
* Launch MongoDB Compass and create a new connection to localhost: 27017


![New Connection](docs/img/newconn.png)

* Next add a db user using mongo shell using the following commands:

![Add new db user](docs/img/mongo_createuser.png)

### Installation

In order to run the service locally or remotely, you will need to have the following environment variables defined.

| name | example value | required | description | sensitive? |
| ---  | ---   | ---         | ---        | ---    |
| `LOCAL_MODE` | `true` | yes | Set to 'true' to use env vars in place of the k8s variable resolver | no |
| `APPLICATION_PORT` | `8080` | yes | The port the service will be hosted on. | no |
| `AWS_REGION` | `us-east-1` | yes | The region where resources needed by the service are deployed | no |
| `AWS_ACCESS_KEY_ID` | `ASIAXXXXXXXXXXXXXX` | yes | The AWS Access Key for a user with access to Backend Resources required by the service | yes |
| `AWS_SECRET_ACCESS_KEY` | `super-secret-key==` | yes | The AWS Secret Key for a user with access to Backend Resources required by the service | yes |
| `AWS_SESSION_TOKEN` | `session-token-xxxxxxxxxx` | no | AWS Session token needed if using an SSO user session to authenticate | yes |
| `ENVIRONMENT` | `osdu-prefix` | yes | The Resource Prefix defined during deployment | no |
| `LOG_LEVEL` | `DEBUG` | yes | The Log Level severity to use (https://www.tutorialspoint.com/log4j/log4j_logging_levels.htm) | no |
| `SSM_ENABLED` | `true` | yes | Set to 'true' to use SSM to resolve config properties, otherwise use env vars | no |
| `SSL_ENABLED` | `false` | no | Set to 'false' to disable SSL for local development | no |
| `MONGODB_ENDPOINT` | `localhost` or `https://some-hosted-url` | yes | Specify the base url for mongo server | no |
| `MONGODB_USE_SRV_ENDPOINT` | `false` or `true` | yes | To run the service locally, set this to false | no | 
| `MONGODB_PORT` | `27017` | no | Specify the port on which the mongo server is running. Default is 27017 | no |
| `MONGODB_AUTH_DATABASE` | `admin` | no | Specify the database name | no |
| `MONGODB_USERNAME` | `admin` | yes | Specify the username on the running mongo server | no |
| `MONGODB_PASSWORD` | `admin` | yes | Specify the password on the running mongo server | no |

### Run Locally
Check that maven is installed:

example:
```bash
$ mvn --version
Apache Maven 3.8.3 (ff8e977a158738155dc465c6a97ffaf31982d739)
Maven home: /usr/local/Cellar/maven/3.8.3/libexec
Java version: 1.8.0_312, vendor: Amazon.com Inc., runtime: /Library/Java/JavaVirtualMachines/amazon-corretto-8.jdk/Contents/Home/jre
...
```

You may need to configure access to the remote maven repository that holds the OSDU dependencies. Copy one of the below files' content to your .m2 folder
* For development against the OSDU GitLab environment, leverage: `<REPO_ROOT>~/.mvn/community-maven.settings.xml`
* For development in an AWS Environment, leverage: `<REPO_ROOT>/provider/entitlements-v2-aws/maven/settings.xml`

* Navigate to the service's root folder and run:

```bash
mvn clean package -pl entitlements-v2-core,provider/entitlements-v2-aws
```

* If you wish to build the project without running tests

```bash
mvn clean package -pl entitlements-v2-core,provider/entitlements-v2-aws -DskipTests
```

After configuring your environment as specified above, you can follow these steps to run the application. These steps should be invoked from the *repository root.*
<br/>
<br/>
NOTE: Replace `*` with version numbers as defined in the app pom.xml if not on osx/linux

```bash
java -jar provider/entitlements-v2-aws/target/entitlements-v2-aws-*.*.*-SNAPSHOT-spring-boot.jar
```

## Testing
 
 ### Running Integration Tests 
 This section describes how to run OSDU Integration tests (testing/entitlements-v2-test-aws).
 
* Before you begin accessing the APIs of this service, you need to first initialize a tenant with the entitlements groups.
  To do this, follow these steps: 
  1. Open a bash shell and set the following environment variables:
 ```
  export AWS_REGION=<YOUR_OSDU_DEPLOYED_REGION> (example:us-east-1)
  export RESOURCE_PREFIX=<YOUR_OSDU_ENVIRONMENT_RESOURCE_PREFIX>  (example: osdu-deshruchdemo)
  export LOCAL_MODE=true  
  export AWS_BASE_URL=http://localhost:8082  
  export AWS_ACCESS_KEY_ID=<YOUR_AWS_ACCESS_KEY_ID>  
  export AWS_SECRET_ACCESS_KEY=<YOUR_AWS_SECRET_ACCESS_KEY>  
  export AWS_SESSION_TOKEN=<YOUR_AWS_SESSION_TOKEN>
  export $AWS_DEPLOYMENTS_SUBDIR=./devops/aws    
```
 3. Run the following command to initialize a tenant for service account:
   ```
   curl --location --request POST 'http://localhost:8080/api/entitlements/v2/tenant-provisioning' \
   --header 'data-partition-id: <YOUR_DATA_PARTITION_ID>' \
   --header 'Content-Type: application/json' \   
   --header 'Authorization: Bearer <YOUR_BEARER_TOKEN_FOR_SERVICE_PRINCIPAL>'
   ```
   Alternatively, look up the bootstrapping script in the devops/aws. This script initializes groups for three users used for 
   integration tests. 

```
  ./devops/aws/bootstrap.sh
  ```
  Re-running this script multiple times will not cause any undesired effects.
  
  After the successful execution of this script, you should see the following using MongoDB Compass:
  
  ![Groups](docs/img/mongodb_groups.png)
  
  
  
  
  ![Users](docs/img/mongodb_users.png)
  

  
* Next, you will need to have the following environment variables defined.
 
 | name | example value | description | sensitive?
 | ---  | ---   | ---         | ---        |
 | `AWS_ACCESS_KEY_ID` | `ASIAXXXXXXXXXXXXXX` | The AWS Access Key for a user with access to Backend Resources required by the service | yes |
 | `AWS_SECRET_ACCESS_KEY` | `super-secret-key==` | The AWS Secret Key for a user with access to Backend Resources required by the service | yes |
 | `AWS_SESSION_TOKEN` | `session-token-xxxxxxxxx` | AWS Session token needed if using an SSO user session to authenticate | yes |
 | `ENTITLEMENT_V2_URL` | `http://localhost:8080/api/entitlements/v2/` or `https:deployed-url` | specify url of entitlements service.  | no |
 | `LOCAL_MODE` | `true` | Set to 'true' to use in local mode | no |
 | `RESOURCE_PREFIX` | `osdu-prefix` | The Resource Prefix defined during deployment. This is used to generate the serviceprincipal token to run tests | no |
 | `AWS_REGION` | `us-east-1` | The region where resources needed by the service are deployed | no |
 
 **Creating a new user to use for integration tests**
 ```
 aws cognito-idp admin-create-user --user-pool-id ${AWS_COGNITO_USER_POOL_ID} --username ${AWS_COGNITO_AUTH_PARAMS_USER} --user-attributes Name=email,Value=${AWS_COGNITO_AUTH_PARAMS_USER} Name=email_verified,Value=True --message-action SUPPRESS

 aws cognito-idp initiate-auth --auth-flow ${AWS_COGNITO_AUTH_FLOW} --client-id ${AWS_COGNITO_CLIENT_ID} --auth-parameters USERNAME=${AWS_COGNITO_AUTH_PARAMS_USER},PASSWORD=${AWS_COGNITO_AUTH_PARAMS_PASSWORD}
 ```
 
 
 Execute following command to build code and run all the integration tests:

### Run Tests simulating Pipeline
```bash
./testing/entitlements-v2-test-aws/build-aws/prepare-dist.sh

#Set Neccessary ENV Vars here as defined in run-tests.sh

./dist/testing/integration/build-aws/run-tests.sh
```

### Run Tests using mvn
Set required env vars and execute the following:
```
mvn clean package -f testing/pom.xml -pl entitlements-v2-test-core,entitlements-v2-test-aws
mvn test -f testing/entitlements-v2-test-aws/pom.xml
```

## License
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.