package org.opengroup.osdu.entitlements.v2.aws.config;


import org.opengroup.osdu.entitlements.v2.aws.AwsAppProperties;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.helper.BasicMongoDBHelper;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.converter.EntityNodeToGroupDocConverter;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.converter.EntityNodeToUserDocConverter;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.converter.GroupDocToEntityNodeConverter;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.converter.UserDocToEntityNodeConverter;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper.GroupHelper;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper.IndexUpdater;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper.UserHelper;
import org.opengroup.osdu.entitlements.v2.aws.spi.AddMemberRepoMongoDB;
import org.opengroup.osdu.entitlements.v2.aws.spi.CreateGroupRepoMongoDB;
import org.opengroup.osdu.entitlements.v2.aws.spi.DeleteGroupRepoMongoDB;
import org.opengroup.osdu.entitlements.v2.aws.spi.ListMemberRepoMongoDB;
import org.opengroup.osdu.entitlements.v2.aws.spi.RemoveMemberRepoMongoDB;
import org.opengroup.osdu.entitlements.v2.aws.spi.RenameGroupRepoMongoDB;
import org.opengroup.osdu.entitlements.v2.aws.spi.RetrieveGroupMongoDB;
import org.opengroup.osdu.entitlements.v2.aws.spi.UpdateAppIdsRepoMongoDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EntitlementsContext {

    @Bean
    public AddMemberRepoMongoDB addMemberRepo() {
        return new AddMemberRepoMongoDB();
    }

    @Autowired
    @Bean
    public IndexUpdater retrieveIndexUpdater(BasicMongoDBHelper basicMongoDBHelper) {
        return new IndexUpdater(basicMongoDBHelper);
    }

    @Autowired
    @Bean
    public GroupHelper retrieveGroupHelper(BasicMongoDBHelper basicMongoDBHelper, IndexUpdater indexUpdater) {
        return new GroupHelper(basicMongoDBHelper, indexUpdater);
    }

    @Autowired
    @Bean
    public UserHelper retrieveUserHelper(BasicMongoDBHelper basicMongoDBHelper, IndexUpdater indexUpdater) {
        return new UserHelper(basicMongoDBHelper, indexUpdater);
    }

    @Bean
    public ListMemberRepoMongoDB listMemberRepoMongoDB() {
        return new ListMemberRepoMongoDB();
    }

    @Bean
    public CreateGroupRepoMongoDB createGroupRepoMongoDB() {
        return new CreateGroupRepoMongoDB();
    }

    @Bean
    public DeleteGroupRepoMongoDB deleteGroupRepoMongoDB() {
        return new DeleteGroupRepoMongoDB();
    }

    @Bean
    public RemoveMemberRepoMongoDB removeMemberRepoMongoDB() {
        return new RemoveMemberRepoMongoDB();
    }

    @Bean
    public RenameGroupRepoMongoDB renameGroupRepoMongoDB() {
        return new RenameGroupRepoMongoDB();
    }

    @Bean
    public UpdateAppIdsRepoMongoDB UpdateAppIdsRepoMongoDB() {
        return new UpdateAppIdsRepoMongoDB();
    }

    @Bean
    public RetrieveGroupMongoDB retrieveGroupMongoDB() {
        return new RetrieveGroupMongoDB();
    }

    @Bean
    public AwsAppProperties awsAppProperties() {
        return new AwsAppProperties();
    }

    //    Converters
    @Bean
    public EntityNodeToUserDocConverter entityNodeToUserDocConverter() {
        return new EntityNodeToUserDocConverter();
    }

    @Bean
    public EntityNodeToGroupDocConverter entityNodeToGroupDocConverter() {
        return new EntityNodeToGroupDocConverter();
    }

    @Bean
    public GroupDocToEntityNodeConverter groupDocToEntityNodeConverter() {
        return new GroupDocToEntityNodeConverter();
    }

    @Bean
    public UserDocToEntityNodeConverter userDocToEntityNodeConverter() {
        return new UserDocToEntityNodeConverter();
    }
}

