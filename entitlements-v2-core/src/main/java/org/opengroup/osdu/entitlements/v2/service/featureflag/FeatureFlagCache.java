package org.opengroup.osdu.entitlements.v2.service.featureflag;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FeatureFlagCache {
    private Map<String, VmCache<String, Boolean>> featureFlags;

    public FeatureFlagCache()  {
        this.featureFlags = new HashMap<>();
        for (FeatureFlag ff : FeatureFlag.values()) {
            this.featureFlags.put(ff.label, new VmCache<>(300, 1000));
        }
    }

    private boolean containsKey(final String ffName, final String key) {
        return this.featureFlags.get(ffName).get(key) != null;
    }

    public Boolean getFeatureFlag(final String ffName, final String key) {
        if (this.containsKey(ffName, key)) {
            return this.featureFlags.get(ffName).get(key);
        }
        return null;
    }

    public void setFeatureFlag(final String ffName, final String key, final boolean value) {
        this.featureFlags.get(ffName).put(key, value);
    }
}
