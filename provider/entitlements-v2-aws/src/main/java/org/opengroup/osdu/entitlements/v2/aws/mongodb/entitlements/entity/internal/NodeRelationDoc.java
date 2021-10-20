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

package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opengroup.osdu.entitlements.v2.model.Role;

/**
 * Describes a relation to other node with a parameter that required to describe the connection
 * Currently, the only parameter that is required is role
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

public class NodeRelationDoc {
    private IdDoc parentId;
    private Role role;

    public IdDoc getParentId() {
        return parentId;
    }

    public void setParentId(IdDoc parentId) {
        this.parentId = parentId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof NodeRelationDoc)) return false;

        NodeRelationDoc that = (NodeRelationDoc) o;

        return new EqualsBuilder().append(parentId, that.parentId).append(role, that.role).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(parentId).append(role).toHashCode();
    }
}
