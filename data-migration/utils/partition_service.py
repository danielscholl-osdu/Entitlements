import requests
import urllib.parse


def get_all_partitions(token, dns):
    header = request_header(token)
    response = requests.get(urllib.parse.urljoin(partition_service_url(dns), '/api/partition/v1/partitions'),
                            headers=header, timeout=30)
    if response.status_code > 299:
        raise Exception(
            'error get partitions with status_code {} and error_message {}'.format(response.status_code, response))
    return response.json()


def get_partition_info(token, partition_id, dns):
    header = request_header(token)
    response = requests.get(
        urllib.parse.urljoin(partition_service_url(dns), '/api/partition/v1/partitions/{}'.format(partition_id)),
        headers=header, timeout=30)
    if response.status_code > 299:
        raise Exception('error get partition detail of {} with status_code {} and error_message {}'.format(partition_id,
                                                                                                           response.status_code,
                                                                                                           response))
    return response.json()


def request_header(token):
    return {'Authorization': 'Bearer ' + token}


def partition_service_url(dns):
    return 'https://{}'.format(dns)
