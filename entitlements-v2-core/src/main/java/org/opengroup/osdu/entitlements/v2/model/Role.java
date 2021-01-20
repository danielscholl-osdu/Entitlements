package org.opengroup.osdu.entitlements.v2.model;

public enum Role {

    MEMBER("MEMBER"),
    OWNER("OWNER");


    private final String name;

    Role(String name) {
        this.name = name;
    }

    public String getValue() {
        return name;
    }

}
