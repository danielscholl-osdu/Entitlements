package org.opengroup.osdu.entitlements.v2.aws.Util;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.BaseDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.opengroup.osdu.entitlements.v2.aws.spi.BasicEntitlementsHelper.ID;

public final class MongoTemplateHelper extends DbUtil {
    MongoTemplate mongoTemplate;

    public MongoTemplateHelper(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @SafeVarargs
    public final <T extends BaseDoc> void insert(T... document) {
        Arrays.stream(document).forEach(t -> {
                    if (t instanceof GroupDoc) {
                        mongoTemplate.insert(t, GROUP_DOC_COLLECTION_PREFIX + DbUtil.DATA_PARTITION);
                    }
                    if (t instanceof UserDoc) {
                        mongoTemplate.insert(t, USER_DOC_COLLECTION_PREFIX + DbUtil.DATA_PARTITION);
                    }
                }
        );
    }

    public <T extends BaseDoc> void insert(Collection<T> documents) {
        documents.forEach(t -> {
                    if (t instanceof GroupDoc) {
                        mongoTemplate.insert(t, GROUP_DOC_COLLECTION_PREFIX + DbUtil.DATA_PARTITION);
                    }
                    if (t instanceof UserDoc) {
                        mongoTemplate.insert(t, USER_DOC_COLLECTION_PREFIX + DbUtil.DATA_PARTITION);
                    }
                }
        );
    }

    public <T extends BaseDoc> T findById(IdDoc idDoc, Class<T> clazz) {
        T document = null;
        if (UserDoc.class.equals(clazz)) {
            document = mongoTemplate.findById(idDoc, clazz, USER_DOC_COLLECTION_PREFIX + idDoc.getDataPartitionId());
        }
        if (GroupDoc.class.equals(clazz)) {
            document = mongoTemplate.findById(idDoc, clazz, GROUP_DOC_COLLECTION_PREFIX + idDoc.getDataPartitionId());
        }
        return document;
    }

    public <T extends BaseDoc> List<T> findAll(Class<T> clazz) {
        List<T> documents = null;
        if (UserDoc.class.equals(clazz)) {
            documents = mongoTemplate.findAll(clazz, USER_DOC_COLLECTION_PREFIX + DATA_PARTITION);
        }
        if (GroupDoc.class.equals(clazz)) {
            documents = mongoTemplate.findAll(clazz, GROUP_DOC_COLLECTION_PREFIX + DATA_PARTITION);
        }
        return documents;
    }

    public void dropCollections() {
        mongoTemplate.getCollectionNames().forEach(mongoTemplate::dropCollection);
    }

    public Set<EntityNode> findByIds(List<IdDoc> idDocs) {
        Set<EntityNode> userNodes = mongoTemplate.find(
                Query.query(Criteria.where(ID).in(idDocs)),
                UserDoc.class,
                USER_DOC_COLLECTION_PREFIX + DATA_PARTITION).stream().map(DbUtil::convertUserDocToNode).collect(Collectors.toSet());
        Set<EntityNode> groupNodes = mongoTemplate.find(
                Query.query(Criteria.where(ID).in(idDocs)),
                GroupDoc.class,
                GROUP_DOC_COLLECTION_PREFIX + DATA_PARTITION).stream().map(DbUtil::convertGroupDocToNode).collect(Collectors.toSet());
        userNodes.addAll(groupNodes);
        return userNodes;
    }
}
