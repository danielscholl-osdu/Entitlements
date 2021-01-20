package org.opengroup.osdu.entitlements.v2.model.deletegroup;


import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteGroupDtoTests {

    @Test
    public void should_convertAllParameters_andAddId_andName(){
        DeleteGroupDto sut = new DeleteGroupDto("name@dp.domain.com");

        EntityNode result = DeleteGroupDto.deleteGroupNode(sut, "dp");

        assertThat(result.getNodeId()).isEqualTo("name@dp.domain.com");
        assertThat(result.getName()).isEqualTo("name");
    }
}
