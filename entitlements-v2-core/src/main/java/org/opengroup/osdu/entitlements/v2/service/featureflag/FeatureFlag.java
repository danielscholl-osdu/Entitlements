package org.opengroup.osdu.entitlements.v2.service.featureflag;

public enum FeatureFlag {
    /**
     * Feature flag to control whether automatically adding users.data.root group as children to all data group
     */
    DISABLE_DATA_ROOT_GROUP_HIERARCHY("disable-data-root-group-hierarchy");
    public final String label;

    private FeatureFlag(String label) {
        this.label = label;
    }
}

