package org.opengroup.osdu.entitlements.v2.acceptance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionInfo {
  private String groupId;
  private String artifactId;
  private String version;
  private String buildTime;
  private String branch;
  private String commitId;
  private String commitMessage;
}
