#!/usr/bin/env bash

set -ex

bootstrap_entitlements_onprem() {

  DATA_PARTITION_ID=$1

  ID_TOKEN="$(curl --location --silent --globoff --request POST "${OPENID_PROVIDER_URL}/protocol/openid-connect/token" \
    --header "data-partition-id: ${DATA_PARTITION_ID}" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "grant_type=client_credentials" \
    --data-urlencode "scope=openid" \
    --data-urlencode "client_id=${OPENID_PROVIDER_CLIENT_ID}" \
    --data-urlencode "client_secret=${OPENID_PROVIDER_CLIENT_SECRET}" | jq -r ".id_token")"
  export ID_TOKEN

  cat <<EOF >/opt/configuration.json
{
  "aliasMappings":
[
{
"aliasId": "SERVICE_PRINCIPAL",
"userId": "$ADMIN_USER_EMAIL"
},
{
"aliasId": "SERVICE_PRINCIPAL_AIRFLOW",
"userId": "$AIRFLOW_COMPOSER_EMAIL"
},
{
"aliasId": "SERVICE_PRINCIPAL_INDEXER",
"userId": "indexer@service.local"
},
{
"aliasId": "SERVICE_PRINCIPAL_NOTIFICATION",
"userId": "notification@service.local"
},
{
"aliasId": "SERVICE_PRINCIPAL_STORAGE",
"userId": "storage@service.local"
},
{
"aliasId": "SERVICE_PRINCIPAL_SEISMIC",
"userId": "seismic@service.local"
}
]
}
EOF

  status_code=$(curl --location --globoff --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/tenant-provisioning" \
    --write-out "%{http_code}" --silent --output "output.txt" \
    --header 'Content-Type: application/json' \
    --header "data-partition-id: ${DATA_PARTITION_ID}" \
    --header "Authorization: Bearer ${ID_TOKEN}" \
    --data @/opt/configuration.json)

  if [ "$status_code" == 200 ]; then
    echo "Entitlements provisioning completed successfully!"
  else
    echo "Entitlements provisioning failed!"
    cat /opt/output.txt | jq
    exit 1
  fi

}

bootstrap_entitlements_gc_system_partition() {

  DATA_PARTITION_ID=$1

  ACCESS_TOKEN="$(gcloud auth print-access-token)"
  export ACCESS_TOKEN

  cat <<EOF >/opt/system-partition-config.json
{
  "aliasMappings":
[
{
"aliasId": "SERVICE_PRINCIPAL",
"userId": "datafier@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_AIRFLOW",
"userId": "$AIRFLOW_COMPOSER_EMAIL"
},
{
"aliasId": "SERVICE_PRINCIPAL_REGISTER",
"userId": "$REGISTER_PUBSUB_IDENTITY"
},
{
"aliasId": "SERVICE_PRINCIPAL_INDEXER",
"userId": "wi-indexer-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_NOTIFICATION",
"userId": "wi-notification-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_STORAGE",
"userId": "wi-storage-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_SEISMIC",
"userId": "wi-seismic-store-gc@${PROJECT_ID}.iam.gserviceaccount.com"
}
]
}
EOF

  status_code=$(curl --location --silent --globoff --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/tenant-provisioning" \
    --write-out "%{http_code}" --output "output.txt" \
    --header 'Content-Type: application/json' \
    --header "data-partition-id: ${DATA_PARTITION_ID}" \
    --header "Authorization: Bearer ${ACCESS_TOKEN}" \
    --data @/opt/system-partition-config.json)

  if [ "$status_code" == 200 ]; then
    echo "Entitlements provisioning completed successfully!"
  else
    echo "Entitlements provisioning failed!"
    cat /opt/output.txt | jq
    exit 1
  fi

}

bootstrap_entitlements_gc_non_system_partition() {

  DATA_PARTITION_ID=$1

  ACCESS_TOKEN="$(gcloud auth print-access-token)"
  export ACCESS_TOKEN

cat <<EOF >/opt/other-partition-config.json
{
  "aliasMappings":
[
{
"aliasId": "SERVICE_PRINCIPAL",
"userId": "$ADMIN_USER_EMAIL"
},
{
"aliasId": "SERVICE_PRINCIPAL_AIRFLOW",
"userId": "$AIRFLOW_COMPOSER_EMAIL"
},
{
"aliasId": "SERVICE_PRINCIPAL_REGISTER",
"userId": "$REGISTER_PUBSUB_IDENTITY"
},
{
"aliasId": "SERVICE_PRINCIPAL_INDEXER",
"userId": "wi-indexer-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_NOTIFICATION",
"userId": "wi-notification-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_STORAGE",
"userId": "wi-storage-gc@${PROJECT_ID}.iam.gserviceaccount.com"
},
{
"aliasId": "SERVICE_PRINCIPAL_SEISMIC",
"userId": "wi-seismic-store-gc@${PROJECT_ID}.iam.gserviceaccount.com"
}
]
}
EOF

  status_code=$(curl --location --silent --globoff --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/tenant-provisioning" \
    --write-out "%{http_code}" --output "output.txt" \
    --header 'Content-Type: application/json' \
    --header "data-partition-id: ${DATA_PARTITION_ID}" \
    --header "Authorization: Bearer ${ACCESS_TOKEN}" \
    --data @/opt/other-partition-config.json)

  if [ "$status_code" == 200 ]; then
    echo "Entitlements provisioning completed successfully!"
  else
    echo "Entitlements provisioning failed!"
    cat /opt/output.txt | jq
    exit 1
  fi

}

source ./validate-env.sh "DATA_PARTITION_ID"
source ./validate-env.sh "ENTITLEMENTS_HOST"
source ./validate-env.sh "ADMIN_USER_EMAIL"
source ./validate-env.sh "GROUP_ID"
source ./validate-env.sh "AIRFLOW_COMPOSER_EMAIL"

if [[ "${ONPREM_ENABLED}" == "true" ]]; then
  source ./validate-env.sh "OPENID_PROVIDER_URL"
  source ./validate-env.sh "OPENID_PROVIDER_CLIENT_ID"
  source ./validate-env.sh "OPENID_PROVIDER_CLIENT_SECRET"
  bootstrap_entitlements_onprem "${DATA_PARTITION_ID}"
elif [[ "${ONPREM_ENABLED}" == "false" ]]; then
  # Specifying "system" partition for GC installation 
  export SYSTEM_PARTITION_ID="system"

  source ./validate-env.sh "PROJECT_ID"
  source ./validate-env.sh "REGISTER_PUBSUB_IDENTITY"

  echo "Bootstrapping entitlements for data_partition: ${SYSTEM_PARTITION_ID}"
  bootstrap_entitlements_gc_system_partition "${SYSTEM_PARTITION_ID}"
  echo "Finished entitlements bootstrap for data_partition: ${SYSTEM_PARTITION_ID}"

  echo "Bootstrapping entitlements for data_partition: ${DATA_PARTITION_ID}"
  bootstrap_entitlements_gc_non_system_partition "${DATA_PARTITION_ID}"
  echo "Finished entitlements bootstrap for data_partition: ${DATA_PARTITION_ID}"
fi

touch /tmp/bootstrap_ready

sleep 365d
