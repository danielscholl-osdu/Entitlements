# OSDU Entitlements Service

## Introduction

Entitlements service is used to enable authorization in OSDU Data Ecosystem. The service allows for the creation of groups. A group name defines a permission. Users who are added to that group obtain that permission.
The main motivation for entitlements service is data authorization, but the functionality enables three use cases:

- **Data groups** used for data authorization e.g.  _data.welldb.viewers_, _data.welldb.owners_
- **Service groups** used for service authorization e.g.  _service.storage.user_, _service.storage.admin_
- **User groups** used for hierarchical grouping of user and service identities e.g.  _users.datalake.viewers_, _users.datalake.editors_

For each group you can either be added as an OWNER or a MEMBER. The only difference being if you are an OWNER of a group, then you can manage the members of that group. 

### Group naming strategy

 All group identifiers (emails) will be of form {groupType}.{serviceName|resourceName}.{permission}@{partition}.{domain}.com with:

 - groupType ∈ {'data', 'service', 'users'}
 - serviceName ∈ {'storage', 'search', 'entitlements', ...}
 - resourceName ∈ {'welldb', 'npd', 'ihs', 'datalake', 'public', ...}
 - permission ∈ {'viewers', 'editors', 'admins' ...}
 - data-partition-id ∈ {'opendes', 'common', ...}
 - domain ∈ {'contoso.com' ...}

 As shown, a group is unique to each data partition. This means that access defined on a per data partition basis i.e. giving a service permission in one data partition does not give that user service permission in another data partition. See below for more information on data partitions.

#### Group naming convention

A group naming convention has been adopted,
such that the group's name should start with the word "data." for data groups; "service." for service groups; and "users." for user groups.
The group's name is case-insensitive. Please refer to [group creation guideline](#group-creation-guideline) under the API section for more details.

## Basic concepts

### Data partition

OSDU Data Ecosystem is a multi-partition solution with two layers of isolation: **data partition isolation** and **data access isolation**. A data partition represents a strong separation of all data between clients. Data access isolation achieved with dedicated ACL (access control list) per object within a given data partition.

All groups and permissions are unique at the data partition level, meaning granting permissions in one data partition has no effect on any other data partitions.

### Elementary data partition groups

When a data partition is provisioned, corresponding group created: **_users_** (e.g., _users@opendes.contoso.com_).

Group named _users_ contains all the identities that are allowed access to the data partition in question.

### Relevant OSDU Data Ecosystem headers

 To deal with data partitions and to be able to track user identities, OSDU Data Ecosystem introduces two headers:

 - **_data-partition-id_** - required header for all OSDU Data Ecosystem APIs used to determine the data partition context for the call.
 - **_correlation-id_** - optional but **recommended** header for all OSDU Data Ecosystem APIs used to trace the API call. If correlation-id value provided in the request, the same value used in the response. If no correlation-id header provided, then entitlements service generates a GUID for this field in the response.