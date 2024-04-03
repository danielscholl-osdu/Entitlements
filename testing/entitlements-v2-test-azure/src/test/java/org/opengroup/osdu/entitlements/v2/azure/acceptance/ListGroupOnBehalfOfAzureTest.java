package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import com.google.gson.Gson;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.ListGroupOnBehalfOfTest;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupType;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class ListGroupOnBehalfOfAzureTest extends ListGroupOnBehalfOfTest {

    public ListGroupOnBehalfOfAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }

    @Test
    public void shouldReturn200WhenMakingValidGetGroupsOnBehalfOfRequestWithRoleEnabled() throws Exception {
        Token token = tokenService.getToken();
        String memberEmail = this.configurationService.getMemberMailId();

        List<GroupItem> createdGroups = setup(memberEmail);
        Map<String, String> queryParams = new HashMap<String, String>() {{
            put("type", GroupType.NONE.toString());
            put("roleRequired", "true");
        }};
        RequestData requestData = RequestData.builder()
                .method("GET")
                .relativePath(String.format("members/%s/groups", memberEmail))
                .queryParams(queryParams)
                .dataPartitionId(configurationService.getTenantId())
                .token(token.getValue())
                .build();

        CloseableHttpResponse response = httpClientService.send(requestData);

        Assert.assertEquals(200, response.getCode());
        String getGroupsResponseBody = EntityUtils.toString(response.getEntity());
        ListGroupResponse groups = new Gson().fromJson(getGroupsResponseBody, ListGroupResponse.class);

        assertEquals(memberEmail.toLowerCase(), groups.getDesId());
        assertEquals(memberEmail.toLowerCase(), groups.getMemberEmail());

        List<String> foundGroups = groups.getGroups().stream()
                .filter(group -> group.getEmail().equals(createdGroups.get(0).getEmail())
                        || group.getEmail().equals(createdGroups.get(1).getEmail())
                        || group.getEmail().equals(createdGroups.get(2).getEmail()))
                .map(GroupItem::getEmail)
                .sorted(String::compareTo)
                .collect(Collectors.toList());

        assertEquals(3, foundGroups.size());
        assertEquals(createdGroups.get(0).getEmail(), foundGroups.get(0));
        assertEquals(createdGroups.get(1).getEmail(), foundGroups.get(1));
        assertEquals(createdGroups.get(2).getEmail(), foundGroups.get(2));

        //assert all items contain the role info
        Assert.assertEquals(groups.getGroups().stream().filter(item-> !item.getRole().isEmpty()).collect(Collectors.toList()).size(), groups.groups.size() );
    }
}