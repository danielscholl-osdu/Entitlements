#!/usr/bin/env bash

set -ex

source ./validate-env.sh "DATA_PARTITION_ID"
source ./validate-env.sh "ENTITLEMENTS_HOST"

bootstrap_entitlements_onprem() {

  ID_TOKEN="$(curl --location --silent --globoff --request POST "${OPENID_PROVIDER_URL}/protocol/openid-connect/token" \
  --header "data-partition-id: ${DATA_PARTITION_ID}" \
  --header "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=client_credentials" \
  --data-urlencode "scope=openid" \
  --data-urlencode "client_id=${OPENID_PROVIDER_CLIENT_ID}" \
  --data-urlencode "client_secret=${OPENID_PROVIDER_CLIENT_SECRET}" | jq -r ".id_token")"
  export ID_TOKEN

  status_code=$(curl --location --globoff --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/tenant-provisioning" \
    --write-out "%{http_code}" --silent --output "/dev/null" \
    --header 'Content-Type: application/json' \
    --header "data-partition-id: ${DATA_PARTITION_ID}" \
    --header "Authorization: Bearer ${ID_TOKEN}")

  if [ "$status_code" == 200 ]
  then
    echo "Entitlements provisioning completed successfully!"
  else
    echo "Entitlements provisioning failed!"
    exit 1
  fi

  # Obtain group list
  curl --location --silent --globoff --request GET "${ENTITLEMENTS_HOST}/api/entitlements/v2/groups" \
    --header 'Content-Type: application/json' \
    --header "Authorization: Bearer ${ID_TOKEN}" \
    --header "data-partition-id: ${DATA_PARTITION_ID}" | \
    jq -r '.groups[].email' > /opt/group.txt

    sed -i "/@${DATA_PARTITION_ID}-epam/d" /opt/group.txt

  # Add user ${ADMIN_USER_EMAIL} to groups:
  cat <<EOF > /opt/user.json
{
"email": "${ADMIN_USER_EMAIL}",
"role": "OWNER"
}
EOF

# Add user ${AIRFLOW_COMPOSER_EMAIL} to groups:

  cat <<EOF > /opt/user_airflow.json
{
"email": "${AIRFLOW_COMPOSER_EMAIL}",
"role": "MEMBER"
}
EOF

  cat <<EOF > /opt/group_airflow.txt
service.storage.admin
service.file.editors
service.search.user
service.workflow.admin
service.workflow.creator
service.schema-service.editors
service.entitlements.user
users
EOF

# shellcheck disable=SC2002
grep -v '^ *#' < /opt/group_airflow.txt | while IFS= read -r GROUP_EMAIL_AIRFLOW
do
    status_code=$(curl --location --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/groups/$GROUP_EMAIL_AIRFLOW@$DATA_PARTITION_ID.$DOMAIN/members" \
      --write-out "%{http_code}" --silent --output "output.txt" \
      --header 'Content-Type: application/json' \
      --header "Authorization: Bearer ${ID_TOKEN}" \
      --header "data-partition-id: ${DATA_PARTITION_ID}" \
      --data @/opt/user_airflow.json)

    if [ "$status_code" == 200 ]
    then
      echo "Successfully granted permission to $GROUP_EMAIL_AIRFLOW"
    elif [ "$status_code" == 409 ]
    then
      cat /opt/output.txt | jq -r '.message'
    else
      cat /opt/output.txt | jq -r '.message'
      exit 1
    fi
    rm /opt/output.txt
done

  # shellcheck disable=SC2013,SC2002
  for GROUP_EMAIL in $(cat /opt/group.txt); do
    status_code=$(curl --location --globoff --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/groups/${GROUP_EMAIL}/members" \
      --write-out "%{http_code}" --silent --output "output.txt" \
      --header 'Content-Type: application/json' \
      --header "Authorization: Bearer ${ID_TOKEN}" \
      --header "data-partition-id: ${DATA_PARTITION_ID}" \
      --data @/opt/user.json)

    if [ "$status_code" == 200 ]
    then
      echo "Successfully granted permission to ${GROUP_EMAIL}"
    elif [ "$status_code" == 409 ]
    then
      cat /opt/output.txt | jq -r '.message'
    else
      cat /opt/output.txt | jq -r '.message'
      exit 1
    fi
    rm /opt/output.txt
  done

  # Add roles to services
  # Get only services(SERVICE_NAME) from /opt/services.json
  # For every service:
  #  - get groups (SERVICE_GROUP_NAME) for this service from /opt/services.json
  #  - find group email(SERVICE_GROUP_EMAIL) for every group from previous step in /opt/group.txt
  #  - create request to add GCP SA to group
  # Fix me:
  # Email in /opt/sa-gcp.json file should be fixed to avoid hard code in email name

  cat <<EOF > /opt/services.json
{
"storage":["users", "service.entitlements.user", "service.legal.user"],
"notification":["users", "service.entitlements.user", "users.datalake.editors"],
"indexer":["users", "service.entitlements.user", "service.storage.viewer", "service.schema-service.viewers", "data.default.viewers"]
}
EOF

  # shellcheck disable=SC2207,SC2002
  SERVICE_NAME=( $(cat /opt/services.json | jq -r ' keys[]') )

  for SERVICE_NAME in "${SERVICE_NAME[@]}"; do

  # Email should be fixed to avoid hard code in email name
  cat <<EOF > /opt/sa-gcp.json
{
"email": "${SERVICE_NAME}@service.local",
"role": "MEMBER"
}
EOF

  # shellcheck disable=SC2207,SC2002,SC2086
  SERVICE_GROUP_NAME=( $(cat "/opt/services.json" | jq -r .${SERVICE_NAME}[]) )
  # shellcheck disable=SC2002
    for SERVICE_GROUP_NAME in "${SERVICE_GROUP_NAME[@]}"; do
      SERVICE_GROUP_EMAIL=$(awk '{ if ($1 ~ /'"${SERVICE_GROUP_NAME}"'@'"${DATA_PARTITION_ID}"'\./) print }' /opt/group.txt)     
      status_code=$(curl --location --globoff --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/groups/${SERVICE_GROUP_EMAIL}/members" \
        --write-out "%{http_code}" --silent --output "output.txt" \
        --header 'Content-Type: application/json' \
        --header "Authorization: Bearer ${ID_TOKEN}" \
        --header "data-partition-id: ${DATA_PARTITION_ID}" \
        --data @/opt/sa-gcp.json)

      if [ "$status_code" == 200 ]
      then
        echo "Successfully added ${SERVICE_NAME} to ${SERVICE_GROUP_EMAIL}"
      elif [ "$status_code" == 409 ]
      then
        cat /opt/output.txt | jq -r '.message'
      else
        cat /opt/output.txt | jq -r '.message'
        exit 1
      fi
      rm /opt/output.txt
    done
  done
}

bootstrap_entitlements_gcp() {

  ACCESS_TOKEN="$(gcloud auth print-access-token)"
  export ACCESS_TOKEN

  status_code=$(curl --location --silent --globoff --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/tenant-provisioning" \
    --write-out "%{http_code}" --output "/dev/null" \
    --header 'Content-Type: application/json' \
    --header "data-partition-id: ${DATA_PARTITION_ID}" \
    --header "Authorization: Bearer ${ACCESS_TOKEN}")

  if [ "$status_code" == 200 ]
  then
    echo "Entitlements provisioning completed successfully!"
  else
    echo "Entitlements provisioning failed!"
    exit 1
  fi

  # Obtain group list
  curl --location --silent --globoff --request GET "${ENTITLEMENTS_HOST}/api/entitlements/v2/groups" \
    --header 'Content-Type: application/json' \
    --header "Authorization: Bearer ${ACCESS_TOKEN}" \
    --header "data-partition-id: ${DATA_PARTITION_ID}" | \
    jq -r '.groups[].email' > /opt/group.txt

  sed -i "/@${DATA_PARTITION_ID}-epam/d" /opt/group.txt

  # Add user ${ADMIN_USER_EMAIL} to groups:
  cat <<EOF > /opt/user.json
{
"email": "${ADMIN_USER_EMAIL}",
"role": "OWNER"
}
EOF

  # shellcheck disable=SC2013,SC2002
  for GROUP_EMAIL in $(cat /opt/group.txt); do
    status_code=$(curl --location --globoff --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/groups/${GROUP_EMAIL}/members" \
      --write-out "%{http_code}" --silent --output "output.txt" \
      --header 'Content-Type: application/json' \
      --header "Authorization: Bearer ${ACCESS_TOKEN}" \
      --header "data-partition-id: ${DATA_PARTITION_ID}" \
      --data @/opt/user.json)

    if [ "$status_code" == 200 ]
    then
      echo "Successfully granted permission to ${GROUP_EMAIL}"
    elif [ "$status_code" == 409 ]
    then
      cat /opt/output.txt | jq -r '.message'
    else
      cat /opt/output.txt | jq -r '.message'
      exit 1
    fi
    rm /opt/output.txt
  done

# Add user ${AIRFLOW_COMPOSER_EMAIL} to groups:

cat <<EOF > /opt/user_airflow.json
{
"email": "$AIRFLOW_COMPOSER_EMAIL",
"role": "MEMBER"
}
EOF

cat <<EOF > /opt/group_airflow.txt
service.storage.admin
service.file.editors
service.search.user
service.workflow.admin
service.workflow.creator
service.schema-service.editors
service.entitlements.user
users
EOF

# shellcheck disable=SC2002
grep -v '^ *#' < /opt/group_airflow.txt | while IFS= read -r GROUP_EMAIL_AIRFLOW
do
  status_code=$(curl --location --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/groups/$GROUP_EMAIL_AIRFLOW@$DATA_PARTITION_ID.$DOMAIN/members" \
    --write-out "%{http_code}" --silent --output "output.txt" \
    --header 'Content-Type: application/json' \
    --header "Authorization: Bearer ${ACCESS_TOKEN}" \
    --header "data-partition-id: ${DATA_PARTITION_ID}" \
    --data @/opt/user_airflow.json)

  if [ "$status_code" == 200 ]
  then
    echo "Successfully granted permission to $GROUP_EMAIL_AIRFLOW"
  elif [ "$status_code" == 409 ]
  then
    cat /opt/output.txt | jq -r '.message'
  else
    cat /opt/output.txt | jq -r '.message'
    exit 1
  fi
  rm /opt/output.txt
done

# Add user ${PUB_SUB_EMAIL} to groups:

cat <<EOF > /opt/user_pub_sub.json
{
"email": "$PUB_SUB_EMAIL",
"role": "MEMBER"
}
EOF

cat <<EOF > /opt/group_pub_sub.txt
users.datalake.ops
service.search.admin
users
EOF

# shellcheck disable=SC2002
grep -v '^ *#' < /opt/group_pub_sub.txt | while IFS= read -r GROUP_EMAIL_PUB_SUB
do

  status_code=$(curl --location --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/groups/$GROUP_EMAIL_PUB_SUB@$DATA_PARTITION_ID.$DOMAIN/members" \
    --write-out "%{http_code}" --silent --output "output.txt" \
    --header 'Content-Type: application/json' \
    --header "Authorization: Bearer ${ACCESS_TOKEN}" \
    --header "data-partition-id: ${DATA_PARTITION_ID}" \
    --data @/opt/user_pub_sub.json)

  if [ "$status_code" == 200 ]
  then
    echo "Successfully granted permission to $GROUP_EMAIL_PUB_SUB for Pub/Sub"
  elif [ "$status_code" == 409 ]
  then
    cat /opt/output.txt | jq -r '.message'
  else
    cat /opt/output.txt | jq -r '.message'
    exit 1
  fi
  rm /opt/output.txt        
done

# Add user ${REGISTER_PUBSUB_IDENTITY} to groups:

cat <<EOF > /opt/user_register.json
{
"email": "$REGISTER_PUBSUB_IDENTITY",
"role": "MEMBER"
}
EOF

cat <<EOF > /opt/group_register.txt
notification.pubsub
users
EOF

# shellcheck disable=SC2002
grep -v '^ *#' < /opt/group_register.txt | while IFS= read -r GROUP_EMAIL_REGISTER
do
  status_code=$(curl --location --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/groups/$GROUP_EMAIL_REGISTER@$DATA_PARTITION_ID.$DOMAIN/members" \
    --write-out "%{http_code}" --silent --output "output.txt" \
    --header 'Content-Type: application/json' \
    --header "Authorization: Bearer ${ACCESS_TOKEN}" \
    --header "data-partition-id: ${DATA_PARTITION_ID}" \
    --data @/opt/user_register.json)

  if [ "$status_code" == 200 ]
  then
    echo "Successfully granted permission to $GROUP_EMAIL_REGISTER for Pub/Sub"
  elif [ "$status_code" == 409 ]
  then
    cat /opt/output.txt | jq -r '.message'
  else
    cat /opt/output.txt | jq -r '.message'
    exit 1
  fi
  rm /opt/output.txt
done

  # Add roles to services
  # Get only services(SERVICE_NAME) from /opt/services.json
  # For every service:
  #  - get groups (SERVICE_GROUP_NAME) for this service from /opt/services.json
  #  - find group email(SERVICE_GROUP_EMAIL) for every group from previous step in /opt/group.txt
  #  - create request to add GCP SA to group
  # Fix me:
  # Email in /opt/sa-gcp.json file should be fixed to avoid hard code in email name

  cat <<EOF > /opt/services.json
{
"storage":["users", "service.entitlements.user", "service.legal.user"],
"notification":["users", "service.entitlements.user", "users.datalake.editors"],
"indexer":["users", "service.entitlements.user", "service.storage.admin", "service.schema-service.viewers", "data.default.viewers", "service.search.admin"  ]
}
EOF

  # shellcheck disable=SC2207,SC2002
  SERVICE_NAME=( $(cat /opt/services.json | jq -r ' keys[]') )


  for SERVICE_NAME in "${SERVICE_NAME[@]}"; do

  # Email should be fixed to avoid hard code in email name
  cat <<EOF > /opt/sa-gcp.json
{
"email": "workload-${SERVICE_NAME}-gcp@${PROJECT_ID}.iam.gserviceaccount.com",
"role": "MEMBER"
}
EOF

  # shellcheck disable=SC2207,SC2002,SC2086
  SERVICE_GROUP_NAME=( $(cat "/opt/services.json" | jq -r .${SERVICE_NAME}[]) )
  # shellcheck disable=SC2002
    for SERVICE_GROUP_NAME in "${SERVICE_GROUP_NAME[@]}"; do
      SERVICE_GROUP_EMAIL=$(awk '{ if ($1 ~ /'"${SERVICE_GROUP_NAME}"'@'"${DATA_PARTITION_ID}"'\./) print }' /opt/group.txt)
      status_code=$(curl --location --globoff --request POST "${ENTITLEMENTS_HOST}/api/entitlements/v2/groups/${SERVICE_GROUP_EMAIL}/members" \
        --write-out "%{http_code}" --silent --output "output.txt" \
        --header 'Content-Type: application/json' \
        --header "Authorization: Bearer ${ACCESS_TOKEN}" \
        --header "data-partition-id: ${DATA_PARTITION_ID}" \
        --data @/opt/sa-gcp.json)

      if [ "$status_code" == 200 ]
      then
        echo "$SERVICE_GROUP_NAME added successfully to ${SERVICE_GROUP_EMAIL}"
      elif [ "$status_code" == 409 ]
      then
        cat /opt/output.txt | jq -r '.message'
      else
        cat /opt/output.txt | jq -r '.message'
        exit 1
      fi
      rm /opt/output.txt
    done
  done
}

if [ "${ONPREM_ENABLED}" == "true" ]
then
  source ./validate-env.sh "OPENID_PROVIDER_URL"
  source ./validate-env.sh "OPENID_PROVIDER_CLIENT_ID"
  source ./validate-env.sh "OPENID_PROVIDER_CLIENT_SECRET"
  bootstrap_entitlements_onprem
else
  source ./validate-env.sh "ADMIN_USER_EMAIL"
  source ./validate-env.sh "PROJECT_ID"
  bootstrap_entitlements_gcp
fi

touch /tmp/bootstrap_ready
