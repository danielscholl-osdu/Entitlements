# entitlements-v2-azure

entitlements-v2-azure is a [Spring Boot](https://spring.io/projects/spring-boot) service which hosts CRUD APIs that enable management of user entitlements.
Data kept in Azure cosmos graph database.


### Graph structure

`id` - auto-generated property <br/>
`appId` - a multi value property <br/>
`dataPartitionId` - a cosmos db partition key. It is a required property in all vertices in a graph.

Group vertex:

    {
        "id": "***"
        "nodeId": "users@opendes.domain.com",
        "name": "users",
        "description": "",
        "dataPartitionId": "opendes",
        "appId": "",
        "label": "GROUP"
    }

User vertex:

    {
        "id": "***",
        "nodeId": "user@test.com",
        "dataPartitionId": "test",
        "label": "USER"
    }
Parent edges point from group to group (in case a group is a member of another group)
or from a user to group (in case a user is a member of a group). <br/>
Child edges point from group to group (in case a group is a member of another group)
or from a group to user (in case a user is a member of a group). <br/>
`role` - an edge property. Can be "OWNER" or "MEMBER". User can be "OWNER" or "MEMBER" of another group.
Group can be only a "MEMBER" of another group.

Child edge:

    {
        "id": "***",
        "label": "child",
        "type": "edge",
        "inVLabel": "USER",
        "outVLabel": "GROUP",
        "inV": "***",
        "outV": "***",
        "properties": {
            "role": "OWNER"
        }
    }

Parent edge:

    {
        "id": "***",
        "label": "parent",
        "type": "edge",
        "inVLabel": "GROUP",
        "outVLabel": "USER",
        "inV": "***",
        "outV": "***"
    }


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

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `LOG_PREFIX` | `entitlements` | Logging prefix | no | - |
| `LOGGING_LEVEL` | `INFO` | Logging level | no | - |
| `partition_service_endpoint` |  ex `https://foo-partition.azurewebsites.net` | Partition Service API endpoint | no | output of infrastructure deployment |
| `aad_client_id` | `********` | AAD client application ID | yes | output of infrastructure deployment |
| `KEYVAULT_URI` | ex `https://foo-keyvault.vault.azure.net/` | URI of KeyVault that holds application secrets | no | output of infrastructure deployment |
| `appinsights_key` | `********` | API Key for App Insights | yes | output of infrastructure deployment |
| `AZURE_TENANT_ID` | `********` | AD tenant to authenticate users from | yes | keyvault secret: `$KEYVAULT_URI/secrets/app-dev-sp-tenant-id` |
| `AZURE_CLIENT_ID` | `********` | Identity to run the service locally. This enables access to Azure resources. You only need this if running locally | yes | keyvault secret: `$KEYVAULT_URI/secrets/app-dev-sp-username` |
| `AZURE_CLIENT_SECRET` | `********` | Secret for `$AZURE_CLIENT_ID` | yes | keyvault secret: `$KEYVAULT_URI/secrets/app-dev-sp-password` |
| `azure_istioauth_enabled` | `true` | Flag to Disable AAD auth | no | -- |
| `server_port` | ex `8080` | Port of the server | no | -- |
| `service_domain_name` | ex `contoso.com` | domain name of the service | yes | -- |
| `root_data_group_quota` | ex `5000` | Maximum number of parents a group users.data.root can have | no | -- |

In order to run the service locally, you will need to have defined environment variables that you can find [here](https://community.opengroup.org/osdu/platform/deployment-and-operations/infra-azure-provisioning/-/blob/master/tools/variables/entitlements.sh#L150).

**Note** The following command can be useful to pull secrets from keyvault:
```bash
az keyvault secret show --vault-name $KEY_VAULT_NAME --name $KEY_VAULT_SECRET_NAME --query value -otsv
```

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

#### Using Cloud Infrastructure

1. Run Entitlements V2 service from Azure provider (assumed that all the required environment variables specified for using Cloud Infrastructure).

2. Define environment variables for integration tests (e.g. maven options):
[See this link](https://community.opengroup.org/osdu/platform/deployment-and-operations/infra-azure-provisioning/-/blob/master/tools/variables/entitlements.sh#L176)

3. Run integration tests:

```bash
# build + install integration test core
$ ./mvnw compile -f testing/entitlements-v2-test-core

# build + run Azure integration tests.
$ ./mvnw test -f testing/entitlements-v2-test-azure
```

#### Using CosmosDB Emulator

1. Set up CosmosDB Emulator
    - Download [Azure Cosmos emulator](https://docs.microsoft.com/en-us/azure/cosmos-db/local-emulator?tabs=cli%2Cssl-netstd21#download-the-emulator) and save the program to your desktop
    - Navigate to the directory and start the emulator from command prompt. This should pop up the Emulator in localhost:8081
       ```
       Microsoft.Azure.Cosmos.Emulator.exe /EnableGremlinEndpoint
       ```
    - Go to Explorer tab and create a database `osdu-graph` and a collection `Entitlements`. For the partition key, use `/dataPartitionId`

2. Using this tool [Entitlements data uploader](https://community.opengroup.org/osdu/platform/deployment-and-operations/infra-azure-provisioning/-/tree/master/tools/test_data/entitlements_data_uploader), populate CosmosDB Emulator by required data for integration tests.

3. Temporarily hardcode in `provider/entitlements-v2-azure/src/main/resources/application.properties` the following properties:
    - `app.gremlin.port`=`8901`
    - `app.gremlin.sslEnabled`=`false`

4. Temporarily hardcode in `provider/entitlements-v2-azure/src/main/java/org/opengroup/osdu/entitlements/v2/azure/AzureAppProperties.java` the following methods so that they start returning such values:
    - `getGraphDbEndpoint()`=`localhost`
    - `getGraphDbPassword()`=`C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==`

5. Run Entitlements V2 service from Azure provider.

6. Define environment variables for integration tests (e.g. maven options):
[See this link](https://community.opengroup.org/osdu/platform/deployment-and-operations/infra-azure-provisioning/-/blob/master/tools/variables/entitlements.sh#L176)

7. Run integration tests:

```bash
# build + install integration test core
$ ./mvnw compile -f testing/entitlements-v2-test-core

# build + run Azure integration tests.
$ ./mvnw test -f testing/entitlements-v2-test-azure
```


## Debugging

Jet Brains - the authors of Intellij IDEA, have written an [excellent guide](https://www.jetbrains.com/help/idea/debugging-your-first-java-application.html) on how to debug java programs.


## Deploying the Service

Service deployments into Azure standardized to make the process the same for all services if using ADO and are
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

