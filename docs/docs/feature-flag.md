## "disable-data-root-group-hierarchy" partition feature flag
Entitlements service implements quota for the membership that one entity can have. The quota regulates the consumption behavior 
and protect the service. An entity could be a user, a service account or a entitlements group.

There is a bootstrap group called "users.data.root", and there is specific implementation to automatically 
add this group as children to all created data groups. The requirement behind this implementation is that 
any user or service account belong to the group "users.data.root" should have access to all data groups within 
the data partition. It also means that the user or service account have full permission to access all data 
within the data partition, since storage is using entitlements data groups as the record ACL.

Quota itself is a good thing to have, however, because of the membership design of "users.data.root" group, 
it introduces an implicit quota which limits how many data group in total that could be created in one data partition.

The feature flag "disable-data-root-group-hierarchy" determines whether the quota is enabled or not. 
So if this feature flag is set to true, "users.data.root" group will not be added as a child to all data groups.

[Related ADR](https://community.opengroup.org/osdu/platform/security-and-compliance/entitlements/-/issues/123)

## Partition feature flag default value
If the feature flag is not set for a partition, the value will be taken from applications.properties
with the following pattern: `partition.feature-flag.defaults.<feature flag name>`. If the value 
is also missing from application.properties then it will be considered `false`.