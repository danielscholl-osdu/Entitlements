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
**logLevel** | logging level | string | `ERROR` | yes
**springProfilesActive** | Spring profile that activate default configuration for GCP environment | string | `gcp` | yes
**entitlementsHost** | Entitlements service host | string | `http://entitlements` | yes
**googleAudiences** | Client ID for getting access to cloud resources and Partition service | string | - | yes
**dataPartitionId** | partition ID | string | - | yes
**dataPartitionIdList** | list of partition IDs | array | - | yes
**adminUserEmail** | admin user email | string | - | yes
**airflowComposerEmail** | airflow composer email  | string | - | yes
**openidProviderClientIds** | List of client ids that can be authorized by Entitlements service | string | - | yes
**partitionApi** | Partition service endpoint | string | `http://partition/api/partition/v1/` | yes
**projectId** | project ID | string | - | yes
**pubSubEmail** | Pub/Sub email | string | - | yes
**registerPubsubIdentity** | service account for communication Register-PubSub-Notification | string | - | yes
**partitionAuthEnabled** | Disable or enable auth token provisioning for requests to Partition service | boolean | false | yes
**entitlementsDomain** | The name of the domain groups are created for | string | `group` | yes
**redisEntHost** | The host for redis instance | string | `redis-ent-master` | yes
**redisEntPort** | The port for redis instance | digit | 6379 | yes

### Deployment variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**requestsCpu** | amount of requested CPU | string | `0.1` | yes
**requestsMemory** | amount of requested memory| string | `640M` | yes
**limitsCpu** | CPU limit | string | `1` | yes
**limitsMemory** | memory limit | string | `1G` | yes
**serviceAccountName** | name of your service account | string | `entitlements` | yes
**imagePullPolicy** | when to pull image | string | `IfNotPresent` | yes
**image** | service image | string | - | yes
**bootstrapImage** | bootstrap image | string | - | yes
**bootstrapServiceAccountName** | bootstrap service account | string | - | yes
**cloudSqlProxyVersion** | version of Cloud SQL Proxy | string | `1.32.0` | yes
**sqlConnectionString** | string for SQL connection | string | - | yes

### Configuration variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**appName** | Service name | string | `entitlements` | yes
**configmap** | configmap to be used | string | `entitlements-config` | yes
**onPremEnabled** | whether on-prem is enabled | boolean | false | yes
**entitlementsPostgresSecretName** | entitlements Postgres secret | string | `entitlements-postgres-secret` | yes
**bootstrapOpenidSecretName** | bootstrap OpenID secret | string | `datafier-secret` | yes
**istioEnabled** | whether Istio is enabled | boolean | true | yes

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
