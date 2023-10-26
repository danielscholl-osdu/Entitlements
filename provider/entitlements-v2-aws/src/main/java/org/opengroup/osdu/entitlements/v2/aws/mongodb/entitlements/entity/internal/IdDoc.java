/**
* Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class IdDoc {

    private String nodeId;
    private String dataPartitionId;


    public IdDoc() {
    }

    public IdDoc(String nodeId, String dataPartitionId) {
        this.nodeId = nodeId;
        this.dataPartitionId = dataPartitionId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getDataPartitionId() {
        return dataPartitionId;
    }

    public void setDataPartitionId(String dataPartitionId) {
        this.dataPartitionId = dataPartitionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof IdDoc idDoc)) return false;

        return new EqualsBuilder().append(nodeId, idDoc.nodeId).append(dataPartitionId, idDoc.dataPartitionId).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(nodeId).append(dataPartitionId).toHashCode();
    }
}
