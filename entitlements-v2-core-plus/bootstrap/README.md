# Bootstrap

Below you can find required variables to bootstrap Entitlements Service on Google Cloud and on-prem. They should be passed as environment variables.

## Common environment variables

```bash
  DATA_PARTITION_ID
  ENTITLEMENTS_HOST
  ADMIN_USER_EMAIL
  PROJECT_ID
  AIRFLOW_COMPOSER_EMAIL
```

#***REMOVED*** specific variables

```bash
  PROJECT_ID
  REGISTER_PUBSUB_IDENTITY
```

## On-prem specific variables

```bash
  OPENID_PROVIDER_URL
  OPENID_PROVIDER_CLIENT_ID
  OPENID_PROVIDER_CLIENT_SECRET
  ONPREM_ENABLED
```
