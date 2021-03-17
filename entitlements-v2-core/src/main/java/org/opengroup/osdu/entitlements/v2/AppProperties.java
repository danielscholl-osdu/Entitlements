package org.opengroup.osdu.entitlements.v2;

import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public abstract class AppProperties {
    public static final String USERS = "service.entitlements.user";
    public static final String ADMIN = "service.entitlements.admin";
    public static final String OPS = "users.datalake.ops";

    @Value("${app.projectId}")
    private String projectId;
    @Value("${app.domain}")
    private String domain;

    public String getProjectId() {
        return projectId;
    }

    public String getDomain() {
        return domain;
    }

    /**
     * @return a list containing paths of configuration files
     */
    public abstract List<String> getInitialGroups();

    /**
     * @return a path of configuration file
     */
    public abstract String getGroupsOfServicePrincipal();
}
