# Entitlements Data Migration Instractions

## Getting started

* [Python 2.7 or 3.5.3+][python]

__SDK installation__

Install the Python SDK

```bash
pip3 install azure-cosmos
pip3 install ada
pip3 install azure-mgmt-datalake-analytics
pip3 install azure-identity
pip3 install azure-keyvault-secrets
```

__Migrate data from entitlements-azure to entitlements__

```bash
# Retrieve Values from Environment Key Vault
export AZURE_CLIENT_ID=$(az keyvault secret show --id https://${ENV_VAULT}.vault.azure.net/secrets/app-dev-sp-username --query value -otsv)
export AZURE_CLIENT_SECRET=$(az keyvault secret show --id https://${ENV_VAULT}.vault.azure.net/secrets/app-dev-sp-password --query value -otsv)
export AZURE_TENANT_ID=$(az keyvault secret show --id https://${ENV_VAULT}.vault.azure.net/secrets/app-dev-sp-tenant-id --query value -otsv)
export RESOURCE_ID=$(az keyvault secret show --id https://${ENV_VAULT}.vault.azure.net/secrets/aad-client-id --query value -otsv)

# Execute the Upload
python3 migrate.py
```



