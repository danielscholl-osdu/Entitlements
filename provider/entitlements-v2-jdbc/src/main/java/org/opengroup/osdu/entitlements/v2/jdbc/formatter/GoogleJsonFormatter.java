/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
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

package org.opengroup.osdu.entitlements.v2.jdbc.formatter;

import ch.qos.logback.contrib.jackson.JacksonJsonFormatter;
import java.io.IOException;
import java.util.Map;

public class GoogleJsonFormatter extends JacksonJsonFormatter {

  private static final String SEVERITY = "severity";
  private static final String LEVEL = "level";

  @Override
  public String toJsonString(Map map) throws IOException {
    map.put(SEVERITY, map.remove(LEVEL));
    return super.toJsonString(map);
  }
}
