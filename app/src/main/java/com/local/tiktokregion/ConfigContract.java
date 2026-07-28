package com.local.tiktokregion;

public final class ConfigContract {
    public static final String MODULE_PACKAGE = "com.local.tiktokregion";
    public static final String TARGET_MUSICAL_LY_PACKAGE = "com.zhiliaoapp.musically";
    public static final String TARGET_TRILL_PACKAGE = "com.ss.android.ugc.trill";
    public static final String[] TARGET_PACKAGES = {
            TARGET_MUSICAL_LY_PACKAGE,
            TARGET_TRILL_PACKAGE
    };
    public static final String PROVIDER_AUTHORITY = "com.local.tiktokregion.profiles";
    public static final String PROFILE_URI = "content://" + PROVIDER_AUTHORITY + "/active";
    public static final String ACTION_PROFILE_CHANGED =
            "com.local.tiktokregion.action.PROFILE_CHANGED";

    public static final String COLUMN_ENABLED = "enabled";
    public static final String COLUMN_PROFILE_ID = "profile_id";
    public static final String COLUMN_REGION = "region";
    public static final String COLUMN_COUNTRY_ISO = "country_iso";
    public static final String COLUMN_MCC_MNC = "mcc_mnc";
    public static final String COLUMN_MCC = "mcc";
    public static final String COLUMN_MNC = "mnc";
    public static final String COLUMN_CARRIER = "carrier";
    public static final String COLUMN_SKIP_STARTUP_LOGIN = "skip_startup_login";

    public static final String PREFERENCES = "region_config";
    public static final String TARGET_PREFERENCES = "tiktok_region_hook_config";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_PROFILE_ID = "profile_id";
    public static final String KEY_SKIP_STARTUP_LOGIN = "skip_startup_login";

    private ConfigContract() {
    }

    public static boolean isTargetPackage(String packageName) {
        for (String targetPackage : TARGET_PACKAGES) {
            if (targetPackage.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
