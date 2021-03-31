// Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package org.opengroup.osdu.entitlements.v2.aws.spi.addmember;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.springframework.stereotype.Repository;



import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class AwsAddMemberRepo implements org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo {


    @Override
    public Set<String> addMember(EntityNode groupNode, AddMemberRepoDto addMemberRepoDto) {

        return new HashSet<>();
    }

    @Override
    public Set<String> addMember(Deque<Operation> executedCommandsDeque, EntityNode groupEntityNode, AddMemberRepoDto addMemberRepoDto) {
        return new HashSet<>();
    }
}
