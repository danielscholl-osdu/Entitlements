package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity;


import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Document("Group")
public class GroupDoc extends BaseDoc {

    private String name;
    private String description = "";

    private Set<String> appIds = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getAppIds() {
        return appIds;
    }

    public void setAppIds(Set<String> appIds) {
        this.appIds = appIds;
    }

}
