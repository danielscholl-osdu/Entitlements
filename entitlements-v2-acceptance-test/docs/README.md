### Running E2E Tests

You will need to have the following environment variables defined.

| name                                | value                                              | description                                                                                                                                                    | sensitive? | source                                                       |
|-------------------------------------|----------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|--------------------------------------------------------------|
| `ENTITLEMENTS_DOMAIN`               | eg. `opendes-gc.projects.com`                      | OSDU R2 entitlements domain to run tests under                                                                                                                 | no         | -                                                            |
| `PARTITION_URL`                     | eg. `http://localhost:8080/api/partition/v1/`      | Endpoint of partition service                                                                                                                                  | no         | -                                                            |
| `INTEGRATION_TESTER_EMAIL`          | eg. `integration-tester@service.local`             | Endpoint of storage service                                                                                                                                    | no         | -                                                            |
| `TENANT_NAME`                       | eg. `osdu`                                         | OSDU tenant used for testing                                                                                                                                   | no         | --                                                           |
| `ENTITLEMENTS_URL`                  | eg. `http://localhost:8080/api/entitlements/v2/`   | Endpoint of entitlements service                                                                                                                               | no         | -                                                            |
| `INDEXER_SERVICE_ACCOUNT_EMAIL`     | eg. `workload-indexer@osdu.iam.gseviceaccount.com` | Indexer service account email with special privileges for data groups                                                                                          | no         | -                                                            |
| `DATA_ROOT_GROUP_HIERARCHY_ENABLED` | eg. `true`                                         | Depending on the DISABLE_DATA_ROOT_GROUP HIERARCHY feature flag in Partition info, this flag controls whenever data.root groups get access to all data groups. | no         | -                                                            |
Authentication can be provided as OIDC config:

| name                                            | value                                   | description                   | sensitive? | source |
|-------------------------------------------------|-----------------------------------------|-------------------------------|------------|--------|
| `ROOT_USER_OPENID_PROVIDER_CLIENT_ID`           | `********`                              | ROOT_USER Client Id           | yes        | -      |
| `ROOT_USER_OPENID_PROVIDER_CLIENT_SECRET`       | `********`                              | ROOT_USER Client secret       | yes        | -      |
| `NO_ACCESS_USER_OPENID_PROVIDER_CLIENT_ID`      | `********`                              | NO_ACCESS_USER Client Id      | yes        | -      |
| `NO_ACCESS_USER_OPENID_PROVIDER_CLIENT_SECRET`  | `********`                              | NO_ACCESS_USER Client secret  | yes        | -      |
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_ID`     | `********`                              | PRIVILEGED_USER Client Id     | yes        | -      |
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_SECRET` | `********`                              | PRIVILEGED_USER Client secret | yes        | -      |
| `TEST_OPENID_PROVIDER_URL`                      | `https://keycloak.com/auth/realms/osdu` | OpenID provider url           | yes        | -      |

Or tokens can be used directly from env variables:

| name                    | value      | description           | sensitive? | source |
|-------------------------|------------|-----------------------|------------|--------|
| `PRIVILEGED_USER_TOKEN` | `********` | PRIVILEGED_USER Token | yes        | -      |
| `NO_ACCESS_USER_TOKEN`  | `********` | NO_ACCESS_USER Token  | yes        | -      |
| `ROOT_USER_TOKEN`       | `********` | ROOT_USER Token       | yes        | -      |



**Entitlements configuration for integration accounts**

| INTEGRATION_TESTER         | NO_DATA_ACCESS_TESTER     |
|----------------------------|---------------------------|
| users                      | users                     |
| service.entitlements.user  | service.entitlements.user | 
| service.entitlements.admin | service.storage.admin     |
| users.datalake.delegation  |                           |                           


Execute following command to build code and run all the integration tests:

 ```bash
 # Note: this assumes that the environment variables for integration tests as outlined
 #       above are already exported in your environment.
 # build + install integration test core
 $ (cd entitlements-v2-acceptance-test && mvn clean verify)
 ```

## License

Copyright © Google LLC

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
