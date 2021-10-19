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

        if (!(o instanceof IdDoc)) return false;

        IdDoc idDoc = (IdDoc) o;

        return new EqualsBuilder().append(nodeId, idDoc.nodeId).append(dataPartitionId, idDoc.dataPartitionId).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(nodeId).append(dataPartitionId).toHashCode();
    }
}
