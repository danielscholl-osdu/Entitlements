package org.opengroup.osdu.entitlements.v2.aws.mongodb.core.entity;

import java.util.List;

public class QueryPageResult<T> {
    private String cursor;
    private List<T> results;

    public QueryPageResult(String cursor, List<T> results) {
        this.cursor = cursor;
        this.results = results;
    }

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    public List<T> getResults() {
        return results;
    }

    public void setResults(List<T> results) {
        this.results = results;
    }
}
