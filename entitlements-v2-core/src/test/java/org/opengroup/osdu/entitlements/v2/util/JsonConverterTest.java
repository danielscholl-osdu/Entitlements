package org.opengroup.osdu.entitlements.v2.util;

import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.junit.Assert;
import org.junit.Test;

public class JsonConverterTest {

    @Test
    public void shouldConvertToAndFromNode() {
        EntityNode node = EntityNode.createNodeFromGroupEmail("a@b.com");
        String json = JsonConverter.toJson(node);
        EntityNode node2 = JsonConverter.fromJson(json, EntityNode.class);
        Assert.assertEquals(node, node2);
    }
}
