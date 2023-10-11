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

package org.opengroup.osdu.entitlements.v2.aws.spi;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper.GroupHelper;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper.UserHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;

public abstract class BasicEntitlementsHelper {

    public static final String ID = "_id";
    public static final String NODE_ID = "_id.nodeId";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String PARENTS = "parents";
    public static final String CHILDREN = "children";
    public static final String APP_IDS = "appIds";
    public static final String DIRECT_PARENTS = "directParents";
    public static final String MEMBER_OF = "memberOf";

    @Autowired
    protected GroupHelper groupHelper;
    @Autowired
    protected UserHelper userHelper;
    @Autowired
    protected ConversionService conversionService;

}
