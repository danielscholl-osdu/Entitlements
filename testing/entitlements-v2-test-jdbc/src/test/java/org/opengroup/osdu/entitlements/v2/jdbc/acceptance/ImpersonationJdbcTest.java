/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.jdbc.acceptance;

import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.entitlements.v2.acceptance.ImpersonationTest;
import org.opengroup.osdu.entitlements.v2.acceptance.util.PartitionService;
import org.opengroup.osdu.entitlements.v2.util.JdbcConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.JdbcTokenService;

@Slf4j
public class ImpersonationJdbcTest extends ImpersonationTest {


  public ImpersonationJdbcTest() {
    super(new JdbcConfigurationService(), new JdbcTokenService(),
        new PartitionService(new JdbcConfigurationService()));
  }
}
