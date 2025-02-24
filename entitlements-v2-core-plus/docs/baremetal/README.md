## Service Configuration for Baremetal

## Environment variables

Define the following environment variables.

Must have:

| name                     | value                 | description                                                                                | sensitive? | source |
|--------------------------|-----------------------|--------------------------------------------------------------------------------------------|------------|--------|
| `SPRING_PROFILES_ACTIVE` | ex `anthos`           | Spring profile that activate default configuration for Google Cloud environment            | false      | -      |
| `GROUP_ID`               | `group`               | The id of the groups is created. The default (and recommended for `jdbc`) value is `group` | no         | -      |
| `PARTITION_HOST`         | ex `http://partition` | Partition service host address                                                             | no         | -      |

Defined in default application property file but possible to override:

| name                                 | value                             | description                                                                                                                                                                                                                   | sensitive? | source |
|--------------------------------------|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|--------|
| `LOG_PREFIX`                         | `entitlements-v2`                 | Logging prefix                                                                                                                                                                                                                | no         | -      |
| `SERVER_SERVLET_CONTEXPATH`          | `/api/entitlements/v2`            | Register context path                                                                                                                                                                                                         | no         | -      |
| `LOG_LEVEL`                          | `INFO`                            | Logging level                                                                                                                                                                                                                 | no         | -      |
| `AUTHENTICATION_MODE`                | `INTERNAL`, `EXTERNAL`            | `INTERNAL` is used by default, which means that Entitlements will verify auth internally, not trusting incoming requests. `EXTERNAL` and `ISTIO`. More information about each mode can be found [here](#authentication-modes) | no         | -      |
| `OPENID_PROVIDER_USER_ID_CLAIM_NAME` | `email`                           | OpenID User ID claim name                                                                                                                                                                                                     | no         | -      |
| `REDIS_USER_INFO_HOST`               | ex `127.0.0.1`                    | Redis host                                                                                                                                                                                                                    | no         | -      |
| `REDIS_USER_INFO_PORT`               | ex `6379`                         | Redis port                                                                                                                                                                                                                    | no         | -      |
| `REDIS_USER_INFO_PASSWORD`           | ex ``                             | Redis password                                                                                                                                                                                                                | yes        | -      |
| `REDIS_USER_INFO_WITH_SSL`           | ex `true` or `false`              | Redis host SSL config                                                                                                                                                                                                         | no         |        |
| `REDIS_USER_GROUPS_HOST`             | ex `127.0.0.1`                    | Redis host                                                                                                                                                                                                                    | no         | -      |
| `REDIS_USER_GROUPS_PORT`             | ex `6379`                         | Redis port                                                                                                                                                                                                                    | no         | -      |
| `REDIS_USER_GROUPS_PASSWORD`         | ex ``                             | Redis password                                                                                                                                                                                                                | yes        | -      |
| `REDIS_USER_GROUPS_WITH_SSL`         | ex `true` or `false`              | Redis host SSL config                                                                                                                                                                                                         | no         |        |
| `SYSTEM_TENANT`                      | ex `system`                       | System tenant ID, default is `system`                                                                                                                                                                                         | no         |        |
| `PARTITION_PROPERTIES_PREFIX`        | ex `entitlements`                 | Prefix for Database connection properties in Partition configuration, default `entitlements`, result `entitlements.datasource.url`                                                                                            | no         |        |
| `MANAGEMENT_ENDPOINTS_WEB_BASE`      | ex `/`                            | Web base for Actuator                                                                                                                                                                                                         | no         | -      |
| `MANAGEMENT_SERVER_PORT`             | ex `8081`                         | Port for Actuator                                                                                                                                                                                                             | no         | -      |


## Postgres Connection configuration

Entitlements database connection is configured via properties, provided by the Partition service:

```json
{
  "properties": {
    "entitlements.datasource.url": {
      "sensitive": true,
      "value": "ENT_PG_URL"
    },
    "entitlements.datasource.username": {
      "sensitive": true,
      "value": "ENT_PG_USER"
    },
    "entitlements.datasource.password": {
      "sensitive": true,
      "value": "ENT_PG_PASS"
    },
    "entitlements.datasource.schema": {
      "sensitive": true,
      "value": "ENT_PG_SCHEMA"
    }
  }
}
```

By default, the prefix is `entitlements` but could be overridden by PARTITION_PROPERTIES_PREFIX env
var.
If `sensitive` is set to false then `value` will be used from the property as is, if it is false
then `value` of the property will be used as the env var name,
and value should be provided in the service environment variables with that name.

## Authentication modes

**ISTIO** Use it when authentication should not be processed by entitlements, Istio will be used for
token validation.

| name                                 | value      | description               | sensitive? | source |
|--------------------------------------|------------|---------------------------|------------|--------|
| `OPENID_PROVIDER_USER_ID_CLAIM_NAME` | ex `email` | OpenID user id claim name | no         | -      |

**EXTERNAL**  Use it with enabled external authentication method, this mode use combined
authentication with OpenID provider, if the request will contain both token and user-id header

| name                                 | value                            | description                                                                  | sensitive? | source |
|--------------------------------------|----------------------------------|------------------------------------------------------------------------------|------------|--------|
| `OPENID_PROVIDER_URL`                | ex `https://accounts.google.com` | OpenID provider                                                              | no         | -      |
| `OPENID_PROVIDER_ALGORITHM`          | ex `RS256`                       | OpenID token algorithm                                                       | no         | -      |
| `USER_IDENTITY_HEADER_NAME`          | ex `x-user-id`                   | The name of the header in which the "id of the authenticated user" is passed | no         | -      |
| `OPENID_PROVIDER_USER_ID_CLAIM_NAME` | ex `preferred_username`          | OpenID user id claim name                                                    | no         | -      |

**INTERNAL** Use it when authentication should be processed by entitlements, OpenID provider will be
used for token validation.

| name                        | value                            | description                                                                  | sensitive? | source |
|-----------------------------|----------------------------------|------------------------------------------------------------------------------|------------|--------|
| `OPENID_PROVIDER_URL`       | ex `https://accounts.google.com` | OpenID provider                                                              | no         | -      |
| `OPENID_PROVIDER_ALGORITHM` | ex `RS256`                       | OpenID token algorithm                                                       | no         | -      |
| `USER_IDENTITY_HEADER_NAME` | ex `x-user-id`                   | The name of the header in which the "id of the authenticated user" is passed | no         | -      |

**Required to run integration tests**

| Name                                           | Value                                             | Description                                                                                                                                                      | Sensitive?                                        | Source |
|------------------------------------------------|---------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|--------|
| `ENTITLEMENT_V2_URL`                           | ex `http://localhost:8080/api/entitlements/v2/`   | The host where the service is running                                                                                                                            | no                                                | --     |
| `GROUP_ID`                                     | ex `contoso.com`                                  | Must match the value of `service_group_id` above                                                                                                                 | no                                                | --     |
| `TENANT_NAME`                                  | ex `opendes`                                      | OSDU tenant used for testing                                                                                                                                     | no                                                | --     |
| `TEST_OPENID_PROVIDER_CLIENT_ID`               | `********`                                        | Client Id for `$INTEGRATION_TESTER`                                                                                                                              | yes                                               | --     |
| `TEST_OPENID_PROVIDER_CLIENT_SECRET`           | `********`                                        |                                                                                                                                                                  | Client secret for `$INTEGRATION_TESTER`           | --     |
| `TEST_NO_ACCESS_OPENID_PROVIDER_CLIENT_ID`     | `********`                                        | Client Id for `$NO_ACCESS_INTEGRATION_TESTER`                                                                                                                    | yes                                               | --     |
| `TEST_NO_ACCESS_OPENID_PROVIDER_CLIENT_SECRET` | `********`                                        |                                                                                                                                                                  | Client secret for `$NO_ACCESS_INTEGRATION_TESTER` | --     |
| `INTEGRATION_TESTER_EMAIL`                     | `datafier@service.local`                          |                                                                                                                                                                  | Email of `$INTEGRATION_TESTER`                    | --     |
| `TEST_OPENID_PROVIDER_URL`                     | `https://keycloak.com/auth/realms/osdu`           | OpenID provider url                                                                                                                                              | yes                                               | --     |
| `PARTITION_API`                                | ex `http://localhost:8080/api/partition/v1 `      | Partition service host                                                                                                                                           | no                                                | --     |
| `INDEXER_SERVICE_ACCOUNT_EMAIL`                | ex `workload-indexer@osdu.iam.gseviceaccount.com` | Indexer service account email with special privileges for data groups                                                                                            | no                                                | --     |
| `DATA_ROOT_GROUP_HIERARCHY_ENABLED`            | ex `true`                                         | Depending on the `DISABLE_DATA_ROOT_GROUP HIERARCHY` feature flag in Partition info, this flag controls whenever data.root groups get access to all data groups. | no                                                | --     |

**Entitlements configuration for integration accounts**

| INTEGRATION_TESTER         | NO ACCESS TESTER          |
|----------------------------|---------------------------|
| users                      | users                     |
| service.entitlements.user  | service.entitlements.user |
| service.entitlements.admin |                           |
| users.datalake.delegation  |                           |
