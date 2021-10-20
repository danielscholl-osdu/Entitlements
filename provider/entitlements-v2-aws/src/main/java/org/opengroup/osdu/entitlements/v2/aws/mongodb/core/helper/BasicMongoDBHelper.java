package org.opengroup.osdu.entitlements.v2.aws.mongodb.core.helper;

import com.google.common.collect.Lists;
import com.mongodb.client.result.UpdateResult;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.entity.BasicMongoDBDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.entity.QueryPageResult;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.exception.InvalidCursorException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ExecutableUpdateOperation;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * Class basic access OSDU MongoDB database collections.
 */
//TODO: fix javadocs
public class BasicMongoDBHelper {

    private final MongoOperations mongoOperations;

    public BasicMongoDBHelper(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    /**
     * Create index for collection associated with clazz if not exists
     *
     * @param clazz class to determinate MongoDB collection.
     * @param index for creating.
     * @param <T>   class to determinate MongoDB collection.
     */
    public <T> void ensureIndex(String collectionName, Index index) {
        mongoOperations.indexOps(collectionName).ensureIndex(index);
    }

    /**
     * Check existing of document with provided field name and value.
     *
     * @param clazz      class to determinate MongoDB collection.
     * @param fieldValue for search.
     * @param fieldName  for search.
     * @return a Boolean; true if value exists in database, false if there is not.
     */
    public <T> boolean existsByField(String fieldName, Object fieldValue, Class<T> clazz) {
        return mongoOperations.exists(Query.query(Criteria.where(fieldName).is(fieldValue)), clazz);
    }

    public <T> boolean existsByQuery(Query query, String collectionName) {
        return mongoOperations.exists(query, collectionName);
    }

    /**
     * Save document into database
     *
     * @param documentToSave document for saving.
     */
    public <T> void save(T documentToSave) {
        mongoOperations.save(documentToSave);
    }

    /**
     * Create document in database
     *
     * @param documentToSave document for saving.
     */
    public <T> void insert(T documentToSave, String collectionName) {
        mongoOperations.insert(documentToSave, collectionName);
    }

    /**
     * Save documents into database
     *
     * @param documentsToSave documents for saving.
     */
    public <T> void save(Collection<T> documentsToSave) {
        if (!CollectionUtils.isEmpty(documentsToSave)) {
            mongoOperations.insertAll(documentsToSave);
        }
    }

    /**
     * Save documents into database in batches of batchSize
     *
     * @param documentsToSave documents for saving.
     * @param batchSize       maximum amount of documents that are saved in one operation
     */
    public <T> void batchSave(List<T> documentsToSave, int batchSize) {
        if (!CollectionUtils.isEmpty(documentsToSave)) {
            if (batchSize < 1) {
                throw new IllegalArgumentException(String.format("Batch size cannot be zero or less, got %d", batchSize));
            }
            Lists.partition(documentsToSave, batchSize).forEach(mongoOperations::insertAll);
        }
    }

    /**
     * Update an entity by the query
     *
     * @param query  query to search by
     * @param update an update object
     * @param clazz  entity to update
     * @return update result
     */
    public UpdateResult updateMulti(Query query, Update update, Class<?> clazz, String collectionName) {
        return mongoOperations.updateMulti(query, update, clazz, collectionName);
    }

    /**
     * Find document in database by field and it's value.
     *
     * @param clazz class to determinate MongoDB collection.
     * @return found document.
     */
    public <T> T getById(Object id, Class<T> clazz, String collectionName) {
        return mongoOperations.findById(id, clazz, collectionName);
    }

    /**
     * Find document in database by field and it's value.
     *
     * @param clazz      class to determinate MongoDB collection.
     * @param fieldName  field name to be searched by.
     * @param fieldValue field value to be searched by.
     * @return found document.
     */
    public <T> T get(String fieldName, Object fieldValue, Class<T> clazz) {
        return mongoOperations.findOne(Query.query(Criteria.where(fieldName).is(fieldValue)), clazz);
    }

    /**
     * Find list of documents in collection by field values
     *
     * @param fieldName   field name to be searched by.
     * @param fieldValues field values to be searched by.
     * @param clazz       class to determinate MongoDB collection.
     * @return found documents.
     */
    public <T> List<T> getList(String fieldName, Collection<?> fieldValues, Class<T> clazz) {
        return mongoOperations.find(Query.query(Criteria.where(fieldName).in(fieldValues)), clazz);
    }

    /**
     * Delete document from database by matching fieldName-value pair. Caution! Can delete multiple items.
     *
     * @param fieldName field name to be searched for.
     * @param value     objects with fieldName and this value will be deleted.
     * @return was any entity deleted
     */
    public <T> boolean delete(String fieldName, Object value, String collectionName) {
        return mongoOperations.remove(Query.query(Criteria.where(fieldName).is(value)), collectionName).getDeletedCount() > 0;
    }

    /**
     * Find all documents by the query
     *
     * @param clazz class to determinate MongoDB collection.
     * @param query query for search.
     */
    public <T> List<T> find(Query query, Class<T> clazz, String collectionName) {
        return mongoOperations.find(query, clazz, collectionName);
    }

    /**
     * Find one document by the query
     *
     * @param clazz class to determinate MongoDB collection.
     * @param query query for search.
     */
    public <T> T findOne(Query query, Class<T> clazz) {
        return mongoOperations.findOne(query, clazz);
    }

    /**
     * To perform update operations the following method returns API for updating
     */
    public <T> ExecutableUpdateOperation.UpdateWithQuery<T> update(Class<T> clazz, String collectionName) {
        return mongoOperations.update(clazz).inCollection(collectionName);
    }

    /**
     * Find document in database by provided query.
     *
     * @param clazz       class to determinate MongoDB collection.
     * @param searchQuery query for search.
     * @param idFieldName name of id for document used as cursor.
     * @param cursorValue current cursor value
     * @param limit       maximum returned document count for current page.
     * @return {@link QueryPageResult} found document list with cursor for next page (if page exists).
     */
    public <T extends BasicMongoDBDoc> QueryPageResult<T> queryPage(
            Query searchQuery, String idFieldName, String cursorValue, Class<T> clazz, Integer limit, String collectionName) throws InvalidCursorException {

        if (cursorValue != null) {
            if (!mongoOperations.exists(Query.query(Criteria.where(idFieldName).is(cursorValue)), clazz)) {
                throw new InvalidCursorException("The requested cursor does not exist or is invalid");
            }
            searchQuery.addCriteria(Criteria.where(idFieldName).gt(cursorValue));
        }

        searchQuery.with(Sort.by(idFieldName).ascending())
                .limit(limit);

        List<T> results = find(searchQuery, clazz, collectionName);
        String newCursor = null;

        if (!results.isEmpty() && results.size() >= limit) {
            newCursor = results.get(results.size() - 1).getCursorValue();
        }

        return new QueryPageResult<>(newCursor, results);
    }

    /**
     * Performs count operation on DB
     */
    public long count(Query query, Class<?> clazz) {
        return mongoOperations.count(query, clazz);
    }

    /**
     * Performs aggregation operation on DB
     */
    public <T> AggregationResults<T> pipeline(Aggregation aggregation, String collectionName, Class<T> outputType) {
        return mongoOperations.aggregate(aggregation, collectionName, outputType);
    }
}
