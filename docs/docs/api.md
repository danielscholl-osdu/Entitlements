# Entitlement Service API

See [OpenAPI Spec](https://community.opengroup.org/osdu/platform/security-and-compliance/entitlements/-/blob/master/docs/api/entitlements_openapi.yaml?ref_type=heads) for additional details on APIs.

## Open API 3.0 - Swagger
- Swagger UI : https://host/context-path/swagger (will redirect to https://host/context-path/swagger-ui/index.html)
- api-docs (JSON) : https://host/context-path/api-docs
- api-docs (YAML) : https://host/context-path/api-docs.yaml

## API
*  **GET /entitlements/v1/groups** - Retrieves all the groups for the user or service extracted from JWT in Authorization
header for the data partition provided in _data-partition-id_ header. This API gives the flat list of the groups
(including all hierarchical groups) that user belongs to.

Optional query parameter 'roleRequired' when passed gives the additional information of a user's Role in the groups he's part of. Default value of this flag is set to `false`, thus if not present, no Role information is fetched.

<details><summary>Curl Get Groups</summary>

```
curl --request GET \
  --url '/entitlements/v1/groups?roleRequired=true' \
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

<details><summary>Curl Post Groups</summary>

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

Optional query parameter 'roleRequired' when passed gives us additional parameter in the response containing the user's Role in the groups he's member of. Default value of this flag is set to `false`, thus if not present, no Role information is fetched.

<details><summary>Curl Get Members</summary>

```
curl --request GET \
  --url '/entitlements/v1/groups/service.example.viewers@opendes.contoso.com/members?includeType=false?roleRequired=true' \
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

<details><summary>Curl Post Members</summary>

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

<details><summary>Curl Delete Members</summary>

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

<details><summary>Curl Delete Groups</summary>

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

<details><summary>Curl Get Group</summary>

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

<details><summary>Curl Patch Group</summary>

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

## OSDU Data Ecosystem user groups

OSDU Data Ecosystem user groups provides an abstraction from permission and user management. Clients or services can be directly added to the user groups to gain the permissions associated with that user group. The following user groups exists by default:

- **users.datalake.viewers** used for viewer level authorization for OSDU Data Ecosystem services.

- **users.datalake.editors** used for editor level authorization for OSDU Data Ecosystem services and authorization to create the data using OSDU Data Ecosystem storage service.

- **users.datalake.admins** used for admin level authorization for OSDU Data Ecosystem services.

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

## Version info endpoint
For deployment available public `/info` endpoint, which provides build and git related information.

#### Example version response:
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
