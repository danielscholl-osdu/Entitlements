# Bootstrap groups structure

### Introduction
In order to bootstrap the minimum permission groups for a new partition to be functioning, the entitlements service implements the POST /tenant-provisioning API to bootstrap the basic service permission groups for a new partition.

The API is idempotent and needs to be executed for new provisioned partitions before the partition can be used.

The bootstrap groups and relationships created by this API are not allowed to be deleted.

### group structure

![](bootstrap-hierarchy.png)

The following table illustrates all the groups created in the POST /tenant-provisioning API

| Group Category | Group Names | Description |
| -------------- | ----------- | ----------- |
| Root user group | users | A group contains all users of the partition, the identity needs to belong to this group to access the partition |
| Data admin group | users.data.root | A group will be automatically added to all data groups so that the member of it has the permission to all data on the partition |
| Basic Core permission groups | users.datalake.ops <br> users.datalake.admins <br> users.datalake.editors <br> users.datalake.viewers | The wrapped core permission groups, the identity needs to belong to one of them to access the partition |
| Default data group | data.default.owners <br> data.default.viewers | The default data groups for a partition and all users of the partition can access it |
| Service groups | service.storage.admin <br> service.storage.creator <br> service.storage.viewer <br> service.search.admin <br> service.search.user <br> service.entitlements.admin <br> service.entitlements.user <br> service.legal.admin <br> service.legal.editor <br> service.legal.user <br> service.plugin.user <br> service.messaging.user <br> service.schema-service.admin <br> service.schema-service.editors <br> service.schema-service.viewers <br> service.file.editors <br> service.file.viewers <br> service.workflow.admin <br> service.workflow.creator <br> service.workflow.viewer <br> service.document.viewer <br> service.index-document.user <br> service.content-extractor.user <br> service.gis-dl-transformation.user <br> service.gis-dl-ingestion.user <br> service.image-classification-classify.user <br> service.image-classification-train.user <br> service.form-extractor.user <br> service.mapping-service.editors <br> service.mapping-service.viewers | Service groups for different OSDU services |

The following table illustrates all and relationships created in the POST /tenant-provisioning API

| Child group | Parent groups |
| ----------- | ------------- |
| users | data.default.owners <br> data.default.viewers |
| users.datalake.viewers | service.storage.viewer <br> service.search.user <br> service.entitlements.user <br> service.legal.user <br> service.plugin.user <br> service.messaging.user <br> service.schema-service.viewers <br> service.file.viewers <br> service.workflow.viewer <br> service.document.viewer <br> service.mapping-service.viewers |
| users.datalake.editors | __All parents of users.datalake.viewers__ <br> service.storage.creator <br> service.legal.editor <br> service.schema-service.editors <br> service.file.editors <br> service.workflow.creator <br> service.index-document.user <br> service.content-extractor.user <br> service.gis-dl-transformation.user <br> service.gis-dl-ingestion.user <br> service.image-classification-classify.user <br> service.image-classification-train.user <br> service.form-extractor.user <br> service.mapping-service.editors |
| users.datalake.admins | __All parents of users.datalake.editors__ <br> service.search.admin <br> service.entitlements.admin <br> service.workflow.admin |
| users.datalake.ops | __All parents of users.datalake.admins__ <br> service.storage.admin <br> service.legal.admin <br> service.schema-service.admins |