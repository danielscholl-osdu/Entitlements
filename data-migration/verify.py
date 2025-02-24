import os
from azure.cosmos import CosmosClient

from utils.entitlements_request import list_member_v1, list_member_v2
from utils.get_migrate_data import get_all_groups
from utils.keyvault import get_secret
from utils.partition_service import get_all_partitions, get_partition_info
from utils.token_util import generate_service_principal_token

env_vault = 'https://' + os.environ['ENV_VAULT']
service_principal_id = os.environ['AZURE_CLIENT_ID']
service_principal_secret = os.environ['AZURE_CLIENT_SECRET']
azure_tenant_id = os.environ["AZURE_TENANT_ID"]
resource_id = os.environ['RESOURCE_ID']
domain = os.environ['DOMAIN']
dns = os.environ['DNS']


def v2_constains_all_v1_members(v1_members, v2_members):
    all_v1_members = []
    for member in v1_members:
        all_v1_members.append(member)
    print(all_v1_members)
    all_v2_members = []
    for member in v2_members:
        all_v2_members.append(member['email'])
    print(all_v2_members)
    if all(x in all_v2_members for x in all_v1_members):
        return True


if __name__ == '__main__':
    # get all partition of the environment
    service_pricipal_token = generate_service_principal_token(service_principal_id, service_principal_secret,
                                                              azure_tenant_id, resource_id)
    all_partitions = get_all_partitions(service_pricipal_token, dns)
    for partition_id in all_partitions:
        # get partition info
        partition_info = get_partition_info(service_pricipal_token, partition_id, dns)
        cosmos_client = CosmosClient(get_secret(env_vault, partition_info['cosmos-endpoint']['value']),
                                     credential=get_secret(env_vault, partition_info['cosmos-primary-key']['value']))
        db = cosmos_client.get_database_client('osdu-db')
        all_groups = get_all_groups(db, partition_id)
        for group_name in all_groups:
            group_id = '{}@{}.{}'.format(group_name, partition_id, domain)
            all_v1_members = list_member_v1(group_id, partition_id, service_pricipal_token, dns)
            all_v2_members = list_member_v2(group_id, partition_id, service_pricipal_token, dns)
            if not v2_constains_all_v1_members(all_v1_members, all_v2_members):
                raise Exception('{} groups in v2 does not contain all v1 members'.format(group_id))
