package com.local.tiktokregion;

public final class ModuleStatus {
    private ModuleStatus() {
    }

    // Replaced only when LSPosed loads this module into its own app process.
    public static boolean isActive() {
        return false;
    }
}
