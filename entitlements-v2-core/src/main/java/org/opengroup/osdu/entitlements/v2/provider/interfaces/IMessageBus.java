package org.opengroup.osdu.entitlements.v2.provider.interfaces;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeEvent;

public interface IMessageBus {
    void publishMessage(DpsHeaders headers, EntitlementsChangeEvent... event);
}
