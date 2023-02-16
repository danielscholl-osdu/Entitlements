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

### Configmap variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**data.logLevel** | logging level | string | `ERROR` | yes
**data.springProfilesActive** | Spring profile that activate default configuration for Google Cloud environment | string | `gcp` | yes
**data.entitlementsHost** | Entitlements service host | string | `http://entitlements` | yes
**data.dataPartitionId** | partition ID | string | - | yes
**data.dataPartitionIdList** | list of partition IDs | array | - | yes
**data.adminUserEmail** | admin user email | string | - | yes
**data.airflowComposerEmail** | airflow composer email  | string | - | yes
**data.partitionHost** | Partition service host | string | `http://partition` | yes
**data.projectId** | project ID | string | - | yes
**data.pubSubEmail** | Pub/Sub email | string | - | yes
**data.registerPubsubIdentity** | service account for communication Register-PubSub-Notification | string | - | yes
**data.entitlementsDomain** | The name of the domain groups are created for | string | `group` | yes
**data.redisEntHost** | The host for redis instance. If empty, helm installs an internal redis instance | string | `redis-ent-master` | yes
**data.redisEntPort** | The port for redis instance | digit | 6379 | yes

### Deployment variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**data.requestsCpu** | amount of requested CPU | string | `110m` | yes
**data.requestsMemory** | amount of requested memory| string | `650Mi` | yes
**data.limitsCpu** | CPU limit | string | `1` | yes
**data.limitsMemory** | memory limit | string | `1G` | yes
**data.serviceAccountName** | name of your service account | string | `entitlements` | yes
**data.imagePullPolicy** | when to pull image | string | `IfNotPresent` | yes
**data.image** | service image | string | - | yes
**data.redisImage** | service image | string | `redis:7` | yes
**data.bootstrapImage** | bootstrap image | string | - | yes
**data.bootstrapServiceAccountName** | bootstrap service account | string | - | yes
**data.cloudSqlProxyVersion** | version of Cloud SQL Proxy | string | `1.32.0` | yes
**data.sqlConnectionString** | string for SQL connection | string | - | yes

### Configuration variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**conf.appName** | Service name | string | `entitlements` | yes
**conf.configmap** | configmap to be used | string | `entitlements-config` | yes
**conf.onPremEnabled** | whether on-prem is enabled | boolean | false | yes
**conf.entitlementsPostgresSecretName** | entitlements Postgres secret | string | `entitlements-postgres-secret` | yes
**conf.entitlementsRedisSecretName** | entitlements Redis secret | string | `entitlements-redis-secret` | yes
**conf.bootstrapOpenidSecretName** | bootstrap OpenID secret | string | `datafier-secret` | yes
**conf.istioEnabled** | whether Istio is enabled | boolean | true | yes

### Istio variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**istio.proxyCPU** | CPU request for Envoy sidecars | string | `60m` | yes
**istio.proxyCPULimit** | CPU limit for Envoy sidecars | string | `200m` | yes
**istio.proxyMemory** | memory request for Envoy sidecars | string | `150Mi` | yes
**istio.proxyMemoryLimit** | memory limit for Envoy sidecars | string | `512Mi` | yes
**istio.bootstrapProxyCPU** | CPU request for Envoy sidecars | string | `10m` | yes
**istio.bootstrapProxyCPULimit** | CPU limit for Envoy sidecars | string | `100m` | yes
**istio.sidecarInject** | whether Istio sidecar will be injected. Be careful: setting to "false" strongly reduces security, because disables any authentication. | boolean | true | yes

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
