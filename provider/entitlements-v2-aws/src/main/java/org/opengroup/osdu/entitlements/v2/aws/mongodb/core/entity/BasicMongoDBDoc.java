// Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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