/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.DeleteMemberTest;
import org.opengroup.osdu.entitlements.v2.util.IBMConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.IBMTokenService;

public class DeleteMemberIBMTest extends DeleteMemberTest {

    public DeleteMemberIBMTest() {
        super(new IBMConfigurationService(), new IBMTokenService());
    }
}
