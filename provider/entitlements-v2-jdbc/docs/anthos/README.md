## Service Configuration for Anthos

## Environment variables:

Define the following environment variables.

Must have:

| name                         | value                                              | description                                                                                              | sensitive? | source                            |
|------------------------------|----------------------------------------------------|----------------------------------------------------------------------------------------------------------|------------|-----------------------------------|
| `SPRING_PROFILES_ACTIVE`     | ex `anthos`                                        | Spring profile that activate default configuration for GCP environment                                   | false      | -                                 |
| `SPRING_DATASOURCE_URL`      | ex `jdbc:postgresql://localhost:5432/entitlements` | The JDBC-valid connection string for database                                                            | yes        | https://console.cloud.google.com/ |
| `SPRING_DATASOURCE_USERNAME` | ex `postgres`                                      | The username of database user                                                                            | yes        | -                                 | 
| `SPRING_DATASOURCE_PASSWORD` | ex `********`                                      | The password of database user                                                                            | yes        | -                                 |
| `DOMAIN`                     | `group`                                            | The name of the domain groups are created for. The default (and recommended for `jdbc`) value is `group` | no         | -                                 |
| `PARTITION_API`              | ex `http://localhost:8080/api/partition/v1`        | Partition service endpoint                                                                               | no         | -                                 |

Defined in default application property file but possible to override:

