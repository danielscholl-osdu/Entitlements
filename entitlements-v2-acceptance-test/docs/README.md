### Running E2E Tests

You will need to have the following environment variables defined.

| name                       | value                                            | description                                                                                                                      | sensitive? | source                                                       |
|----------------------------|--------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|------------|--------------------------------------------------------------|
| `GROUP_ID`                 | eg. `open`                                       | OSDU R2 to run tests under                                                                                                       | no         | -                                                            |
| `PARTITION_API`            | eg. `http://localhost:8080/api/partition/v1/`    | Legal API endpoint                                                                                                               | no         | -                                                            |
| `INTEGRATION_TESTER_EMAIL` | eg. `integration-tester@service.local`           | Endpoint of storage service                                                                                                      | no         | -                                                            |
| `TENANT_NAME`              | eg. `osdu`                                       | OSDU tenant used for testing                                                                                                     | no         | --                                                           |
| `ENTITLEMENTS_V2_URL`      | eg. `http://localhost:8080/api/entitlements/v2/` | Endpoint of entitlements service                                                                                                 | no         | -                                                            |

Authentication can be provided as OIDC config:

| name                                           | value                                   | description                         | sensitive?                                        | source |
|------------------------------------------------|-----------------------------------------|-------------------------------------|---------------------------------------------------|--------|
| `TEST_NO_ACCESS_OPENID_PROVIDER_CLIENT_ID`     | `********`                              | NO_DATA_ACCESS_TESTER Client Id     | yes                                               | -      |
| `TEST_NO_ACCESS_OPENID_PROVIDER_CLIENT_SECRET` | `********`                              | NO_DATA_ACCESS_TESTER Client secret | Client secret for `$NO_ACCESS_INTEGRATION_TESTER` | -      |
| `TEST_OPENID_PROVIDER_CLIENT_ID`               | `********`                              | INTEGRATION_TESTER Client Id        | yes                                               | -      |
| `TEST_OPENID_PROVIDER_CLIENT_SECRET`           | `********`                              | INTEGRATION_TESTER Client secret    | Client secret for `$INTEGRATION_TESTER`           | -      |
| `TEST_OPENID_PROVIDER_URL`                     | `https://keycloak.com/auth/realms/osdu` | OpenID provider url                 | yes                                               | --     |

Or tokens can be used directly from env variables:

| name                       | value      | description                 | sensitive? | source |
|----------------------------|------------|-----------------------------|------------|--------|
| `INTEGRATION_TESTER_TOKEN` | `********` | INTEGRATION_TESTER Token    | yes        | -      |
| `NO_DATA_ACCESS_TOKEN`     | `********` | NO_DATA_ACCESS_TESTER Token | yes        | -      |



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
