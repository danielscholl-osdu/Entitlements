package org.opengroup.osdu.entitlements.v2.spi;

public interface Operation {
    void execute();
    void undo();
}
