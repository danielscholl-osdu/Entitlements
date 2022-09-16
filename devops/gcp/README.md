# Examples

Currently, GCP Helm chart supports 2 modes:

- GCP (default)
- on-prem

## GCP usage

Fill in `values.yaml` files inside `configmap` and `deploy` folders with required values to deploy `entitlements` on GCP.

Full list of variables can be found here [GCP variables](https://community.opengroup.org/osdu/platform/security-and-compliance/entitlements/-/blob/master/provider/entitlements-v2-jdbc/README.md)

Values for configmap:

```yaml
  data:
    # common
    domain: ""
    log_level: "INFO" # default
    log_prefix: "ent-v2" # default
    openid_provider_client_ids: ""
    partition_api: ""
    spring_datasource_url: ""
    spring_datasource_username: ""
    # gcp
    google_audiences: ""
    spring_datasource_password: ""
    spring_profiles_active: "dev" # default

  conf:
    configmap: "entitlements-config"
    app_name: "entitlements"
    secret_name: "entitlements-secret"
    on_prem_enabled: false # default
```

Values for deploy:

```yaml
  data:
    # common
    requests_cpu: "0.1" # default
    requests_memory: "640M" # default
    limits_cpu: "1" # default
    limits_memory: "1G" # default
    image: "" 
    imagePullPolicy: "IfNotPresent" # default
    serviceAccountName: ""
    # gcp
    cloud_sql_proxy_version: "1.32.0" # default
    sql_connection_string: ""

  conf:
    configmap: "entitlements-config"
    app_name: "entitlements"
    secret_name: "entitlements-secret"
    on_prem_enabled: false # default
```

Example of Helm installation is below:

```bash
  helm upgrade --install entitlements-config configmap
  helm upgrade --install entitlements deploy
```

## On-prem usage

Fill in `values.yaml` files inside `configmap` and `deploy` folders with required values to deploy `entitlements` on-prem.

Full list of variables can be found here [GCP variables](https://community.opengroup.org/osdu/platform/security-and-compliance/entitlements/-/blob/master/provider/entitlements-v2-jdbc/README.md)

Values for configmap:

```yaml
  data:
    # common
    domain: ""
    log_level: "INFO" # default
    log_prefix: "ent-v2" # default
    openid_provider_client_ids: ""
    partition_api: ""
    spring_datasource_url: ""
    spring_datasource_username: ""
    # onprem
    gcp_authentication_mode: ""
    openid_provider_client_id: ""
    openid_provider_client_secret: ""
    openid_provider_url: ""
    openid_provider_user_id_claim_name: "preferred_username" # default
    partition_auth_enabled: ""
    service_token_provider: "OPENID" # default

  conf:
    configmap: "entitlements-config"
    app_name: "entitlements"
    secret_name: "entitlements-secret"
    on_prem_enabled: false # default
```

Values for deploy:

```yaml
  data:
    # common
    requests_cpu: "0.1" # default
    requests_memory: "640M" # default
    limits_cpu: "1" # default
    limits_memory: "1G" # default
    image: "" 
    imagePullPolicy: "IfNotPresent" # default
    serviceAccountName: ""

  conf:
    configmap: "entitlements-config"
    app_name: "entitlements"
    secret_name: "entitlements-secret"
    on_prem_enabled: false # default
```

Example of Helm installation is below:

```bash
  helm upgrade --install entitlements-config configmap --set conf.on_prem_enabled=true
  helm upgrade --install entitlements deploy --set conf.on_prem_enabled=true
```
