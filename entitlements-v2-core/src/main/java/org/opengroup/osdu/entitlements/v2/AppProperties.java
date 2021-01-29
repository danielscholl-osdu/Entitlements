package org.opengroup.osdu.entitlements.v2;

import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

public abstract class AppProperties {
    public static final String USERS = "service.entitlements.user";
    public static final String ADMIN = "service.entitlements.admin";
    public static final String OPS = "users.datalake.ops";

    @Value("${app.projectId}")
    private String projectId;
    @Value("${app.domain}")
    private String domain;
    @Value("${ACCEPT_HTTP:false}")
    private boolean httpAccepted;

    public String getProjectId() {
        return projectId;
    }

    public String getDomain() {
        return domain;
    }

    public boolean isHttpAccepted() {
        return httpAccepted;
    }

    public List<String> getInitialGroups() {
        List<String> initialGroups = new ArrayList<>(3);
        initialGroups.add("/provisioning/groups/datalake_user_groups.json");
        initialGroups.add("/provisioning/groups/datalake_service_groups.json");
        initialGroups.add("/provisioning/groups/data_groups.json");
        return initialGroups;
    }
}
