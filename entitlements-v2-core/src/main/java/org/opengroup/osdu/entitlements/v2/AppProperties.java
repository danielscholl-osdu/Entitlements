package org.opengroup.osdu.entitlements.v2;

import lombok.Getter;
import lombok.Setter;
import org.opengroup.osdu.entitlements.v2.model.init.InitServiceDto;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public abstract class AppProperties {
    public static final String USERS = "service.entitlements.user";
    public static final String ADMIN = "service.entitlements.admin";
    public static final String OPS = "users.datalake.ops";

    @Getter
    @Value("${app.projectId}")
    private String projectId;

    @Getter
    @Value("${app.domain}")
    private String domain;

    @Getter
    @Setter
    protected InitServiceDto initServiceDto;

    /**
     * @return a list containing paths of configuration files
     */
    public abstract List<String> getInitialGroups();

    /**
     * @return a path of configuration file
     */
    public abstract String getGroupsOfServicePrincipal();

    /**
     * @return a list containing paths of configuration files
     */
    public abstract List<String> getGroupsOfInitialUsers();

    /**
     * Returns members which are protected from removal from their groups
     */
    public abstract List<String> getProtectedMembers();
}
