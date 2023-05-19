# entitlements-v2-jdbc

entitlements-v2-jdbc is a [Spring Boot](https://spring.io/projects/spring-boot) service which hosts CRUD APIs that enable management of user entitlements.
Data kept in Google Cloud instance of Postgres database.

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

## Service Configuration

## Authorisation flow for impersonated users
### What problem we tried to solve

```
Currently Ingestion Jobs in OSDU like CSV Parser, Manifest Ingestion etc. 
uses Service Account Token while calling any OSDU Service APIs like Storage/Dataset, 
which means any Authorization checks happening for API Access or Data Level Access 
(ACL checks in Storage service) is based on permission level of Service Account 
rather than based on the User who initiated the Ingestion in the first place.

Therefore, the Users indirectly gets highest level of permissions in OSDU 
which can be used to modify data of other users in the system 
(a scenario from CSV Parser will be discussed later to understand the issue better). 
This problem is not just specific to Ingestion but can be true for any service 
which performs long running jobs and is relying on Service Account Token. 
For rest of this ADR we will discuss Ingestion Scenario and related flows 
to highlight the problem and solution, but as said it can be applicable to OSDU in general.
```
Example:

![Screenshot](./pics/security_problem.PNG)

### And suggested solution was:
```
- (service side changes) A new header x-on-behalf-of will be introduced 
which will store the user identity (context)
- (change in SPI Layer (Service Mesh)) If the request contains 
Internal Service Account Token and x-on-behalf-of header is not empty or null, 
then the x-user-id header will be set to x-on-behalf-of header:
Else set the x-user-id header by existing logic
```
### Current implementation:

User impersonation the responsibility of the Entitlements service.
Added header '**on-behalf-of**'.

And users of Entitlements service should have groups listed below:
* **users.datalake.delegation** || Users who can impersonate
* **users.datalake.impersonation** || Users who can be impersonated

Using these groups Entitlements service can check if
impersonation allowed for current **requesterId**.
And if not it throws **403 Forbidden** Exception.

```
Impersonation won't be allowed if 
- impersonation group not found
- delegation group not found
```

### Baremetal Service Configuration

[Baremetal service configuration](docs/baremetal/README.md)

##***REMOVED*** Service Configuration

[Google Cloud service configuration](docs/gc/README.md)

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

### Baremetal Service Configuration

[Baremetal service configuration](docs/baremetal/README.md)

##***REMOVED*** Service Configuration

[Google Cloud service configuration](docs/gc/README.md)

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
