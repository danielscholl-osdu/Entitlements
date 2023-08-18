package org.opengroup.osdu.entitlements.v2.acceptance;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class DeleteMemberTest extends AcceptanceBaseTest {
    private final List<String> groupsForFurtherDeletion;
    private final Token token;

    public DeleteMemberTest(ConfigurationService configurationService, TokenService tokenService) {
        super(configurationService, tokenService);
        groupsForFurtherDeletion = new ArrayList<>();
        token = tokenService.getToken();
    }

    @Override
    protected void cleanup() throws Exception {
        for (String groupName : groupsForFurtherDeletion) {
            entitlementsV2Service.deleteGroup(groupName, token.getValue());
        }
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        return RequestData.builder()
                .method("DELETE")
                .relativePath("members/member@test.com")
                .dataPartitionId(configurationService.getTenantId())
                .build();
    }

    /**
     * 1) Create groups 1, 2 and 3.
     * 2) Add the group 3 to the groups 1 and 2 as a member.
     * 3) Delete the member (group 3).
     * 4) Check that the group 3 is not a member of the groups 1 and 2.
     */
    @Test
    public void shouldSuccessfullyDeleteMember() throws Exception {
        List<GroupItem> groups = setup();

        assertTrue(isGroupAMemberOfAnotherGroup(groups.get(2).getEmail(), groups.get(0).getEmail()));
        assertTrue(isGroupAMemberOfAnotherGroup(groups.get(2).getEmail(), groups.get(1).getEmail()));

        entitlementsV2Service.deleteMember(groups.get(2).getEmail(), token.getValue());

        assertFalse(isGroupAMemberOfAnotherGroup(groups.get(2).getEmail(), groups.get(0).getEmail()));
        assertFalse(isGroupAMemberOfAnotherGroup(groups.get(2).getEmail(), groups.get(1).getEmail()));
    }

    @Test
    public void shouldBeAbleToInvokeApiInParallel() throws Exception {
        List<GroupItem> groups = setup();

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<CloseableHttpResponse>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            Callable<CloseableHttpResponse> task = () -> {
                try {
                    return entitlementsV2Service.deleteMember(groups.get(2).getEmail(), token.getValue());
                } catch (Exception e) {
                    return null;
                }
            };
            tasks.add(task);
        }

        List<Future<CloseableHttpResponse>> responses = executor.invokeAll(tasks);
        executor.shutdown();
        //noinspection ResultOfMethodCallIgnored
        executor.awaitTermination(30, TimeUnit.SECONDS);
        int successResponseCount = 0;
        for (Future<CloseableHttpResponse> future : responses) {
            CloseableHttpResponse closeableHttpResponse = future.get();
            if (closeableHttpResponse == null) {
                fail("Failed to get response client response");
            } else if (204 == closeableHttpResponse.getCode() || 404 == closeableHttpResponse.getCode()) {
                successResponseCount++;
            } else {
                fail(String.format("Inappropriate status code %s from client response", closeableHttpResponse.getCode()));
            }
        }

        assertEquals("Expected 10 successful response", threads, successResponseCount);
    }

    private List<GroupItem> setup() throws Exception {
        List<GroupItem> groups = new ArrayList<>();

        String group1Name = "group1-" + currentTime;
        String group2Name = "group2-" + currentTime;
        String group3Name = "group3-" + currentTime;

        GroupItem group1Item = entitlementsV2Service.createGroup(group1Name, token.getValue());
        groupsForFurtherDeletion.add(group1Item.getEmail());
        groups.add(group1Item);

        GroupItem group2Item = entitlementsV2Service.createGroup(group2Name, token.getValue());
        groupsForFurtherDeletion.add(group2Item.getEmail());
        groups.add(group2Item);

        GroupItem group3Item = entitlementsV2Service.createGroup(group3Name, token.getValue());
        groupsForFurtherDeletion.add(group3Item.getEmail());
        groups.add(group3Item);

        addMember(group1Item.getEmail(), group3Item.getEmail());
        addMember(group2Item.getEmail(), group3Item.getEmail());

        return groups;
    }

    private void addMember(String groupEMail, String memberEmail) throws Exception {
        AddMemberRequestData addMemberRequestData = AddMemberRequestData.builder()
                .groupEmail(groupEMail)
                .role("MEMBER")
                .memberEmail(memberEmail)
                .build();
        entitlementsV2Service.addMember(addMemberRequestData, token.getValue());
    }

    private boolean isGroupAMemberOfAnotherGroup(String groupEmail, String anotherGroupEmail) throws Exception {
        return entitlementsV2Service.getMembers(anotherGroupEmail, token.getValue()).getMembers().stream()
                .anyMatch(memberItem -> memberItem.getEmail().equals(groupEmail));
    }
}
