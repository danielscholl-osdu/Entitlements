<!--- Deploy -->

# Deploy helm chart

## Introduction

This chart bootstraps a deployment on a [Kubernetes](https://kubernetes.io) cluster using [Helm](https://helm.sh) package manager.

## Prerequisites

The code was tested on **Kubernetes cluster** (v1.21.11) with **Istio** (1.12.6)
> It is possible to use other versions, but it hasn't been tested

### Operation system

The code works in Debian-based Linux (Debian 10 and Ubuntu 20.04) and Windows WSL 2. Also, it works but is not guaranteed in Google Cloud Shell. All other operating systems, including macOS, are not verified and supported.

### Packages

Packages are only needed for installation from a local computer.

- **HELM** (version: v3.7.1 or higher) [helm](https://helm.sh/docs/intro/install/)
- **Kubectl** (version: v1.21.0 or higher) [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl)

## Installation

You need to set variables in **values.yaml** file using any code editor. Some of the values are prefilled, but you need to specify some values as well. You can find more information about them below.

### Global variables

| Name                       | Description                                             | Type    | Default | Required |
| -------------------------- | ------------------------------------------------------- | ------- | ------- | -------- |
| **global.domain**          | your domain for the external endpoint, ex `example.com` | string  | -       | yes      |
| **global.limitsEnabled**   | whether CPU and memory limits are enabled               | boolean | `true`  | yes      |
| **global.dataPartitionId** | partition ID                                            | string  | -       | yes      |
| **global.logLevel**        | severity of logging level                               | string  | `ERROR` | yes      |
| **global.tier**            | Only PROD must be used to enable autoscaling            | string  | ""      | no       |
| **global.autoscaling**     | enables horizontal pod autoscaling, when tier=PROD      | boolean | true    | yes      |

### Configmap variables

| Name                          | Description                                                                     | Type    | Default               | Required                                        |
| ----------------------------- | ------------------------------------------------------------------------------- | ------- | --------------------- | ----------------------------------------------- |
| **data.logLevel**             | logging severity level for this service only                                    | string  | -                     | yes, only if differs from the `global.logLevel` |
| **data.entitlementsHost**     | Entitlements service host                                                       | string  | `http://entitlements` | yes                                             |
| **data.adminUserEmail**       | admin user email                                                                | string  | -                     | yes                                             |
| **data.airflowComposerEmail** | airflow composer email                                                          | string  | -                     | yes                                             |
| **data.partitionHost**        | Partition service host                                                          | string  | `http://partition`    | yes                                             |
| **data.projectId**            | project ID                                                                      | string  | -                     | yes                                             |
| **data.entitlementsDomain**   | The name of the domain groups are created for                                   | string  | `group`               | yes                                             |
| **data.redisEntHost**         | The host for redis instance. If empty, helm installs an internal redis instance | string  | `redis-ent-master`    | yes                                             |
| **data.redisEntPort**         | The port for redis instance                                                     | integer | `6379`                | yes                                             |

### Deployment variables

| Name                                 | Description                  | Type   | Default                              | Required                               |
| ------------------------------------ | ---------------------------- | ------ | ------------------------------------ | -------------------------------------- |
| **data.requestsCpu**                 | amount of requested CPU      | string | `25m`                                | yes                                    |
| **data.requestsMemory**              | amount of requested memory   | string | `350Mi`                              | yes                                    |
| **data.limitsCpu**                   | CPU limit                    | string | `1`                                  | only if `global.limitsEnabled` is true |
| **data.limitsMemory**                | memory limit                 | string | `1G`                                 | only if `global.limitsEnabled` is true |
| **data.serviceAccountName**          | name of your service account | string | `entitlements`                       | yes                                    |
| **data.imagePullPolicy**             | when to pull image           | string | `IfNotPresent`                       | yes                                    |
| **data.image**                       | service image                | string | -                                    | yes                                    |
| **data.redisImage**                  | service image                | string | `docker.io/library/redis:7.2-alpine` | yes                                    |
| **data.bootstrapImage**              | bootstrap image              | string | -                                    | yes                                    |
| **data.bootstrapServiceAccountName** | bootstrap service account    | string | -                                    | yes                                    |
| **data.cloudSqlProxyVersion**        | version of Cloud SQL Proxy   | string | `1.32.0`                             | yes                                    |
| **data.sqlConnectionString**         | string for SQL connection    | string | -                                    | yes                                    |

### Configuration variables

| Name                                    | Description                  | Type    | Default                        | Required |
| --------------------------------------- | ---------------------------- | ------- | ------------------------------ | -------- |
| **conf.appName**                        | Service name                 | string  | `entitlements`                 | yes      |
| **conf.replicas**                       | Default number of replicas   | integer | `2`                            | yes      |
| **conf.configmap**                      | configmap to be used         | string  | `entitlements-config`          | yes      |
| **conf.entitlementsPostgresSecretName** | entitlements Postgres secret | string  | `entitlements-postgres-secret` | yes      |
| **conf.entitlementsRedisSecretName**    | entitlements Redis secret    | string  | `entitlements-redis-secret`    | yes      |

### Istio variables

| Name                             | Description                                                                                                                            | Type    | Default | Required |
| -------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- | ------- | ------- | -------- |
| **istio.proxyCPU**               | CPU request for Envoy sidecars                                                                                                         | string  | `10m`   | yes      |
| **istio.proxyCPULimit**          | CPU limit for Envoy sidecars                                                                                                           | string  | `200m`  | yes      |
| **istio.proxyMemory**            | memory request for Envoy sidecars                                                                                                      | string  | `64Mi`  | yes      |
| **istio.proxyMemoryLimit**       | memory limit for Envoy sidecars                                                                                                        | string  | `256Mi` | yes      |
| **istio.bootstrapProxyCPU**      | CPU request for Envoy sidecars                                                                                                         | string  | `10m`   | yes      |
| **istio.bootstrapProxyCPULimit** | CPU limit for Envoy sidecars                                                                                                           | string  | `100m`  | yes      |
| **istio.sidecarInject**          | whether Istio sidecar will be injected. Be careful: setting to `false` strongly reduces security, because disables any authentication. | boolean | `true`  | yes      |
| **istio.corsEnabled**            | enable CORS                                                                                                                            | boolean | `false` | no       |

### Horizontal Pod Autoscaling (HPA) variables (works only if tier=PROD and autoscaling=true)

| Name                                                | Description                                                                   | Type    | Default        | Required                                                       |
| --------------------------------------------------- | ----------------------------------------------------------------------------- | ------- | -------------- | -------------------------------------------------------------- |
| **hpa.minReplicas**                                 | minimum number of replicas                                                    | integer | 6              | only if `global.autoscaling` is true and `global.tier` is PROD |
| **hpa.maxReplicas**                                 | maximum number of replicas                                                    | integer | 15             | only if `global.autoscaling` is true and `global.tier` is PROD |
| **hpa.targetType**                                  | type of measurements: AverageValue or Value                                   | string  | "AverageValue" | only if `global.autoscaling` is true and `global.tier` is PROD |
| **hpa.targetValue**                                 | threshold value to trigger the scaling up                                     | integer | 80             | only if `global.autoscaling` is true and `global.tier` is PROD |
| **hpa.behaviorScaleUpStabilizationWindowSeconds**   | time to start implementing the scale up when it is triggered                  | integer | 10             | only if `global.autoscaling` is true and `global.tier` is PROD |
| **hpa.behaviorScaleUpPoliciesValue**                | the maximum number of new replicas to create (in percents from current state) | integer | 50             | only if `global.autoscaling` is true and `global.tier` is PROD |
| **hpa.behaviorScaleUpPoliciesPeriodSeconds**        | pause for every new scale up decision                                         | integer | 15             | only if `global.autoscaling` is true and `global.tier` is PROD |
| **hpa.behaviorScaleDownStabilizationWindowSeconds** | time to start implementing the scale down when it is triggered                | integer | 60             | only if `global.autoscaling` is true and `global.tier` is PROD |
| **hpa.behaviorScaleDownPoliciesValue**              | the maximum number of replicas to destroy (in percents from current state)    | integer | 25             | only if `global.autoscaling` is true and `global.tier` is PROD |
| **hpa.behaviorScaleDownPoliciesPeriodSeconds**      | pause for every new scale down decision                                       | integer | 60             | only if `global.autoscaling` is true and `global.tier` is PROD |

### Limits variables

| Name                     | Description                                     | Type    | Default | Required                                                       |
| ------------------------ | ----------------------------------------------- | ------- | ------- | -------------------------------------------------------------- |
| **limits.maxTokens**     | maximum number of requests per fillInterval     | integer | 50      | only if `global.autoscaling` is true and `global.tier` is PROD |
| **limits.tokensPerFill** | number of new tokens allowed every fillInterval | integer | 50      | only if `global.autoscaling` is true and `global.tier` is PROD |
| **limits.fillInterval**  | time interval                                   | string  | "1s"    | only if `global.autoscaling` is true and `global.tier` is PROD |

## Install the Helm chart

Run this command from within this directory:

```console
helm install entitlements-deploy .
```

## Uninstall the Helm chart

To uninstall the helm deployment:

```console
helm uninstall entitlements-deploy
```

> Do not forget to delete all k8s secrets and PVCs accociated with the Service.

[Move-to-Top](#deploy-helm-chart)
