
## Table of contents <a name="TOC"></a>
- [Introduction](#introduction)
  * [Entitlement Service API](#entitlement-service-api)
- [OSDU Data Ecosystem user groups](#datalake-user-groups)
- [Version info endpoint](#version-info-endpoint)

## Introduction<a name="introduction"></a>

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

[Back to table of contents](#TOC)

## Basic concepts

### Data partition

OSDU Data Ecosystem is a multi-partition solution with two layers of isolation: **data partition isolation** and **data access isolation**. A data partition represents a strong separation of all data between clients. Data access isolation achieved with dedicated ACL (access control list) per object within a given data partition.

All groups and permissions are unique at the data partition level, meaning granting permissions in one data partition has no effect on any other data partitions.

### Elementary data partition groups

When a data partition is provisioned, corresponding group created: **_users_** (e.g., _users@opendes.contoso.com_).

Group named _users_ contains all the identities that are allowed access to the data partition in question.

### <a name="header">Relevant OSDU Data Ecosystem headers</a>

 To deal with data partitions and to be able to track user identities, OSDU Data Ecosystem introduces two headers:

 - **_data-partition-id_** - required header for all OSDU Data Ecosystem APIs used to determine the data partition context for the call.
 - **_correlation-id_** - optional header for all OSDU Data Ecosystem APIs used to trace the API call. If correlation-id value provided in the request, the same value used in the response. If no correlation-id header provided, then entitlements service generates a GUID for this field in the response.

[Back to table of contents](#TOC)

## Entitlement Service API<a name="entitlement-service-api"></a>

*  **GET /entitlements/v1/groups** - Retrieves all the groups for the user or service extracted from JWT in Authorization
header for the data partition provided in _data-partition-id_ header. This API gives the flat list of the groups
(including all hierarchical groups) that user belongs to.

<details>

```
curl --request GET \
  --url '/entitlements/v1/groups' \
  --header 'authorization: Bearer <JWT>' \
  --header 'content-type: application/json' \
  --header 'data-partition-id: opendes, common'
```
</details>
&nbsp;

*  **POST /entitlements/v1/groups** - Creates the group within the data partition provided in _data-partition-id_ header.
This API will create a group with following email {name}@{data-partition-id}.{domain}.com, where {data-partition-id} is
received from _data-partition_id_ header. The user or service extracted from JWT in _Authorization_ header made an OWNER
of the group. The user or service must belong to service.entitlements.admin@{data-partition-id}.{domain}.com group.
This API will be mainly used to create service and data groups.

   Group creation guidelines: <a name="group-creation-guideline"></a>
   - **Data groups** used for data authorization e.g. of group name is : data.{resourceName}.{permission}@{data-partition-id}.{domain}.com
   - **Service groups** used for service authorization e.g. of group name is : service.{serviceName}.{permission}@{data-partition-id}.{domain}.com
   - **User groups** used for hierarchical grouping of user and service identities e.g. of group name is : users.{serviceName}.{permission}@{data-partition-id}.{domain}.com

<details>

```
curl --request POST \
  --url '/entitlements/v1/groups' \
  --header 'authorization: Bearer <JWT>' \
  --header 'content-type: application/json' \
  --header 'data-partition-id: opendes' \
  --data '{
             "name": "service.example.viewers",
             "description": "This is an service group for example service which has viewer permission."
         }'
```
</details>
&nbsp;

*  **GET /entitlements/v1/groups/{group_email}/members** - Retrieves members that belong to a group_email within the data partition provided in _data-partition-id_ header.
E.g. group_email value is {name}@{data-partition-id}.{domain}.com. Query parameter role can be specified to filter group
members by role of OWNER or MEMBER. The user or service extracted from JWT in _Authorization_ header checked for
membership within group_email as the OWNER or within users.datalake.admins group or within users.datalake.ops group. This API lists the direct members of
the group (excluding hierarchical groups). When we need to get all members in the hierarchy, client needs to implement
its own recursive function, but "includeType=true" query parameter could be useful to determine whether a member is a USER or GROUP.

<details>

```
curl --request GET \
  --url '/entitlements/v1/groups/service.example.viewers@opendes.contoso.com/members?includeType=false' \
  --header 'authorization: Bearer <JWT>' \
  --header 'content-type: application/json' \
  --header 'data-partition-id: opendes'
```
</details>
&nbsp;

*  **POST /entitlements/v1/groups/{group_email}/members** - Adds members to a group with group_email within the data partition provided in _data-partition-id_ header.
The member added can either be a _user_ or a _group_. E.g. group_email value is {name}@{data-partition-id}.{domain}.com.
Member body needs to have an email and role for a member. Member role can be OWNER or MEMBER. The user or service extracted from JWT in _Authorization_ header
checked for OWNER role membership within group_email or within users.datalake.ops (ops user) group. In case the user is not ops user, it should be within service.entitlements.user
or service.entitlements.admin group.

<details>

```
curl --request POST \
  --url '/entitlements/v1/groups/service.example.viewers@opendes.contoso.com/members' \
  --header 'authorization: Bearer <JWT>' \
  --header 'content-type: application/json' \
  --header 'data-partition-id: opendes' \
  --data '{
            "email": "member@domain.com",
            "role": "MEMBER"
          }'
```
</details>
&nbsp;

*  **DELETE /entitlements/v1/groups/{group_email}/members/{member_email}** - Deletes a member from a group with email group_email within the data partition provided in _data-partition-id_ header.
The member deleted can either be a _user_ or a _group_. E.g. group_email value is {name}@{data-partition-id}.{domain}.com.
Path parameter member_email needs an email of a member. The user or service extracted from JWT in _Authorization_ header checked for OWNER role membership within group_email
and within users.datalake.ops (ops user) group.

<details>

```
curl --request DELETE \
  --url '/entitlements/v1/groups/service.example.viewers@opendes.contoso.com/members/member@domain.com' \
  --header 'authorization: Bearer <JWT>' \
  --header 'content-type: application/json' \
  --header 'data-partition-id: opendes'
```
</details>
&nbsp;

*  **DELETE /groups/{group_email}** - Deletes entitlements group. The user or service extracted from JWT in _Authorization_ header checked for membership
within group_email as the OWNER or within users.datalake.ops (ops user) group. In case the user is not ops user, it should be within service.entitlements.admin group.

<details>

```
curl --request DELETE \
  --url '/entitlements/v1/groups/data.test.viewers@opendes.contoso.com' \
  --header 'authorization: Bearer <JWT>' \
  --header 'content-type: application/json' \
  --header 'data-partition-id: opendes' \
```
</details>
&nbsp;


*  **GET /entitlements/v1/groups/{group_email}/membersCount** - Retrieves the count of members that belong to a group_email within the data partition provided in _data-partition-id_ header.
   E.g. group_email value is {name}@{data-partition-id}.{domain}.com. Query parameter role can be specified to filter group
   members by role of OWNER or MEMBER. The user or service extracted from JWT in _Authorization_ header checked for
   membership within group_email as the OWNER or within users.datalake.admins group or within users.datalake.ops group. This API lists the count of direct members of
   the group (excluding hierarchical groups).

<details>

```
curl --request GET \
  --url '/entitlements/v1/groups/service.example.viewers@opendes.contoso.com/membersCount?role=OWNER' \
  --header 'authorization: Bearer <JWT>' \
  --header 'content-type: application/json' \
  --header 'data-partition-id: opendes'
```
</details>

* **PATCH /groups/{group_email}** - Updates entitlements group. The user or service extracted from JWT in _Authorization_ header checked for membership
within group_email as the OWNER or within users.datalake.ops (ops user) group. In case user is not ops user, it should be within service.entitlements.admin
or service.entitlements.user group.

<details>

```
curl --request PATCH \
  --url '/entitlements/v1/groups/data.test.viewers@opendes.contoso.com' \
  --header 'authorization: Bearer <JWT>' \
  --header 'content-type: application/json' \
  --header 'data-partition-id: opendes' \
  --data '{
            "op": "replace",
            "path": "/appIds",
            "value": ["app1", "app2"]
          }'
```
</details>
&nbsp;

[Back to table of contents](#TOC)

## OSDU Data Ecosystem user groups<a name="datalake-user-groups"></a>

OSDU Data Ecosystem user groups provides an abstraction from permission and user management. Clients or services can be directly added to the user groups to gain the permissions associated with that user group. The following user groups exists by default:

- **users.datalake.viewers** used for viewer level authorization for OSDU Data Ecosystem services.

- **users.datalake.editors** used for editor level authorization for OSDU Data Ecosystem services and authorization to create the data using OSDU Data Ecosystem storage service.

- **users.datalake.admins** used for admin level authorization for OSDU Data Ecosystem services.

[Back to table of contents](#TOC)

### Permissions

| **_Endpoint URL_** | **_Method_** | **_Minimum Permissions Required_** |
| --- | --- | --- |
| /entitlements/v1/groups | GET | service.entitlements.user |
| /entitlements/v1/groups | POST | service.entitlements.admin |
| /entitlements/v1/groups/{group_email} | DELETE | service.entitlements.admin |
| /entitlements/v1/groups/{group_email} | PATCH | service.entitlements.user |
| /entitlements/v1/groups/{group_email}/members | GET | service.entitlements.user |
| /entitlements/v1/groups/{group_email}/members | POST | service.entitlements.user |
| /entitlements/v1/groups/{group_email}/members/{member_email} | DELETE | service.entitlements.user |

[Back to table of contents](#TOC)

## Version info endpoint
For deployment available public `/info` endpoint, which provides build and git related information.
#### Example response:
```json
{
    "groupId": "org.opengroup.osdu",
    "artifactId": "storage-gcp",
    "version": "0.10.0-SNAPSHOT",
    "buildTime": "2021-07-09T14:29:51.584Z",
    "branch": "feature/GONRG-2681_Build_info",
    "commitId": "7777",
    "commitMessage": "Added copyright to version info properties file",
    "connectedOuterServices": [
      {
        "name": "elasticSearch",
        "version":"..."
      },
      {
        "name": "postgresSql",
        "version":"..."
      },
      {
        "name": "redis",
        "version":"..."
      }
    ]
}
```
This endpoint takes information from files, generated by `spring-boot-maven-plugin`,
`git-commit-id-plugin` plugins. Need to specify paths for generated files to matching
properties:
- `version.info.buildPropertiesPath`
- `version.info.gitPropertiesPath`

[Back to table of contents](#TOC)