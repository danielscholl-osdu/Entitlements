package org.opengroup.osdu.entitlements.v2.aws.spi;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengroup.osdu.entitlements.v2.aws.Util.ParentUtil;
import org.opengroup.osdu.entitlements.v2.aws.config.EntitlementsTestConfig;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataMongoTest
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@SpringJUnitConfig(classes = {EntitlementsTestConfig.class})
public class ListMemberRepoMongoDBTest extends ParentUtil {

    @Autowired
    private ListMemberRepoMongoDB listMemberRepoMongoDB;


    @BeforeEach
    public void generateDataset() {
        mongoTemplateHelper.dropCollections();
        initDefaultDataSet();
    }

    @Test
    public void runTest() {
        //given
        ListMemberServiceDto request = ListMemberServiceDto.builder()
                .partitionId(DATA_PARTITION)
                .groupId(String.format(GROUP_TEMPLATE, "4"))
                .requesterId(null)
                .build();

        //when
        List<ChildrenReference> result = listMemberRepoMongoDB.run(request);

        //then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.stream().map(ChildrenReference::getId).distinct().count());
    }

    @Disabled // todo enabled
    @Test
    public void runNotFoundGroup() {
        //given
        ListMemberServiceDto request = ListMemberServiceDto.builder()
                .partitionId(DATA_PARTITION)
                .groupId(String.format(GROUP_TEMPLATE, 666544))
                .requesterId(null)
                .build();

        //whe
        Assertions.assertThrows(IllegalArgumentException.class, () -> listMemberRepoMongoDB.run(request));
    }

}
