package org.opengroup.osdu.entitlements.v2.aws.mongodb.core.entity;

import org.springframework.data.annotation.Id;

/**
 * Interface for MongoDB documents for supporting pagination.
 */
public abstract class BasicMongoDBDoc<T> {

    @Id
    private String id;

    private T data;

    public BasicMongoDBDoc() {

    }

    public BasicMongoDBDoc(T data, String id) {
        this.data = data;
        this.id = id;
    }

    /**
     * Get value of mongodb document witch used as cursor (most likely id)
     *
     * @return cursor value.
     */
    public String getCursorValue() {
        return id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}