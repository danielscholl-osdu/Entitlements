package org.opengroup.osdu.entitlements.v2.model;

public enum NodeType {
    USER ("USER"),
    GROUP ("GROUP");

    private final String name;

    NodeType(String name) {
        this.name = name;
    }

    public String getValue() {
        return name;
    }
}
