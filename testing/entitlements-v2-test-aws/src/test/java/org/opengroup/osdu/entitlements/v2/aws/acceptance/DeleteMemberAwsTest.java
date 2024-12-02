/**
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
package org.opengroup.osdu.entitlements.v2.aws.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.DeleteMemberTest;
import org.opengroup.osdu.entitlements.v2.util.AwsConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AwsTokenService;

public class DeleteMemberAwsTest extends DeleteMemberTest {

    public DeleteMemberAwsTest() {
        super(new AwsConfigurationService(), AwsTokenService.getInstance());
    }
}
