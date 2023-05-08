package org.opengroup.osdu.entitlements.v2.azure.servicebus;

import org.opengroup.osdu.azure.publisherFacade.MessagePublisher;
import org.opengroup.osdu.azure.publisherFacade.PublisherInfo;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.azure.config.PublisherConfig;
import org.opengroup.osdu.entitlements.v2.azure.config.ServiceBusConfig;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeEvent;
import org.opengroup.osdu.entitlements.v2.provider.interfaces.IMessageBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class MessageBusImpl implements IMessageBus {
    @Autowired
    private ServiceBusConfig serviceBusConfig;
    @Autowired
    private MessagePublisher messagePublisher;
    @Autowired
    private PublisherConfig publisherConfig;

    @Override
    public void publishMessage(DpsHeaders headers, EntitlementsChangeEvent... event) {
        final int BATCH_SIZE = Integer.parseInt(publisherConfig.getPubSubBatchSize());
        for (int i = 0; i < event.length; i += BATCH_SIZE) {
            EntitlementsChangeEvent[] batch = Arrays.copyOfRange(event, i, Math.min(event.length, i + BATCH_SIZE));
            PublisherInfo publisherInfo = PublisherInfo.builder()
                    .batch(batch)
                    .serviceBusTopicName(serviceBusConfig.getEntitlementsChangeServiceBusTopic())
                    .build();

            messagePublisher.publishMessage(headers, publisherInfo, Optional.empty());
        }
    }
}
