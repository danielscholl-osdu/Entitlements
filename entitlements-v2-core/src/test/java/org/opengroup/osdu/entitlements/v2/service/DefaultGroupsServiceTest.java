package org.opengroup.osdu.entitlements.v2.service;

import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.util.FileReaderService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;

import java.util.Collections;

public class DefaultGroupsServiceTest {
    private final String GROUPS = "{\n" +
            "  \"groups\": [\n" +
            "    {\n" +
            "      \"name\": \"group1\",\n" +
            "      \"description\": \"desc1\",\n" +
            "      \"members\": [\n" +
            "        {\n" +
            "          \"name\": \"member1\"\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"group2\",\n" +
            "      \"description\": \"desc2\",\n" +
            "      \"members\": [\n" +
            "        {\n" +
            "          \"name\": \"member2\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    @Test
    public void shouldSuccessfullyLoadGroupNames() throws Exception {
        FileReaderService fileReaderService = Mockito.mock(FileReaderService.class);
        AppProperties appProperties = Mockito.mock(AppProperties.class);
        Mockito.when(appProperties.getInitialGroups()).thenReturn(Collections.singletonList("/provisioning/groups/groups.json"));
        Mockito.when(fileReaderService.readFile("/provisioning/groups/groups.json"))
                .thenReturn(GROUPS);
        DefaultGroupsService defaultGroupsService = new DefaultGroupsService(fileReaderService, appProperties);
        Whitebox.invokeMethod(defaultGroupsService, "init");
        Assert.assertTrue(defaultGroupsService.isDefaultGroupName("group1"));
        Assert.assertTrue(defaultGroupsService.isNotDefaultGroupName("group3"));
    }
}
