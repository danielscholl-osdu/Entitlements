package org.opengroup.osdu.entitlements.v2.di;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.util.FileReaderService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class KeySvcAccBeanConfiguration {

    private final FileReaderService fileReaderService;
    private final RequestInfo requestInfo;
    private final AppProperties appProperties;

    private Map<String, Set<String>> svcAccGroupConfig;

    @PostConstruct
    private void init() {
        this.svcAccGroupConfig = new HashMap<>();
        loadConfiguration(appProperties.getGroupsOfServicePrincipal());
    }

    public boolean isKeySvcAccountInBootstrapGroup(final String groupEmail, final String memberEmail) {
        return isKeyServiceAccount(memberEmail) && getServiceAccountGroups(memberEmail).contains(groupEmail);
    }

    public Set<String> getServiceAccountGroups(final String email) {
        if (isServicePrincipalAccount(email)) {
            return this.svcAccGroupConfig.computeIfAbsent("SERVICE_PRINCIPAL", k -> new HashSet<>());
        }
        return new HashSet<>();
    }

    public boolean isKeyServiceAccount(final String email) {
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
