# entitlements-v2-azure

entitlements-v2-azure is a [Spring Boot](https://spring.io/projects/spring-boot) service that hosts CRUD APIs that enable management of user entitlements.

## Running Locally

### Requirements

In order to run this service locally, you will need the following:

- [Maven 3.6.0+](https://maven.apache.org/download.cgi)
- [AdoptOpenJDK8](https://adoptopenjdk.net/)
- Infrastructure dependencies, deployable through the relevant [infrastructure template](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_git/infrastructure-templates?path=%2Finfra&version=GBmaster&_a=contents)
- While not a strict dependency, example commands in this document use [bash](https://www.gnu.org/software/bash/)

### General Tips

**Environment Variable Management**
The following tools make environment variable configuration simpler
 - [direnv](https://direnv.net/) - for a shell/terminal environment
 - [EnvFile](https://plugins.jetbrains.com/plugin/7861-envfile) - for [Intellij IDEA](https://www.jetbrains.com/idea/)

**Lombok**
This project uses [Lombok](https://projectlombok.org/) for code generation. You may need to configure your IDE to take advantage of this tool.
 - [Intellij configuration](https://projectlombok.org/setup/intellij)
 - [VSCode configuration](https://projectlombok.org/setup/vscode)

### Environment Variables

In order to run the service locally, you will need to have the following environment variables defined.

**Note** The following command can be useful to pull secrets from keyvault:
```bash
az keyvault secret show --vault-name $KEY_VAULT_NAME --name $KEY_VAULT_SECRET_NAME --query value -otsv
```

**Required to run service**

 TODO: Update this section when infrastructure changes will be in place

In Order to run service with Istio authentication, add below environment variables. This is needed only to test Istio filter scenarios,
with these settings service expects "x-payload" header which contains Base64 encoded format of Payload. In this approach service will not do Authentication.

 name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `azure_istioauth_enabled` | `true` | Flag to Disable AAD auth | no | -- |

**Required to run integration tests**

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `ENTITLEMENT_V2_URL` | ex `http://localhost:8080/api/entitlements/v2` | The host where the service is running | no | -- |
| `INTEGRATION_TESTER` | `********` | System identity to assume for API calls. Note: this user must have entitlements configured already | no | -- |
| `AZURE_TESTER_SERVICEPRINCIPAL_SECRET` | `********` | Secret for `$INTEGRATION_TESTER` | yes | -- |
| `AZURE_AD_TENANT_ID` | `********` | AD tenant to authenticate users from | yes | -- |
| `AZURE_AD_APP_RESOURCE_ID` | `********` | AAD client application ID | yes | output of infrastructure deployment |
| `NO_DATA_ACCESS_TESTER` | `********` | Service principal ID of a service principal without entitlements | yes | `osdu-infra-test-app-noaccess-id` secret from keyvault |
| `NO_DATA_ACCESS_TESTER_SERVICEPRINCIPAL_SECRET` | `********` | Secret for `$NO_DATA_ACCESS_TESTER` | yes | `osdu-infra-test-app-noaccess-key` secret from keyvault |

### Build and run the application

After configuring your environment as specified above, you can follow these steps to build and run the application. These steps should be invoked from the *repository root.*

```bash
# build + test + install core service code
$ ./mvnw clean install

# run service
#
# Note: this assumes that the environment variables for running the service as outlined
#       above are already exported in your environment.
$ java -jar $(find provider/entitlements-v2-azure/target/ -name '*-spring-boot.jar')

# Alternately you can run using the Maven Task
$ ./mvnw spring-boot:run -pl provider/entitlements-v2-azure
```

### Test the application
How to setup local environment, including cosmos db, please refer to: testing/entitlements-v2-test-azure/README.md

How to run the integration tests:

```bash
# build + install integration test core
$ ./mvnw compile -f testing/entitlements-v2-test-core

# build + run Azure integration tests.
#
# Note: this assumes that the environment variables for integration tests as outlined
#       above are already exported in your environment.
$ ./mvnw test -f testing/entitlements-v2-test-azure
```

## Debugging

Jet Brains - the authors of Intellij IDEA, have written an [excellent guide](https://www.jetbrains.com/help/idea/debugging-your-first-java-application.html) on how to debug java programs.

## Deploying the Service

Service deployments into Azure are standardized to make the process the same for all services if using ADO and are 
closely related to the infrastructure deployed. The steps to deploy into Azure can be [found here](https://github.com/azure/osdu-infrastructure)

The default ADO pipeline is /devops/pipeline.yml

## License
Copyright © Microsoft Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
