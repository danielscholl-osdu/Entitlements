package org.opengroup.osdu.entitlements.v2.di;

import com.google.gson.JsonParser;
import org.opengroup.osdu.entitlements.v2.util.FileReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

@Component
public class WhitelistSvcAccBeanConfiguration {

    @Autowired
    private FileReaderService fileReaderService;

    private Set<String> serviceAccounts;

    @PostConstruct
    public void init() {
        this.serviceAccounts = new HashSet<>();
        loadServiceAccounts("/provisioning/whitelistServiceAccount/quota_white_list.json");
    }

    private void loadServiceAccounts(final String filename) {
        final String fileContent = fileReaderService.readFile(filename);
        this.serviceAccounts.addAll(getAccountNames(fileContent));
    }

    private Set<String> getAccountNames(final String fileContent) {
        Set<String> accountNames = new HashSet<>();
        new JsonParser()
                .parse(fileContent)
                .getAsJsonObject()
                .get("partitionAssociationQuota")
                .getAsJsonArray()
                .forEach(element -> accountNames.add(element.getAsJsonObject().get("name").getAsString()));
        return accountNames;
    }

    public boolean isWhitelistedServiceAccount(final String email) {
        return serviceAccounts.contains(email);
    }
}