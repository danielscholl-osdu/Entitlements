
### How To Run Azure Integration Tests in local environment

1. Temporarily hardcode `requestInfo.getTenantInfo().getServiceAccount()` with datafier account id in following classes:
   - ListGroupService
   - DeleteGroupService
   - RemoveMemberService
   - KeySvcAccBeanConfiguration
   - PermissionService
2. Set Up CosmosDB Emulator
   - Download [Azure Cosmos emulator](https://docs.microsoft.com/en-us/azure/cosmos-db/local-emulator?tabs=cli%2Cssl-netstd21#download-the-emulator) and save the program to your desktop.
   - Navigate to the directory and start the emulator from command prompt. This should pop up the Emulator in localhost:8081.
      ```
      Microsoft.Azure.Cosmos.Emulator.exe /EnableGremlinEndpoint
      ```
   - Go to 'Explorer' tab and create a database "db1" and a collection "coll1". For the partition key, use "/dataPartitionId".
3. Set Up Gremlin Console
   - Download [apache-tinkerpop-gremlin-console-3.3.4](https://archive.apache.org/dist/tinkerpop/3.3.4/)
   - Place `testing/entitlements-v2-test-azure/src/test/resources/local/remote-localcompute.yaml` in conf folder of
     apache-tinkerpop-gremlin-console
4. Add Bootstrap Data to CosmosDB Emulator using Gremlin Console
   - Run `bin\gremlin.bat` from apache-tinkerpop-gremlin-console-3.3.4 directory to start Gremlin Console.
   - Once console has started, run these commands to connect to Emulator:
      ```
      :remote connect tinkerpop.server conf/remote-localcompute.yaml
      :remote console
      ```
   - Run gremlin queries from `testing/entitlements-v2-test-azure/src/test/resources/bootstrap/bootstrap-data-local.txt`
5. Temporarily hardcode in provider/entitlements-v2-azure/src/main/resources/application.properties the following properties:
    app.gremlin.sslEnabled to 'false'
    app.gremlin.port to '8901'
6. Run Entitlements V2 service from Azure provider
7. Define 5 env variables for integration tests (e.g. maven options):
   1. ENTITLEMENT_V2_URL=`http://localhost:8080/api/entitlements/v2`
   2. INTEGRATION_TESTER=`Keyvault > Choose 'app-dev-sp-username' -> Copy Secret value`
   3. AZURE_TESTER_SERVICEPRINCIPAL_SECRET=`Keyvault > Choose 'app-dev-sp-password' -> Copy Secret value`
   4. AZURE_AD_TENANT_ID=`Keyvault > Choose 'app-dev-sp-tenant-id' -> Copy Secret value`
   5. AZURE_AD_APP_RESOURCE_ID=`Keyvault > Choose 'aad-client-id' -> Copy Secret value`
8. Run integration tests
