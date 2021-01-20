package org.opengroup.osdu.entitlements.v2.model.creategroup;

import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateGroupDtoTests {
    @Test
    public void should_convertAllParameters_andAddId_andSetNameTolowercase(){
        CreateGroupDto sut = new CreateGroupDto("NaMe", "DEsC");

        EntityNode result = CreateGroupDto.createGroupNode(sut, "dp.Domain.COM", "dp");

        assertThat(result.getNodeId()).isEqualTo("name@dp.domain.com");
        assertThat(result.getName()).isEqualTo("name");
        assertThat(result.getDescription()).isEqualTo("DEsC");
    }
}
