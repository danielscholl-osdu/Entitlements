#!/bin/bash

ROLE_EMAIL="osdubvt@opengroup.org"
KEYCLOAK_CERT_URL=http://${keycloak_hostname}/auth/realms/OSDU/protocol/openid-connect/certs
KEYCLOAK_TOKEN_URL=http://${keycloak_hostname}/auth/realms/OSDU/protocol/openid-connect/token
ENTITLEMENT_URL=http://${RELEASE_NAME}-ibm-entitlement-deploy:8080/api/entitlements/v2
CLIENT_SECRET=${osdu_login_secret}

echo "kc cert url ==>" ${KEYCLOAK_CERT_URL}
echo "kc token url ==>" ${KEYCLOAK_TOKEN_URL}
echo "osdu ent url ==>" ${ENTITLEMENT_URL}
echo "osdu client secret ==>" ${CLIENT_SECRET}

while [[ "$(curl -s -k -L -o /dev/null -w ''%{http_code}'' ${KEYCLOAK_CERT_URL})" != "200" ]]; do sleep 5; done
echo "Keycloak is ready"
sleep 20

echo "Obtaining Access Token"
while true; do
  export ACCESS_TOKEN_RESPONSE=$(curl --request POST \
    --url ${KEYCLOAK_TOKEN_URL} \
    --header 'Content-Type: application/x-www-form-urlencoded' \
    --data grant_type=password \
    --data client_id=osdu-login \
    --data client_secret=${CLIENT_SECRET} \
    --data username=admin-sa \
    --data password=changeit \
    --data scope=openid)
  if [ $? -eq 0 ]; then
    ACCESS_TOKEN=$(echo $ACCESS_TOKEN_RESPONSE | sed 's/.*access_token":"//g' | sed 's/".*//g')
    break
  fi
  sleep 5
done

echo "Getting the groups"
echo ${RELEASE_NAME}

while true; do
  RESPONSE=$(curl -k -s -w '%{http_code}' -o /dev/null --location --request GET \
  --url ${ENTITLEMENT_URL}/groups \
  --header "Authorization: Bearer ${ACCESS_TOKEN}" \
  --header 'data-partition-id: opendes')
  
  echo "Resp1==>" ${RESPONSE}

  if [[ $RESPONSE -eq 200 ]]; then
    break
  else
    sleep 5
  fi
done  
 
echo "Registering Roles"
while true; do
  RESPONSE=$(curl -k -s -w '%{http_code}' -o /dev/null --location --request POST ${ENTITLEMENT_URL}/tenant-provisioning \
  --header 'data-partition-id: opendes' \
  --header "Authorization: Bearer ${ACCESS_TOKEN}" \
  --header "Content-Type: application/json" \
  --data-raw "")
  echo "Resp1==>" ${RESPONSE}
  if [[ $RESPONSE -eq 200 ]] || [[ $RESPONSE -eq 409 ]]; then
    break
  else
    sleep 5
  fi
done

echo "Registering Roles common"
while true; do
  RESPONSE=$(curl -k -s -w '%{http_code}' -o /dev/null --location --request POST ${ENTITLEMENT_URL}/tenant-provisioning \
  --header 'data-partition-id: common' \
  --header "Authorization: Bearer ${ACCESS_TOKEN}" \
  --header "Content-Type: application/json" \
  --data-raw "")
  if [[ $RESPONSE -eq 200 ]] || [[ $RESPONSE -eq 409 ]]; then
    break
  else
    sleep 5
  fi
done

echo "Registering Roles for User osdu-bvt for Opendes"
while true; do
  RESPONSE=$(curl -k -s -w '%{http_code}' -o /dev/null --location --request POST ${ENTITLEMENT_URL}/groups/users.datalake.ops@opendes.ibm.com/members \
    --header 'data-partition-id: opendes' \
    --header "Authorization: Bearer ${ACCESS_TOKEN}" \
    --header 'Content-Type: application/json' \
    --data-raw '{
        "email" : "'"$ROLE_EMAIL"'",
        "role" : "OWNER"
  }')

  if [[ $RESPONSE -eq 200 ]] || [[ $RESPONSE -eq 409 ]]; then
    break
  else
    sleep 5
  fi
done

echo "Registering Roles for User osdu-bvt for common"
while true; do
  RESPONSE=$(curl -k -s -w '%{http_code}' -o /dev/null --location --request POST ${ENTITLEMENT_URL}/groups/users.datalake.ops@common.ibm.com/members \
    --header 'data-partition-id: common' \
    --header "Authorization: Bearer ${ACCESS_TOKEN}" \
    --header 'Content-Type: application/json' \
    --data-raw '{
        "email" : "'"$ROLE_EMAIL"'",
        "role" : "OWNER"
    }')

  if [[ $RESPONSE -eq 200 ]] || [[ $RESPONSE -eq 409 ]]; then
    break
  else
    sleep 5
  fi
done

echo "Registering user Roles for User osdu-bvt for Opendes"
while true; do
  RESPONSE=$(curl -k -s -w '%{http_code}' -o /dev/null -k --location --request POST ${ENTITLEMENT_URL}/groups/users@opendes.ibm.com/members \
    --header 'data-partition-id: opendes' \
    --header "Authorization: Bearer ${ACCESS_TOKEN}" \
    --header 'Content-Type: application/json' \
    --data-raw '{
        "email" : "'"$ROLE_EMAIL"'",
        "role" : "OWNER"
    }')

  if [[ $RESPONSE -eq 200 ]] || [[ $RESPONSE -eq 409 ]]; then
    break
  else
    sleep 5
  fi
done

echo "Registering user Roles for User osdu-bvt for Common"
while true; do
  RESPONSE=$(curl -k -s -w '%{http_code}' -o /dev/null -k --location --request POST ${ENTITLEMENT_URL}/groups/users@common.ibm.com/members \
    --header 'data-partition-id: common' \
    --header "Authorization: Bearer ${ACCESS_TOKEN}" \
    --header 'Content-Type: application/json' \
    --data-raw '{
        "email" : "'"$ROLE_EMAIL"'",
        "role" : "OWNER"
    }')

  if [[ $RESPONSE -eq 200 ]] || [[ $RESPONSE -eq 409 ]]; then
    break
  else
    sleep 5
  fi
done
echo "Script Execution is completed"