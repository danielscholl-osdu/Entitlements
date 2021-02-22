import logging
import requests
import urllib.parse

logger = logging.getLogger()
logger.setLevel(logging.INFO)
fileHandler = logging.FileHandler('migration_error.log')
fileHandler.setLevel(logging.ERROR)
logger.addHandler(fileHandler)
consoleHandler = logging.StreamHandler()
logger.addHandler(consoleHandler)


def create_group_v2(group_name, partition_id, token, dns):
    header = request_header(partition_id, token)
    request_body = {'name': group_name, 'description': ''}
    logger.info('creating group {}'.format(group_name))
    try:
        response = requests.post(urllib.parse.urljoin(entitlement_v2_url(dns), '/api/entitlements/v2/groups'),
                                 headers=header, json=request_body, timeout=30)
        if response.status_code > 299 and response.status_code != 409:
            logger.error(
                'error creating group {} on partition {} with status_code {} and error_message {}'.format(group_name,
                                                                                                          partition_id,
                                                                                                          response.status_code,
                                                                                                          response.json()))
    except:
        logger.error(
            'error creating group {} on partition {} when sending request to entitlemens service'.format(group_name,
                                                                                                         partition_id))


def create_membership_v2(group_id, user_id, partition_id, token, role, dns):
    header = request_header(partition_id, token)
    request_body = {'email': user_id, 'role': role}
    logger.info('Adding {} into group {} as {}'.format(user_id, group_id, role))
    try:
        response = requests.post(
            urllib.parse.urljoin(entitlement_v2_url(dns), '/api/entitlements/v2/groups/{}/members'.format(group_id)),
            headers=header, json=request_body, timeout=30)
        if response.status_code > 299 and response.status_code != 409:
            logger.error('error adding member {} into group {} with status_code {} and error_message {}'.format(user_id,
                                                                                                                group_id,
                                                                                                                response.status_code,
                                                                                                                response.json()))
    except:
        logger.error('error adding member {} into group {} when sending request to entitlements service'.format(user_id,
                                                                                                                group_id))


def list_member_v2(group_id, partition_id, token, dns):
    header = request_header(partition_id, token)
    logger.info('listing group {}'.format(group_id))
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
    logger.info('listing group {}'.format(group_id))
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
