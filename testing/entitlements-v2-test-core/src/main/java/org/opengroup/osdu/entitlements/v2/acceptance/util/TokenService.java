package org.opengroup.osdu.entitlements.v2.acceptance.util;

import org.apache.commons.lang.NotImplementedException;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;

public interface TokenService {

    Token getToken();

    default Token getNoAccToken(){
        throw new NotImplementedException();
    }
}
