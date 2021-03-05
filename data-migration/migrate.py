import os
from azure.cosmos import CosmosClient

from utils.entitlements_request import tenant_provisioning_v2, create_group_v2, create_membership_v2, has_admin_group
from utils.get_migrate_data import get_all_groups, get_all_membership
from utils.keyvault import get_secret
from utils.partition_service import get_all_partitions, get_partition_info
from utils.token_util import generate_service_principal_token
import json

env_vault = 'https://' + os.environ['ENV_VAULT']
service_principal_id = os.environ['AZURE_CLIENT_ID']
service_principal_secret = os.environ['AZURE_CLIENT_SECRET']
azure_tenant_id = os.environ["AZURE_TENANT_ID"]
resource_id = os.environ['RESOURCE_ID']
domain = os.environ['DOMAIN']
dns = os.environ['DNS']

if __name__ == '__main__':
    # get all partition of the environment
    service_pricipal_token = generate_service_principal_token(service_principal_id, service_principal_secret,
                                                              azure_tenant_id, resource_id)
    all_partitions = get_all_partitions(service_pricipal_token, dns)
    for partition_id in all_partitions:
        # run v2 tenant provisioning API on the partition
        tenant_provisioning_v2(partition_id, service_pricipal_token, dns)
        # get partition info
        partition_info = get_partition_info(service_pricipal_token, partition_id, dns)
        cosmos_client = CosmosClient(get_secret(env_vault, partition_info['cosmos-endpoint']['value']),
                                     credential=get_secret(env_vault, partition_info['cosmos-primary-key']['value']))
        db = cosmos_client.get_database_client('osdu-db')
        # get all data from v1 cosmos db
        all_groups = get_all_groups(db, partition_id)
        all_membership = get_all_membership(db)
        # migrate groups
        for group_name in all_groups:
            create_group_v2(group_name, partition_id, service_pricipal_token, dns)
        # migrate members
        for membership in all_membership:
            user_id = membership['id']
            for tenant in membership['tenants']:
                if tenant['name'] == partition_id:
                    # By design, all v1 admins will be migrated into groups as OWNER, otherwise, will migrated as MEMBER
                    role = 'MEMBER'
                    if has_admin_group(tenant['groups']):
                        role = 'OWNER'
                    service_pricipal_token = generate_service_principal_token(service_principal_id,
                                                                              service_principal_secret, azure_tenant_id,
                                                                              resource_id)
                    for group_name in tenant['groups']:
                        group_id = '{}@{}.{}'.format(group_name, partition_id, domain)
                        create_membership_v2(group_id, user_id, partition_id, service_pricipal_token, role, dns)
        # migrate data admins
        with open('DataAdminServiceAccountsList.json') as f:
            all_data_admins = json.load(f)
        data_admin_group_id = 'users.data.root@{}.{}'.format(partition_id, domain)
        for data_admin in all_data_admins:
            create_membership_v2(data_admin_group_id, data_admin, partition_id, service_pricipal_token, 'MEMBER', dns)