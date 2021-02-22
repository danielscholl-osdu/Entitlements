def get_all_groups(db, partition_id):
    container = db.get_container_client('TenantInfo')
    all_tenant_info = container.read_all_items()
    for item in all_tenant_info:
        if item['id'] == partition_id:
            return item['groups']


def get_all_membership(db):
    container = db.get_container_client('UserInfo')
    all_items = container.read_all_items()
    return all_items
