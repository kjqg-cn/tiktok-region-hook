package com.local.tiktokregion;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class RegionHook implements IXposedHookLoadPackage {
    private static final String LOG_PREFIX = "[TikTokRegionHook] ";
    private static final String LOG_TAG = "TikTokRegionHook";
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static volatile long processAttachedAt;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!ConfigContract.isTargetPackage(loadPackageParam.packageName)) {
            return;
        }

        XposedHelpers.findAndHookMethod(
                Application.class,
                "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!INITIALIZED.compareAndSet(false, true)) {
                            return;
                        }

                        Context context = (Context) param.args[0];
                        processAttachedAt = SystemClock.elapsedRealtime();
                        registerConfigReceiver(context);
                        HookConfig config = loadConfig(context);
                        if (config == null || !config.enabled) {
                            log("disabled for " + loadPackageParam.processName);
                            return;
                        }

                        RegionProfile profile = config.profile;
                        int frameworkHooks = installFrameworkHooks(profile);
                        int appHooks = installTikTokHooks(
                                loadPackageParam.classLoader,
                                profile,
                                config.skipStartupLogin);
                        log("active for " + loadPackageParam.processName
                                + ", profile=" + profile.id
                                + ", frameworkHooks=" + frameworkHooks
                                + ", appHooks=" + appHooks);
                    }
                });
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private static void registerConfigReceiver(Context context) {
        IntentFilter filter = new IntentFilter(ConfigContract.ACTION_PROFILE_CHANGED);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                if (ConfigContract.ACTION_PROFILE_CHANGED.equals(intent.getAction())) {
                    if (intent.hasExtra(ConfigContract.KEY_PROFILE_ID)) {
                        HookConfig config = new HookConfig(
                                intent.getBooleanExtra(ConfigContract.KEY_ENABLED, true),
                                RegionProfile.find(intent.getStringExtra(
                                        ConfigContract.KEY_PROFILE_ID)),
                                intent.getBooleanExtra(
                                        ConfigContract.KEY_SKIP_STARTUP_LOGIN,
                                        true));
                        saveTargetConfig(receiverContext, config);
                    }
                    Process.killProcess(Process.myPid());
                }
            }
        };
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
    }

    private static HookConfig loadConfig(Context context) {
        Throwable lastFailure = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HookConfig config = loadConfigFromProvider(context);
                saveTargetConfig(context, config);
                return config;
            } catch (Throwable throwable) {
                lastFailure = throwable;
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        try {
            XSharedPreferences preferences = new XSharedPreferences(
                    ConfigContract.MODULE_PACKAGE,
                    ConfigContract.PREFERENCES);
            preferences.reload();
            if (!preferences.contains(ConfigContract.KEY_PROFILE_ID)) {
                throw new IllegalStateException("Shared profile is unavailable");
            }
            HookConfig config = new HookConfig(
                    preferences.getBoolean(ConfigContract.KEY_ENABLED, true),
                    RegionProfile.find(preferences.getString(
                            ConfigContract.KEY_PROFILE_ID,
                            "US")),
                    preferences.getBoolean(
                            ConfigContract.KEY_SKIP_STARTUP_LOGIN,
                            true));
            saveTargetConfig(context, config);
            return config;
        } catch (Throwable throwable) {
            HookConfig cached = loadTargetConfig(context);
            if (cached != null) {
                log("using TikTok-local profile cache: " + cached.profile.id);
                return cached;
            }
            if (lastFailure != null) {
                logFailure("profile provider", lastFailure);
            }
            logFailure("shared profile", throwable);
            return null;
        }
    }

    private static HookConfig loadConfigFromProvider(Context context) {
        try (Cursor cursor = context.getContentResolver().query(
                Uri.parse(ConfigContract.PROFILE_URI),
                null,
                null,
                null,
                null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                throw new IllegalStateException("Empty profile response");
            }
            RegionProfile profile = new RegionProfile(
                    cursor.getString(cursor.getColumnIndexOrThrow(
                            ConfigContract.COLUMN_PROFILE_ID)),
                    "",
                    cursor.getString(cursor.getColumnIndexOrThrow(
                            ConfigContract.COLUMN_REGION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(
                            ConfigContract.COLUMN_COUNTRY_ISO)),
                    cursor.getString(cursor.getColumnIndexOrThrow(
                            ConfigContract.COLUMN_MCC_MNC)),
                    cursor.getString(cursor.getColumnIndexOrThrow(
                            ConfigContract.COLUMN_MCC)),
                    cursor.getString(cursor.getColumnIndexOrThrow(
                            ConfigContract.COLUMN_MNC)),
                    cursor.getString(cursor.getColumnIndexOrThrow(
                            ConfigContract.COLUMN_CARRIER)));
            return new HookConfig(
                    cursor.getInt(cursor.getColumnIndexOrThrow(
                            ConfigContract.COLUMN_ENABLED)) != 0,
                    profile,
                    cursor.getInt(cursor.getColumnIndexOrThrow(
                            ConfigContract.COLUMN_SKIP_STARTUP_LOGIN)) != 0);
        }
    }

    private static void saveTargetConfig(Context context, HookConfig config) {
        boolean saved = context.getSharedPreferences(
                        ConfigContract.TARGET_PREFERENCES,
                        Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ConfigContract.KEY_ENABLED, config.enabled)
                .putString(ConfigContract.KEY_PROFILE_ID, config.profile.id)
                .putBoolean(
                        ConfigContract.KEY_SKIP_STARTUP_LOGIN,
                        config.skipStartupLogin)
                .commit();
        if (!saved) {
            log("unable to save TikTok-local profile cache");
        }
    }

    private static HookConfig loadTargetConfig(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                ConfigContract.TARGET_PREFERENCES,
                Context.MODE_PRIVATE);
        if (!preferences.contains(ConfigContract.KEY_PROFILE_ID)) {
            return null;
        }
        return new HookConfig(
                preferences.getBoolean(ConfigContract.KEY_ENABLED, true),
                RegionProfile.find(preferences.getString(
                        ConfigContract.KEY_PROFILE_ID,
                        "US")),
                preferences.getBoolean(
                        ConfigContract.KEY_SKIP_STARTUP_LOGIN,
                        true));
    }

    private static final class HookConfig {
        final boolean enabled;
        final RegionProfile profile;
        final boolean skipStartupLogin;

        HookConfig(boolean enabled, RegionProfile profile, boolean skipStartupLogin) {
            this.enabled = enabled;
            this.profile = profile;
            this.skipStartupLogin = skipStartupLogin;
        }
    }

    private static int installFrameworkHooks(RegionProfile profile) {
        int count = 0;
        count += hookAll(TelephonyManager.class, "getNetworkCountryIso", profile.countryIso);
        count += hookAll(TelephonyManager.class, "getSimCountryIso", profile.countryIso);
        count += hookAll(TelephonyManager.class, "getNetworkOperator", profile.mccMnc);
        count += hookAll(TelephonyManager.class, "getSimOperator", profile.mccMnc);
        count += hookAll(TelephonyManager.class, "getNetworkOperatorName", profile.carrier);
        count += hookAll(TelephonyManager.class, "getSimOperatorName", profile.carrier);

        count += hookAll(SubscriptionInfo.class, "getCountryIso", profile.countryIso);
        count += hookAll(SubscriptionInfo.class, "getMcc", Integer.parseInt(profile.mcc));
        count += hookAll(SubscriptionInfo.class, "getMnc", Integer.parseInt(profile.mnc));
        count += hookAll(SubscriptionInfo.class, "getMccString", profile.mcc);
        count += hookAll(SubscriptionInfo.class, "getMncString", profile.mnc);
        count += hookAll(SubscriptionInfo.class, "getCarrierName", profile.carrier);

        count += hookAll(ServiceState.class, "getOperatorNumeric", profile.mccMnc);
        count += hookAll(ServiceState.class, "getOperatorAlphaLong", profile.carrier);
        count += hookAll(ServiceState.class, "getOperatorAlphaShort", profile.carrier);
        return count;
    }

    private static int installTikTokHooks(
            ClassLoader classLoader,
            RegionProfile profile,
            boolean skipStartupLogin) {
        int count = 0;

        count += hookAll(classLoader, "X.0VV8", "LIZJ", profile.countryIso);
        count += hookAll(classLoader, "X.0VV8", "LJ", profile.mccMnc);
        count += hookAll(classLoader, "X.0VV8", "LJI", profile.carrier);
        count += hookAll(classLoader, "X.0VV8", "LJIIIIZZ", profile.countryIso);
        count += hookAll(classLoader, "X.0VV8", "LJIIJ", profile.mccMnc);
        count += hookAll(classLoader, "X.0VV8", "LJIIL", profile.carrier);

        count += hookAll(classLoader, "X.11ga", "LIZ", profile.region);
        count += hookAll(classLoader, "X.11ga", "LIZIZ", profile.region);
        count += hookAll(classLoader, "X.11ga", "LIZJ", profile.region);
        count += hookAll(classLoader, "X.11ga", "LIZLLL", profile.region);
        count += hookAll(classLoader, "X.11ga", "LJ", profile.region);
        count += hookAll(classLoader, "X.11ga", "LJFF", profile.region);

        count += hookSystemLocaleRegion(classLoader, profile);
        count += hookCurrentSimInfo(classLoader, profile);
        count += hookStoreRegion(classLoader, profile);
        if (skipStartupLogin) {
            count += hookStartupLogin(classLoader);
        }
        count += hookProfileRequestDiagnostics(classLoader);
        return count;
    }

    private static int hookStartupLogin(ClassLoader classLoader) {
        try {
            Class<?> loginActivityClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.account.login.auth."
                            + "I18nSignUpActivityWithNoAnimation",
                    classLoader);
            Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(
                    loginActivityClass,
                    "onCreate",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            long processAge = SystemClock.elapsedRealtime() - processAttachedAt;
                            if (processAge < 0L || processAge > 20_000L
                                    || !(param.thisObject instanceof Activity)) {
                                return;
                            }
                            Activity activity = (Activity) param.thisObject;
                            activity.finish();
                            activity.overridePendingTransition(0, 0);
                            log("closed startup login activity");
                        }
                    });
            return hooks.size();
        } catch (Throwable throwable) {
            logFailure("startup login activity", throwable);
            return 0;
        }
    }

    private static int hookStoreRegion(
            ClassLoader classLoader,
            RegionProfile profile) {
        int count = 0;
        try {
            Class<?> storeRegionInfoClass = XposedHelpers.findClass("X.0W7B", classLoader);
            Object storeRegionInfo = XposedHelpers.newInstance(
                    storeRegionInfoClass,
                    profile.region,
                    "local");
            count += hookAll(
                    classLoader,
                    "com.bytedance.i18n.region.StoreRegionSource",
                    "LIZ",
                    storeRegionInfo);
            count += hookAll(classLoader, "X.0W6U", "LIZ", storeRegionInfo);
        } catch (Throwable throwable) {
            logFailure("store region source", throwable);
        }

        count += hookAll(classLoader, "X.0Wdy", "LIZIZ", profile.region);
        count += hookAll(classLoader, "X.0We0", "LIZ", profile.region);

        try {
            Class<?> pipelineClass = XposedHelpers.findClass("X.0XBC", classLoader);
            Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(
                    pipelineClass,
                    "LIZ",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.args.length == 1
                                    && "store_region".equals(param.args[0])) {
                                param.setResult(profile.countryIso);
                            }
                        }
                    });
            count += hooks.size();
        } catch (Throwable throwable) {
            logFailure("X.0XBC.LIZ(store_region)", throwable);
        }
        return count;
    }

    private static int hookProfileRequestDiagnostics(ClassLoader classLoader) {
        try {
            Class<?> interceptorClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.net.interceptor.UrlTransformInterceptorTTNet",
                    classLoader);
            Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(
                    interceptorClass,
                    "intercept",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args.length != 1 || param.args[0] == null) {
                                return;
                            }
                            try {
                                Object request = XposedHelpers.getObjectField(
                                        param.args[0],
                                        "LIZJ");
                                String url = (String) XposedHelpers.callMethod(request, "getUrl");
                                Uri uri = Uri.parse(url);
                                String path = uri.getPath();
                                if (path == null || (!path.contains("/aweme/v1/user/")
                                        && !path.contains("/user/profile/"))) {
                                    return;
                                }
                                param.setObjectExtra("region_hook_request", describeRegionRequest(uri));
                            } catch (Throwable throwable) {
                                Log.w(LOG_TAG, "Unable to inspect profile request", throwable);
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object description = param.getObjectExtra("region_hook_request");
                            if (description == null) {
                                return;
                            }
                            try {
                                int httpCode = -1;
                                Object response = param.getResult();
                                if (response != null) {
                                    Object rawResponse = XposedHelpers.getObjectField(response, "LIZ");
                                    httpCode = XposedHelpers.getIntField(rawResponse, "LIZIZ");
                                }
                                Log.i(LOG_TAG, description + " http=" + httpCode);
                            } catch (Throwable throwable) {
                                Log.w(LOG_TAG, description + " response inspection failed", throwable);
                            }
                        }
                    });
            return hooks.size();
        } catch (Throwable throwable) {
            logFailure("profile request diagnostics", throwable);
            return 0;
        }
    }

    private static String describeRegionRequest(Uri uri) {
        String[] regionKeys = {
                "region",
                "carrier_region",
                "sys_region",
                "app_region",
                "mcc_mnc",
                "network_sim_region",
                "current_region",
                "store_region",
                "op_region",
                "sim_region"
        };
        StringBuilder description = new StringBuilder("profileRequest path=")
                .append(uri.getPath());
        for (String key : regionKeys) {
            String value = uri.getQueryParameter(key);
            if (value != null) {
                description.append(' ').append(key).append('=').append(value);
            }
        }
        return description.toString();
    }

    private static int hookSystemLocaleRegion(
            ClassLoader classLoader,
            RegionProfile profile) {
        try {
            Class<?> regionClass = XposedHelpers.findClass("X.0VV4", classLoader);
            Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(
                    regionClass,
                    "LIZIZ",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.args.length == 1
                                    && param.args[0] instanceof Locale
                                    && param.args[0].equals(Locale.getDefault())) {
                                param.setResult(profile.region);
                            }
                        }
                    });
            return hooks.size();
        } catch (Throwable throwable) {
            logFailure("X.0VV4.LIZIZ", throwable);
            return 0;
        }
    }

    private static int hookCurrentSimInfo(
            ClassLoader classLoader,
            RegionProfile profile) {
        try {
            final Class<?> simInfoClass = XposedHelpers.findClass("X.0V1y", classLoader);
            Class<?> simReaderClass = XposedHelpers.findClass("X.0V1x", classLoader);
            Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(
                    simReaderClass,
                    "LIZJ",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args.length == 2 && param.args[1] instanceof Integer) {
                                param.setResult(XposedHelpers.newInstance(
                                        simInfoClass,
                                        param.args[1],
                                        profile.countryIso,
                                        profile.carrier));
                            }
                        }
                    });
            return hooks.size();
        } catch (Throwable throwable) {
            logFailure("X.0V1x.LIZJ", throwable);
            return 0;
        }
    }

    private static int hookAll(Class<?> targetClass, String methodName, Object result) {
        try {
            return XposedBridge.hookAllMethods(
                    targetClass,
                    methodName,
                    XC_MethodReplacement.returnConstant(result)).size();
        } catch (Throwable throwable) {
            logFailure(targetClass.getName() + "." + methodName, throwable);
            return 0;
        }
    }

    private static int hookAll(
            ClassLoader classLoader,
            String className,
            String methodName,
            Object result) {
        try {
            Class<?> targetClass = XposedHelpers.findClass(className, classLoader);
            return hookAll(targetClass, methodName, result);
        } catch (Throwable throwable) {
            logFailure(className + "." + methodName, throwable);
            return 0;
        }
    }

    private static void logFailure(String hook, Throwable throwable) {
        log("skipped " + hook + ": " + throwable.getClass().getSimpleName());
    }

    private static void log(String message) {
        XposedBridge.log(LOG_PREFIX + message);
        Log.i(LOG_TAG, message);
    }
}
