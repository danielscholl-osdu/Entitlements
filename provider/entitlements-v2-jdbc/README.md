# entitlements-v2-jdbc

entitlements-v2-jdbc is a [Spring Boot](https://spring.io/projects/spring-boot) service which hosts CRUD APIs that enable management of user entitlements.
Data kept in GCP instance of Postgres database.


### Database structure

The database used in this implementation is PostgreSQL 13.0. The database structure and additional
info can be found [here][JDBC documentation].


### Requirements

In order to run this service from a local machine to Cloud Run, you need the following:

- [Maven 3.6.0+](https://maven.apache.org/download.cgi)
- [AdoptOpenJDK8](https://adoptopenjdk.net/)
- [Google Cloud SDK](https://cloud.google.com/sdk/)
- [Docker](https://docs.docker.com/engine/install/)

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
| `SPRING_DATASOURCE_URL` | ex `jdbc:postgresql://localhost:5432/entitlements` | The JDBC-valid connection string for database | yes | https://console.cloud.google.com/ |
| `SPRING_DATASOURCE_USERNAME` | ex `postgres` | The username of database user | yes | - | 
| `SPRING_DATASOURCE_PASSWORD` | ex `********` | The password of database user | yes | - |
| `SPRING_PROFILES_ACTIVE` | ex `dev` | Spring profile to be active | no | - |
| `LOG_PREFIX` | `entitlements-v2` | Logging prefix | no | - |
| `LOG_LEVEL` | `INFO` | Logging level | no | - |
| `DOMAIN` | ex `opendes.com` | The name of the domain for which the service runs | no | -- |
| `PARTITION_API` | ex `http://localhost:8080/api/partition/v1` | Partition service endpoint | no | - |
| `server_port` | ex `8080` | Port of the server | no | -- |
| `GOOGLE_AUDIENCES` | ex `*****.apps.googleusercontent.com` | Client ID for getting access to cloud resources | yes | https://console.cloud.google.com/apis/credentials |
| `GOOGLE_APPLICATION_CREDENTIALS` | `********` | Need this only if running locally, this service acc must have token sign access | yes | -- |

### Build and run the application

After configuring your environment as specified above, you can follow these steps to build and run the application. These steps should be invoked from the *repository root.*

```bash
# build + test + install core service code
$ ./mvnw clean install

# run service
#
# Note: this assumes that the environment variables for running the service as outlined
#       above are already exported in your environment.
$ java -jar $(find provider/entitlements-v2-jdbc/target/ -name '*-spring-boot.jar')

# Alternately you can run using the Maven Task
$ ./mvnw spring-boot:run -pl provider/entitlements-v2-jdbc
```


### Test the application


### Integration Tests

In order to run integration tests, you need to have the following environment variables defined:

**Required to run integration tests**

| Name | Value | Description | Sensitive? | Source |
| ---  | ---   | ---         | ---        | ---    |
| `ENTITLEMENT_V2_URL` | ex `http://localhost:8080/api/entitlements/v2/` | The host where the service is running | no | -- |
| `DOMAIN` | ex `contoso.com` | Must match the value of `service_domain_name` above | no | -- |
| `TENANT_NAME` | ex `opendes` | OSDU tenant used for testing | no | -- |
| `INTEGRATION_TESTER` | `********` | System identity to assume for API calls. Note: This user must have entitlements already configured | yes | -- |
| `INTEGRATION_TEST_AUDIENCE` | `********` | Client Id for `$INTEGRATION_TESTER` | yes | -- |
| `GOOGLE_APPLICATION_CREDENTIALS` | `********` | System identity to provide access for cleaning up groups created during test | yes | -- |

**Entitlements configuration for integration accounts**

| INTEGRATION_TESTER  | 
| --- |
| users<br/>service.entitlements.user<br/>service.entitlements.admin |


#### Using Cloud Infrastructure

1. Run Entitlements V2 service from JDBC provider (assumed that all the required environment variables specified for using Cloud Infrastructure).

2. Define environment variables for integration tests (e.g. maven options).
   
3. Run integration tests:

```bash
# build + install integration test core
$ ./mvnw compile -f testing/entitlements-v2-test-core

# build + run JDBC integration tests.
$ ./mvnw test -f testing/entitlements-v2-test-jdbc
```
## Debugging

Jet Brains - the authors of Intellij IDEA, have written an [excellent guide](https://www.jetbrains.com/help/idea/debugging-your-first-java-application.html) on how to debug java programs.

## License
Copyright © EPAM Systems

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[JDBC Documentation]: ../../docs/JDBC.md