| name                                 | value                                                                                                                                                                                           | description                                                       | sensitive? | source |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|------------|--------|
| `LOG_PREFIX`                         | `entitlements-v2`                                                                                                                                                                               | Logging prefix                                                    | no         | -      |
| `SERVER_SERVLET_CONTEXPATH`          | `/api/entitlements/v2`                                                                                                                                                                          | Register context path                                             | no         | -      |
| `LOG_LEVEL`                          | `INFO`                                                                                                                                                                                          | Logging level                                                     | no         | -      |
| `GCP_AUTHENTICATION_MODE`            | `ISTIO` is used by default for Anthos implementation, but there are other modes - `IAP`, `EXTERNAL` and `INTERNAL`. More information about each mode you can find [here](#authentication-modes) |                                                                   | no         | -      |
| `OPENID_PROVIDER_CLIENT_IDS`         | `workload-identity-storage, workload-identity-file, workload-identity-partition, etc.`                                                                                                          | List of client ids that can be authorized by Entitlements service | no         | -      |
| `OPENID_PROVIDER_USER_ID_CLAIM_NAME` | `email`                                                                                                                                                                                         | OpenID user id claim name                                         | no         | -      |
| `REDIS_USER_INFO_HOST`               | ex `127.0.0.1`                                                                                                                                                                                  | Redis host                                                        | no         | -      |
| `REDIS_USER_INFO_PORT`               | ex `6379`                                                                                                                                                                                       | Redis port                                                        | no         | -      |
| `REDIS_USER_INFO_PASSWORD`           | ex ``                                                                                                                                                                                           | Redis password                                                    | yes        | -      |
| `REDIS_USER_INFO_WITH_SSL`           | ex `true` or `false`                                                                                                                                                                            | Redis  host ssl config                                            | no         |        |
| `REDIS_USER_GROUPS_HOST`             | ex `127.0.0.1`                                                                                                                                                                                  | Redis host                                                        | no         | -      |
| `REDIS_USER_GROUPS_PORT`             | ex `6379`                                                                                                                                                                                       | Redis port                                                        | no         | -      |
| `REDIS_USER_GROUPS_PASSWORD`         | ex ``                                                                                                                                                                                           | Redis password                                                    | yes        | -      |
| `REDIS_USER_GROUPS_WITH_SSL`         | ex `true` or `false`                                                                                                                                                                            | Redis  host ssl config                                            | no         |        |

## Authentication modes

**ISTIO** Use it when authentication should not be processed by entitlements, Istio will be used for token validation.

| name                                 | value      | description               | sensitive? | source |
|--------------------------------------|------------|---------------------------|------------|--------|
| `OPENID_PROVIDER_USER_ID_CLAIM_NAME` | ex `email` | OpenID user id claim name | no         | -      |

**IAP** Use it with enabled IAP, this mode use combined authentication with OpenID provider, which will be used inside secured perimeter with service to service communication and IAP tokens verification that came with users requests.

| name                                 | value                                                                                                   | description                                      | sensitive? | source |
|--------------------------------------|---------------------------------------------------------------------------------------------------------|--------------------------------------------------|------------|--------|
| `IAP_PROVIDER_JWT_HEADER`            | ex `x-goog-iap-jwt-assertion`                                                                           | Header that will contain IAP token               | no         | -      |
| `IAP_PROVIDER_USER_ID_HEADER`        | ex `x-goog-authenticated-user-email`                                                                    | Header that will contain user email added by IAP | no         | -      |
| `IAP_PROVIDER_ISSUER_URL`            | ex `https://cloud.google.com/iap`                                                                       | IAP issuer url                                   | no         | -      |
| `IAP_PROVIDER_AUD`                   | ex `/projects/${iap.provider.project-number}/global/backendServices/${iap.provider.backend-service-id}` | IAP client id(audiences)                         | no         | -      |
| `IAP_PROVIDER_PROJECT_NUMBER`        | ex `12345`                                                                                              | Google project number                            | no         | -      |
| `IAP_PROVIDER_BACKEND_SERVICE_ID`    | ex `12345`                                                                                              | Google backend service id                        | no         | -      |
| `IAP_PROVIDER_JWK_URL`               | ex `https://www.gstatic.com/iap/verify/public_key-jwk`                                                  | IAP jwk url                                      | no         | -      |
| `IAP_PROVIDER_ALGORITHM`             | ex `ES256`                                                                                              | IAP token algorithm                              | no         | -      |
| `OPENID_PROVIDER_URL`                | ex `https://accounts.google.com`                                                                        | OpenID provider                                  | no         | -      |
| `OPENID_PROVIDER_CLIENT_ID`          | ex `123.apps.googleusercontent.com`                                                                     | OpenID client id                                 | no         | -      |
| `OPENID_PROVIDER_ALGORITHM`          | ex `RS256`                                                                                              | OpenID token algorithm                           | no         | -      |
| `OPENID_PROVIDER_USER_ID_CLAIM_NAME` | ex `email`                                                                                              | OpenID user id claim name                        | no         | -      |

**EXTERNAL**  Use it with enabled external authentication method, this mode use combined authentication with OpenID provider, if the request will contain both token and user-id header

| name                                 | value                               | description                                                                  | sensitive? | source |
|--------------------------------------|-------------------------------------|------------------------------------------------------------------------------|------------|--------|
| `OPENID_PROVIDER_URL`                | ex `https://accounts.google.com`    | OpenID provider                                                              | no         | -      |
| `OPENID_PROVIDER_CLIENT_ID`          | ex `123.apps.googleusercontent.com` | OpenID client id                                                             | no         | -      |
| `OPENID_PROVIDER_ALGORITHM`          | ex `RS256`                          | OpenID token algorithm                                                       | no         | -      |
| `GCP_X_USER_IDENTITY_HEADER_NAME`    | ex `x-user-id`                      | The name of the header in which the "id of the authenticated user" is passed | no         | -      |
| `OPENID_PROVIDER_USER_ID_CLAIM_NAME` | ex `preferred_username`             | OpenID user id claim name                                                    | no         | -      |

**INTERNAL** Use it when authentication should be processed by entitlements, OpenID provider will be used for token validation.

| name                              | value                               | description                                                                  | sensitive? | source |
|-----------------------------------|-------------------------------------|------------------------------------------------------------------------------|------------|--------|
| `OPENID_PROVIDER_URL`             | ex `https://accounts.google.com`    | OpenID provider                                                              | no         | -      |
| `OPENID_PROVIDER_CLIENT_ID`       | ex `123.apps.googleusercontent.com` | OpenID client id                                                             | no         | -      |
| `OPENID_PROVIDER_ALGORITHM`       | ex `RS256`                          | OpenID token algorithm                                                       | no         | -      |
| `GCP_X_USER_IDENTITY_HEADER_NAME` | ex `x-user-id`                      | The name of the header in which the "id of the authenticated user" is passed | no         | -      |

**Required to run integration tests**

| Name                                           | Value                                           | Description                                         | Sensitive?                                        | Source |
|------------------------------------------------|-------------------------------------------------|-----------------------------------------------------|---------------------------------------------------|--------|
| `ENTITLEMENT_V2_URL`                           | ex `http://localhost:8080/api/entitlements/v2/` | The host where the service is running               | no                                                | --     |
| `DOMAIN`                                       | ex `contoso.com`                                | Must match the value of `service_domain_name` above | no                                                | --     |
| `TENANT_NAME`                                  | ex `opendes`                                    | OSDU tenant used for testing                        | no                                                | --     |
| `TEST_OPENID_PROVIDER_CLIENT_ID`               | `********`                                      | Client Id for `$INTEGRATION_TESTER`                 | yes                                               | --     |
| `TEST_OPENID_PROVIDER_CLIENT_SECRET`           | `********`                                      |                                                     | Client secret for `$INTEGRATION_TESTER`           | --     |
| `TEST_NO_ACCESS_OPENID_PROVIDER_CLIENT_ID`     | `********`                                      | Client Id for `$NO_ACCESS_INTEGRATION_TESTER`       | yes                                               | --     |
| `TEST_NO_ACCESS_OPENID_PROVIDER_CLIENT_SECRET` | `********`                                      |                                                     | Client secret for `$NO_ACCESS_INTEGRATION_TESTER` | --     |
| `INTEGRATION_TESTER_EMAIL`                     | `datafier@service.local`                        |                                                     | Email of `$INTEGRATION_TESTER`                    | --     |
| `TEST_OPENID_PROVIDER_URL`                     | `https://keycloak.com/auth/realms/osdu`         | OpenID provider url                                 | yes                                               | --     |

**Entitlements configuration for integration accounts**

| INTEGRATION_TESTER                                                                               | 
|--------------------------------------------------------------------------------------------------|
| users<br/>service.entitlements.user<br/>service.entitlements.admin<br/>users.datalake.delegation |