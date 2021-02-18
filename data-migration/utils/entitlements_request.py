import requests
import urllib.parse


def create_group_v2(group_name, partition_id, token, dns):
    header = request_header(partition_id, token)
    request_body = {'name': group_name, 'description': ''}
    print('creating group {}'.format(group_name))
    response = requests.post(urllib.parse.urljoin(entitlement_v2_url(dns), '/api/entitlements/v2/groups'),
                             headers=header, json=request_body, timeout=30)
    if response.status_code > 299 and response.status_code != 409:
        raise Exception('error creating group with status_code {} and error_message {}'.format(response.status_code,
                                                                                               response.json()))


def create_membership_v2(group_id, user_id, partition_id, token, role, dns):
    header = request_header(partition_id, token)
    request_body = {'email': user_id, 'role': role}
    print('Adding {} into group {} as {}'.format(user_id, group_id, role))
    response = requests.post(
        urllib.parse.urljoin(entitlement_v2_url(dns), '/api/entitlements/v2/groups/{}/members'.format(group_id)),
        headers=header, json=request_body, timeout=30)
    if response.status_code > 299 and response.status_code != 409:
        raise Exception('error adding member with status_code {} and error_message {}'.format(response.status_code,
                                                                                              response.json()))


def list_member_v2(group_id, partition_id, token, dns):
    header = request_header(partition_id, token)
    print('listing group {}'.format(group_id))
    response = requests.get(
        urllib.parse.urljoin(entitlement_v2_url(dns), '/api/entitlements/v2/groups/{}/members'.format(group_id)),
        headers=header, timeout=30)
    if response.status_code > 299:
        raise Exception(
            'error listing member of the group with status_code {} and error_message {}'.format(response.status_code,
                                                                                                response.json()))
    return response.json()['members']


def list_member_v1(group_id, partition_id, token, dns):
    header = request_header(partition_id, token)
    print('listing group {}'.format(group_id))
    response = requests.get(
        urllib.parse.urljoin(entitlement_v1_url(dns), '/entitlements/v1/groups/{}/members'.format(group_id)),
        headers=header, timeout=30)
    if response.status_code > 299:
        raise Exception(
            'error listing member of the group with status_code {} and error_message {}'.format(response.status_code,
                                                                                                response.json()))

    return response.json()


def request_header(partition_id, token):
    return {'Authorization': 'Bearer ' + token, 'data-partition-id': partition_id}


def entitlement_v1_url(dns):
    return 'https://{}'.format(dns)


def entitlement_v2_url(dns):
    return 'https://{}'.format(dns)


def has_admin_group(groups):
    for group_name in groups:
        if group_name == 'users.datalake.admins':
            return True
    return False
