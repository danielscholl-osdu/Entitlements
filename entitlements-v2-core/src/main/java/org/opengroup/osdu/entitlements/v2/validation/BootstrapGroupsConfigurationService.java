package org.opengroup.osdu.entitlements.v2.validation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.util.FileReaderService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BootstrapGroupsConfigurationService {

    private final FileReaderService fileReaderService;
    private final AppProperties appProperties;
    private Map<String, Set<String>> membersPerGroup;

    @PostConstruct
    private void init() {
        membersPerGroup = new HashMap<>();
        for (String filePath: appProperties.getProtectedMembers()) {
            loadConfiguration(filePath);
        }
    }

    public boolean isMemberProtectedFromRemoval(EntityNode memberNode, EntityNode groupNode) {
        if (!memberNode.getDataPartitionId().equals(groupNode.getDataPartitionId())) {
            return false;
        }
        return membersPerGroup.getOrDefault(groupNode.getName(), new HashSet<>()).contains(memberNode.getName());
    }

    private void loadConfiguration(final String fileName) {
        String fileContent = fileReaderService.readFile(fileName);
        JsonArray groups = getGroupList(fileContent);
        for (JsonElement group: groups) {
            String groupName = group.getAsJsonObject().get("name").getAsString();
            Set<String> setOfMembers = new HashSet<>();
            JsonArray arrayOfMembers = group.getAsJsonObject().get("members").getAsJsonArray();
            for (JsonElement member: arrayOfMembers) {
                setOfMembers.add(member.getAsJsonObject().get("name").getAsString());
            }
            membersPerGroup.put(groupName, setOfMembers);
        }
    }

    private JsonArray getGroupList(final String fileContent) {
        return JsonParser.parseString(fileContent)
                .getAsJsonObject()
                .get("groups")
                .getAsJsonArray();
    }
}
