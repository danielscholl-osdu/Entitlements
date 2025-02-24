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

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**global.domain** | your domain for the external endpoint, ex `example.com` | string | - | yes
**global.limitsEnabled** | whether CPU and memory limits are enabled | boolean | true | yes
**global.dataPartitionId** | partition ID | string | - | yes

### Configmap variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**data.logLevel** | logging level | string | `ERROR` | yes
**data.entitlementsHost** | Entitlements service host | string | `http://entitlements` | yes
**data.adminUserEmail** | admin user email | string | - | yes
**data.airflowComposerEmail** | airflow composer email  | string | - | yes
**data.partitionHost** | Partition service host | string | `http://partition` | yes
**data.entitlementsDomain** | The name of the domain groups are created for | string | `group` | yes
**data.redisEntHost** | The host for redis instance. If empty, helm installs an internal redis instance | string | `redis-ent-master` | yes
**data.redisEntPort** | The port for redis instance | digit | 6379 | yes

### Deployment variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**data.requestsCpu** | amount of requested CPU | string | `10m` | yes
**data.requestsMemory** | amount of requested memory| string | `450Mi` | yes
**data.limitsCpu** | CPU limit | string | `1` | only if `global.limitsEnabled` is true
**data.limitsMemory** | memory limit | string | `1G` | only if `global.limitsEnabled` is true
**data.serviceAccountName** | name of your service account | string | `entitlements` | yes
**data.imagePullPolicy** | when to pull image | string | `IfNotPresent` | yes
**data.image** | service image | string | - | yes
**data.redisImage** | service image | string | `redis:7` | yes
**data.bootstrapImage** | bootstrap image | string | - | yes
**data.bootstrapServiceAccountName** | bootstrap service account | string | - | yes

### Configuration variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**conf.appName** | Service name | string | `entitlements` | yes
**conf.configmap** | configmap to be used | string | `entitlements-config` | yes
**conf.entitlementsPostgresSecretName** | entitlements Postgres secret | string | `entitlements-postgres-secret` | yes
**conf.entitlementsRedisSecretName** | entitlements Redis secret | string | `entitlements-redis-secret` | yes
**conf.bootstrapOpenidSecretName** | bootstrap OpenID secret | string | `datafier-secret` | yes

### Auth variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**auth.keycloakUrl** | endpoint of Keycloak when using an external one, ex `keycloak.com`. For local instance it uses `domain` | string | - | no
**auth.localUrl** | authentication local URL | string | `keycloak` | yes
**auth.realm** | Keycloak realm | string | `osdu` | yes

### Istio variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**istio.proxyCPU** | CPU request for Envoy sidecars | string | `10m` | yes
**istio.proxyCPULimit** | CPU limit for Envoy sidecars | string | `200m` | yes
**istio.proxyMemory** | memory request for Envoy sidecars | string | `64Mi` | yes
**istio.proxyMemoryLimit** | memory limit for Envoy sidecars | string | `256Mi` | yes
**istio.bootstrapProxyCPU** | CPU request for Envoy sidecars | string | `10m` | yes
**istio.bootstrapProxyCPULimit** | CPU limit for Envoy sidecars | string | `100m` | yes
**istio.sidecarInject** | whether Istio sidecar will be injected. Be careful: setting to "false" strongly reduces security, because disables any authentication. | boolean | true | yes

## Install the Helm chart

Run this command from within this directory:

```console
helm install core-plus-entitlements-deploy .
```

## Uninstall the Helm chart

To uninstall the helm deployment:

```console
helm uninstall core-plus-entitlements-deploy
```

> Do not forget to delete all k8s secrets and PVCs accociated with the Service.

[Move-to-Top](#deploy-helm-chart)
