package org.opengroup.osdu.entitlements.v2.aws.spi;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper.GroupHelper;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper.UserHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;

abstract public class BasicEntitlementsHelper {

    public static final String ID = "_id";
    public static final String NODE_ID = "_id.nodeId";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String PARENTS = "parents";
    public static final String CHILDREN = "children";
    public static final String APP_IDS = "appIds";
    public static final String DIRECT_PARENTS = "directParents";
    public static final String MEMBER_OF = "memberOf";

    @Autowired
    protected GroupHelper groupHelper;
    @Autowired
    protected UserHelper userHelper;
    @Autowired
    protected ConversionService conversionService;

}
