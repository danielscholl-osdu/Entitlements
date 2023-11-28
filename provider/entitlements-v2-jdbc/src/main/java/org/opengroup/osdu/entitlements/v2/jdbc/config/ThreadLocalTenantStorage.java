package org.opengroup.osdu.entitlements.v2.jdbc.config;

public class ThreadLocalTenantStorage {

  private static final ThreadLocal<String> tenant = new ThreadLocal<>();

  private ThreadLocalTenantStorage() {
  }

  public static void setTenantName(String tenantName) {
    tenant.set(tenantName);
  }

  public static String getTenantName() {
    return tenant.get();
  }

  public static void clear() {
    tenant.remove();
  }

}
