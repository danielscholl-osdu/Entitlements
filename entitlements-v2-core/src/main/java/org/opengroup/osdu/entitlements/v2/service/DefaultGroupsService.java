package org.opengroup.osdu.entitlements.v2.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.util.FileReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DefaultGroupsService {

    private final FileReaderService fileReaderService;
    private final AppProperties appProperties;

    private Set<String> defaultGroupNames;

    @PostConstruct
    private void init() {
        defaultGroupNames = loadInitialGroups();
    }

    public boolean isNotDefaultGroupName(final String groupName) {
        return !isDefaultGroupName(groupName);
    }

    public boolean isDefaultGroupName(final String groupName) {
        return defaultGroupNames.contains(groupName);
    }

    private Set<String> loadInitialGroups() {
        final Set<String> result = new HashSet<>();
        appProperties.getInitialGroups().forEach(fileName -> result.addAll(getGroupNamesFromFile(fileName)));
        return result;
    }

    private Set<String> getGroupNamesFromFile(String fileName) {
        Set<String> result = new HashSet<>();
        getGroupsFromJson(fileName).forEach(element -> result.add(getName(element)));
        return result;
    }

    private JsonArray getGroupsFromJson(final String fileName) {
        String fileContent = fileReaderService.readFile(fileName);
        return new JsonParser()
                .parse(fileContent)
                .getAsJsonObject()
                .get("groups")
                .getAsJsonArray();
    }

    private String getName(final JsonElement element) {
        final JsonObject jsonObject = element.getAsJsonObject();
        return jsonObject.get("name").getAsString();
    }
}
