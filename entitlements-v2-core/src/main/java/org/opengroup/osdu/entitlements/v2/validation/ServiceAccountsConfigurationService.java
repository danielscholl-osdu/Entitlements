package org.opengroup.osdu.entitlements.v2.validation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.util.FileReaderService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ServiceAccountsConfigurationService {

    private final FileReaderService fileReaderService;
    private final RequestInfo requestInfo;
    private final AppProperties appProperties;

    private Map<String, Set<String>> svcAccGroupConfig;

    @PostConstruct
    private void init() {
        this.svcAccGroupConfig = new HashMap<>();
        loadConfiguration(appProperties.getGroupsOfServicePrincipal());
    }

    /**
     * Checks if a member is a service account and if it belongs to its default group
     */
    public boolean isMemberProtectedServiceAccount(final EntityNode memberNode, final EntityNode groupNode) {
        if (!memberNode.getDataPartitionId().equals(groupNode.getDataPartitionId())) {
            return false;
        }
        return isKeyServiceAccount(memberNode.getNodeId())
                && getServiceAccountGroups(memberNode.getNodeId()).contains(groupNode.getName());
    }

    /**
     * Returns a set of default groups of service account
     */
    public Set<String> getServiceAccountGroups(final String email) {
        if (isServicePrincipalAccount(email)) {
            return this.svcAccGroupConfig.computeIfAbsent("SERVICE_PRINCIPAL", k -> new HashSet<>());
        }
        return new HashSet<>();
    }

    private boolean isKeyServiceAccount(final String email) {
        return isServicePrincipalAccount(email);
    }

    private boolean isServicePrincipalAccount(final String email) {
        return email.equalsIgnoreCase(requestInfo.getTenantInfo().getServiceAccount());
    }

    private void loadConfiguration(final String fileName) {
        String fileContent = fileReaderService.readFile(fileName);
        JsonObject userElement = getUserJsonObject(fileContent);
        String emailKey = userElement.get("email").getAsString();
        Set<String> groupNames = getGroupNamesForOwner(fileContent);
        this.svcAccGroupConfig.put(emailKey, groupNames);
    }

    private JsonObject getUserJsonObject(final String fileContent) {
        return JsonParser.parseString(fileContent)
                .getAsJsonObject()
                .get("users")
                .getAsJsonArray()
                .iterator()
                .next()
                .getAsJsonObject();
    }

    private Set<String> getGroupNamesForOwner(final String fileContent) {
        Set<String> groupNames = new HashSet<>();
        JsonArray array = JsonParser.parseString(fileContent)
                .getAsJsonObject()
                .get("ownersOf")
                .getAsJsonArray();
        array.forEach(element -> groupNames.add(element.getAsJsonObject().get("groupName").getAsString()));
        return groupNames;
    }
}
