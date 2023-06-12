package org.opengroup.osdu.entitlements.v2.azure.servicebus;

import org.opengroup.osdu.azure.publisherFacade.MessagePublisher;
import org.opengroup.osdu.azure.publisherFacade.PublisherInfo;
import org.opengroup.osdu.core.common.exception.CoreException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.status.Message;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.opengroup.osdu.entitlements.v2.azure.config.PublisherConfig;
import org.opengroup.osdu.entitlements.v2.azure.config.ServiceBusConfig;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Component
public class MessageBusImpl implements IEventPublisher {
    @Autowired
    private ServiceBusConfig serviceBusConfig;
    @Autowired
    private MessagePublisher messagePublisher;
    @Autowired
    private PublisherConfig publisherConfig;

    @Override
    public void publish(Message[] messages, Map<String, String> headersMap) throws CoreException {
        final int BATCH_SIZE = Integer.parseInt(publisherConfig.getPubSubBatchSize());
        for (int i = 0; i < messages.length; i += BATCH_SIZE) {
            EntitlementsChangeEvent[] batch = (EntitlementsChangeEvent[]) Arrays.copyOfRange(messages, i, Math.min(messages.length, i + BATCH_SIZE));
            PublisherInfo publisherInfo = PublisherInfo.builder()
                    .batch(batch)
                    .serviceBusTopicName(serviceBusConfig.getEntitlementsChangeServiceBusTopic())
                    .build();
            DpsHeaders headers = DpsHeaders.createFromMap(headersMap);

            messagePublisher.publishMessage(headers, publisherInfo, Optional.empty());
        }
    }
}
