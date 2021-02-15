import requests

def create_group(group_name, partition_id, token, dns):
  header = request_header(partition_id, token)
  print(partition_id)
  print(token)
  request_body = {'name': group_name, 'description': ''}
  print('creating group {}'.format(group_name))
  response = requests.post(entitlement_url(dns) + '/groups', headers=header, json=request_body, timeout=30)
  if response.status_code > 299 and response.status_code != 409:
    raise Exception('error creating group with status_code {} and error_message {}'.format(response.status_code, response))


def create_membership(group_id, user_id, partition_id, token, role, dns):
  header = request_header(partition_id, token)
  request_body = {'email': user_id, 'role': role}
  print('Adding {} into group {} as {}'.format(user_id, group_id, role))
  response = requests.post(entitlement_url(dns) + '/group/' + group_id + '/members', headers=header, json=request_body, timeout=30)
  if response.status_code > 299 and response.status_code != 409:
    raise Exception('error creating group with status_code {} and error_message {}'.format(response.status_code, response))


def request_header(partition_id, token):
  return {'Authorization': 'Bearer ' + token, 'data-partition-id': partition_id}


def entitlement_url(dns):
  return 'https://{}/api/entitlements/v2'.format(dns)


def has_admin_group(groups):
  for group_name in groups:
    if group_name == 'users.datalake.admins':
      return True
  return False