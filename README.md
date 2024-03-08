# Entitlement Service
Official documentation can be found at [https://osdu.pages.opengroup.org/platform/security-and-compliance/entitlements/](https://osdu.pages.opengroup.org/platform/security-and-compliance/entitlements/)

## Running the Entitlements Service locally
The Entitlements Service is a Maven multi-module project with each cloud implemention placed in its submodule.

#***REMOVED***

Instructions for running and testing this service can be found [here](./provider/entitlements-v2-aws/README.md)

### Integration tests
Instructions for running the Azure integration tests in local environment can be found [here][Azure documentation]

Instructions for running the JDBC integration tests can be found [here][JDBC documentation].

[Azure documentation]: testing/entitlements-v2-test-azure/README.md
[JDBC documentation]: provider/entitlements-v2-jdbc/README.md


### Open API 3.0 - Swagger
- Swagger UI : https://host/context-path/swagger (will redirect to https://host/context-path/swagger-ui/index.html)
- api-docs (JSON) : https://host/context-path/api-docs
- api-docs (YAML) : https://host/context-path/api-docs.yaml

All the Swagger and OpenAPI related common properties are managed here [swagger.properties](./entitlements-v2-core/src/main/resources/swagger.properties)

#### Server Url(full path vs relative path) configuration
- `api.server.fullUrl.enabled=true` It will generate full server url in the OpenAPI swagger
- `api.server.fullUrl.enabled=false` It will generate only the contextPath only
- default value is false (Currently only in Azure it is enabled)
[Reference]:(https://springdoc.org/faq.html#_how_is_server_url_generated)

##***REMOVED***

Instructions for running the AWS integration tests can be found [here](./provider/entitlements-v2-aws/README.md).
