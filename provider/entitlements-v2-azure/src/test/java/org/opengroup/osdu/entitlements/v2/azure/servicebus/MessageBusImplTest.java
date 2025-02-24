package org.opengroup.osdu.entitlements.v2.azure.servicebus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.publisherFacade.MessagePublisher;
import org.opengroup.osdu.azure.publisherFacade.PublisherInfo;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.azure.config.PublisherConfig;
import org.opengroup.osdu.entitlements.v2.azure.config.ServiceBusConfig;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeAction;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeEvent;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeType;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class MessageBusImplTest {

    @InjectMocks
    private MessageBusImpl messageBusImpl;

    @Mock
    private ServiceBusConfig serviceBusConfig;

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private PublisherConfig publisherConfig;

    @Test
    public void shouldPublishMessage() {
        when(publisherConfig.getPubSubBatchSize()).thenReturn("1");
        when(serviceBusConfig.getEntitlementsChangeServiceBusTopic()).thenReturn("entitlements-changed");
        EntitlementsChangeEvent[] messages = {
                EntitlementsChangeEvent.builder()
                        .kind(EntitlementsChangeType.groupChanged)
                        .group("data.x@common.contoso.com")
                        .user("member@contoso.com")
                        .action(EntitlementsChangeAction.add)
                        .modifiedBy("requesterid")
                        .modifiedOn(1291371330000L).build()
        };
        Map<String, String> headersMap = Collections.singletonMap("testKey", "testValue");
        DpsHeaders headers = DpsHeaders.createFromMap(headersMap);
        PublisherInfo publisherInfo = PublisherInfo.builder()
                .batch(messages)
                .serviceBusTopicName("entitlements-changed")
                .build();

        messageBusImpl.publish(messages, headersMap);

        verify(messagePublisher, times(1)).publishMessage(any(DpsHeaders.class), any(PublisherInfo.class), any(Optional.class));

    }
}