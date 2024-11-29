/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.model.GroupType;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.model.response.ListGroupInPartitionResponse;
import org.opengroup.osdu.entitlements.v2.util.CommonConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.TokenTestUtils;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class ListAllPartitionGroupsTest extends AcceptanceBaseTest {

    private ListAllPartitionGroupsTest() {
        super(new CommonConfigurationService());
    }
    
    @BeforeEach
    @Override
    public void setupTest() throws Exception {
        this.testUtils = new TokenTestUtils();
    }

    @AfterEach
    @Override
    public void tearTestDown() throws Exception {
        this.testUtils = null;
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        return RequestData.builder()
            .method("GET")
            .dataPartitionId(configurationService.getTenantId())
            .relativePath("groups/all")
            .build();
    }


    @Test
    public void shouldReturnOnlyServiceGroupsByGroupType() throws Exception {
        sendGetGroupsWithTypeParam(GroupType.SERVICE);
    }

    @Test
    public void shouldReturnOnlyDataGroupsByGroupType() throws Exception {
        sendGetGroupsWithTypeParam(GroupType.DATA);
    }

    @Test
    public void shouldReturnOnlyUserGroupsByGroupType() throws Exception {
        sendGetGroupsWithTypeParam(GroupType.USER);
    }

    @Test
    public void shouldReturnGroupsWithinLimitIfLimitParamPresent() throws Exception {
        String limitParam = "limit=10";
        String typeParam = "type=service";
        ListGroupInPartitionResponse serviceGroups = entitlementsV2Service.getGroupsWithinPartition(testUtils.getToken(), limitParam, typeParam);
        assertThat(serviceGroups.getGroups().size(), is(10));
    }

    @Test
    public void shouldReturnBadRequestIfTypeParamNotPresent() throws Exception {
        sendBadRequest( "limit=10");
    }

    @Test
    public void shouldReturnBadRequestWhenMalformedLimit() throws Exception {
        sendBadRequest("limit=-1", "type=service");
    }

    @Test
    public void shouldReturnBadRequestWhenMalformedGroupType() throws Exception {
        sendBadRequest("type=NONEXISTENT");
    }

    private void sendBadRequest(String...params) throws Exception {
        String joinedParams = "";
        if(params.length > 0){
            StringJoiner paramJoiner = new StringJoiner("&", "?", "");
            for (String param: params){
                paramJoiner.add(param);
            }
            joinedParams = paramJoiner.toString();
        }
        RequestData requestData = RequestData.builder()
            .method("GET").dataPartitionId(configurationService.getTenantId())
            .relativePath("groups/all" + joinedParams)
            .token(testUtils.getToken())
            .build();

        CloseableHttpResponse response = httpClientService.send(requestData);
        assertEquals(400, response.getCode());
    }

    private void sendGetGroupsWithTypeParam(GroupType groupType) throws Exception {
        String groupTypeParam = "type=" + groupType.toString().toLowerCase();
        ListGroupInPartitionResponse serviceGroups = entitlementsV2Service.getGroupsWithinPartition(testUtils.getToken(), groupTypeParam);
        List<String> groupEmails = serviceGroups.getGroups().stream()
            .map(GroupItem::getEmail)
            .collect(Collectors.toList());
        assertThat(groupEmails, everyItem(startsWith(groupType.toString().toLowerCase())));
    }

}
