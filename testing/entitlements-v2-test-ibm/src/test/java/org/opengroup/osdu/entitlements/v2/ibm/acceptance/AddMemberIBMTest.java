/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.AddMemberTest;
import org.opengroup.osdu.entitlements.v2.util.IBMConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.IBMTokenService;

public class AddMemberIBMTest extends AddMemberTest {

    public AddMemberIBMTest() {
        super(new IBMConfigurationService(), new IBMTokenService());
    }
}
