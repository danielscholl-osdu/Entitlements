/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.DeleteGroupTest;
import org.opengroup.osdu.entitlements.v2.util.IBMConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.IBMTokenService;

public class DeleteGroupIBMTest extends DeleteGroupTest {

    public DeleteGroupIBMTest() {
        super(new IBMConfigurationService(), new IBMTokenService());
    }
}
