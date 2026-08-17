package com.local.tiktokregion;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class MainActivity extends Activity {
    private static final int ACCENT = Color.rgb(254, 44, 85);
    private static final int TEXT_PRIMARY = Color.rgb(22, 24, 29);
    private static final int TEXT_SECONDARY = Color.rgb(93, 95, 100);
    private static final int DIVIDER = Color.rgb(229, 229, 232);

    private SharedPreferences preferences;
    private Switch enabledSwitch;
    private Switch skipStartupLoginSwitch;
    private RadioGroup profileGroup;
    private Spinner timeZoneSpinner;
    private TextView activeProfileView;
    private Button applyButton;
    private Button clearButton;
    private final Map<String, Integer> profileViewIds = new HashMap<>();
    private final Map<String, String> selectedTimeZones = new HashMap<>();
    private RegionTimeZone[] visibleTimeZones = new RegionTimeZone[0];
    private String spinnerProfileId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        preferences = getSharedPreferences(ConfigContract.PREFERENCES, Context.MODE_PRIVATE);
        setContentView(createContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences != null && enabledSwitch != null && profileGroup != null) {
            restoreSavedSelection();
        }
    }

    private View createContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(24), dp(24), dp(24), dp(18));

        TextView title = text(getString(R.string.title), 24, TEXT_PRIMARY);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        header.addView(title);

        activeProfileView = text("", 14, TEXT_SECONDARY);
        LinearLayout.LayoutParams activeParams = matchWrap();
        activeParams.topMargin = dp(6);
        header.addView(activeProfileView, activeParams);
        root.addView(header);

        View headerDivider = new View(this);
        headerDivider.setBackgroundColor(DIVIDER);
        root.addView(headerDivider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(16));

        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setGravity(Gravity.CENTER_VERTICAL);
        switchRow.setMinimumHeight(dp(56));

        TextView switchLabel = text(getString(R.string.enable_override), 16, TEXT_PRIMARY);
        switchRow.addView(switchLabel, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));

        enabledSwitch = new Switch(this);
        enabledSwitch.setThumbTintList(new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{ACCENT, Color.rgb(142, 145, 151)}));
        enabledSwitch.setChecked(preferences.getBoolean(ConfigContract.KEY_ENABLED, true));
        switchRow.addView(enabledSwitch);
        content.addView(switchRow);

        LinearLayout skipLoginRow = new LinearLayout(this);
        skipLoginRow.setGravity(Gravity.CENTER_VERTICAL);
        skipLoginRow.setMinimumHeight(dp(56));

        TextView skipLoginLabel = text(
                getString(R.string.skip_startup_login),
                16,
                TEXT_PRIMARY);
        skipLoginRow.addView(skipLoginLabel, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));

        skipStartupLoginSwitch = new Switch(this);
        skipStartupLoginSwitch.setThumbTintList(new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{ACCENT, Color.rgb(142, 145, 151)}));
        skipStartupLoginSwitch.setChecked(preferences.getBoolean(
                ConfigContract.KEY_SKIP_STARTUP_LOGIN,
                true));
        skipLoginRow.addView(skipStartupLoginSwitch);
        content.addView(skipLoginRow);

        LinearLayout timeZoneRow = new LinearLayout(this);
        timeZoneRow.setGravity(Gravity.CENTER_VERTICAL);
        timeZoneRow.setMinimumHeight(dp(56));

        TextView timeZoneLabel = text(getString(R.string.time_zone_city), 16, TEXT_PRIMARY);
        timeZoneRow.addView(timeZoneLabel, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));

        timeZoneSpinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        timeZoneSpinner.setMinimumWidth(dp(184));
        timeZoneSpinner.setMinimumHeight(dp(48));
        timeZoneRow.addView(timeZoneSpinner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(timeZoneRow);

        TextView sectionTitle = text(getString(R.string.region_presets), 13, TEXT_SECONDARY);
        sectionTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams sectionParams = matchWrap();
        sectionParams.topMargin = dp(14);
        sectionParams.bottomMargin = dp(6);
        content.addView(sectionTitle, sectionParams);

        profileGroup = new RadioGroup(this);
        profileGroup.setOrientation(RadioGroup.VERTICAL);
        for (RegionProfile profile : RegionProfile.all()) {
            RadioButton option = new RadioButton(this);
            option.setId(View.generateViewId());
            option.setTag(profile.id);
            option.setText(getString(
                    R.string.profile_row,
                    profile.displayName,
                    profile.region,
                    profile.carrier,
                    profile.mccMnc));
            option.setTextColor(TEXT_PRIMARY);
            option.setTextSize(16);
            option.setGravity(Gravity.CENTER_VERTICAL);
            option.setMinHeight(dp(72));
            option.setPadding(dp(4), dp(8), dp(4), dp(8));
            option.setButtonTintList(new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{}
                    },
                    new int[]{ACCENT, Color.rgb(142, 145, 151)}));
            profileGroup.addView(option, new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            profileViewIds.put(profile.id, option.getId());
        }
        content.addView(profileGroup);

        Space bottomSpace = new Space(this);
        content.addView(bottomSpace, new LinearLayout.LayoutParams(1, dp(12)));
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f));

        applyButton = new Button(this);
        applyButton.setText(R.string.apply_and_open);
        applyButton.setTextColor(Color.WHITE);
        applyButton.setTextSize(16);
        applyButton.setAllCaps(false);
        applyButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        applyButton.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        applyButton.setOnClickListener(view -> applyAndRestartTikTok());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56));
        buttonParams.setMargins(dp(20), dp(10), dp(20), dp(4));
        root.addView(applyButton, buttonParams);

        clearButton = new Button(this);
        clearButton.setText(R.string.clear_and_restart);
        clearButton.setTextColor(TEXT_PRIMARY);
        clearButton.setTextSize(15);
        clearButton.setAllCaps(false);
        clearButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        clearButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(238, 238, 240)));
        clearButton.setOnClickListener(view -> clearRegionAndRestartTikTok());
        LinearLayout.LayoutParams clearButtonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52));
        clearButtonParams.setMargins(dp(20), 0, dp(20), dp(14));
        root.addView(clearButton, clearButtonParams);

        CompoundButton.OnCheckedChangeListener stateListener = (button, checked) -> {
            setOptionsEnabled(checked);
            updateActiveProfile();
        };
        enabledSwitch.setOnCheckedChangeListener(stateListener);
        profileGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateTimeZoneOptions();
            updateActiveProfile();
        });
        timeZoneSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id) {
                if (spinnerProfileId != null
                        && position >= 0
                        && position < visibleTimeZones.length) {
                    selectedTimeZones.put(
                            spinnerProfileId,
                            visibleTimeZones[position].timeZoneId);
                }
                updateActiveProfile();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateActiveProfile();
            }
        });
        restoreSavedSelection();
        return root;
    }

    private void clearRegionAndRestartTikTok() {
        enabledSwitch.setChecked(false);
        applyAndRestartTikTok();
    }

    private void applyAndRestartTikTok() {
        RegionProfile selected = getSelectedProfile();
        boolean saved = preferences.edit()
                .putBoolean(ConfigContract.KEY_ENABLED, enabledSwitch.isChecked())
                .putString(ConfigContract.KEY_PROFILE_ID, selected.id)
                .putString(ConfigContract.KEY_TIME_ZONE_ID, selected.timeZoneId)
                .putBoolean(
                        ConfigContract.KEY_SKIP_STARTUP_LOGIN,
                        skipStartupLoginSwitch.isChecked())
                .commit();
        if (!saved) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        updateActiveProfile();

        List<TikTokTarget> installedTargets = findInstalledTikTokTargets();
        if (installedTargets.isEmpty()) {
            Toast.makeText(this, R.string.tiktok_not_installed, Toast.LENGTH_SHORT).show();
            return;
        }

        setRestartButtonsEnabled(false);
        boolean overrideEnabled = enabledSwitch.isChecked();
        boolean skipStartupLogin = skipStartupLoginSwitch.isChecked();
        new Thread(() -> {
            boolean synchronizedConfig = synchronizeTargetConfigs(
                    installedTargets,
                    selected,
                    overrideEnabled,
                    skipStartupLogin);
            boolean restarted;
            if (synchronizedConfig) {
                restarted = restartTikTok(installedTargets);
            } else {
                forceStopTikTok(installedTargets);
                restarted = false;
            }
            runOnUiThread(() -> {
                setRestartButtonsEnabled(true);
                if (restarted) {
                    return;
                }
                Toast.makeText(
                        this,
                        synchronizedConfig
                                ? R.string.force_stop_failed
                                : R.string.config_sync_failed,
                        Toast.LENGTH_SHORT).show();
                if (!synchronizedConfig) {
                    return;
                }
                Intent fallbackLaunch = getPackageManager().getLaunchIntentForPackage(
                        installedTargets.get(0).packageName);
                if (fallbackLaunch != null) {
                    startActivity(fallbackLaunch);
                }
            });
        }, "TikTok-force-stop").start();
    }

    private void setRestartButtonsEnabled(boolean enabled) {
        applyButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
    }

    private List<TikTokTarget> findInstalledTikTokTargets() {
        List<TikTokTarget> installedTargets = new ArrayList<>();
        for (String packageName : ConfigContract.TARGET_PACKAGES) {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null && launchIntent.getComponent() != null) {
                installedTargets.add(new TikTokTarget(
                        packageName,
                        launchIntent.getComponent().flattenToShortString()));
            }
        }
        return installedTargets;
    }

    private void sendProfileChanged(
            String packageName,
            RegionProfile selected,
            boolean overrideEnabled,
            boolean skipStartupLogin,
            String syncToken) {
        Intent changed = new Intent(ConfigContract.ACTION_PROFILE_CHANGED);
        changed.setPackage(packageName);
        changed.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        changed.putExtra(ConfigContract.KEY_ENABLED, overrideEnabled);
        changed.putExtra(ConfigContract.KEY_PROFILE_ID, selected.id);
        changed.putExtra(ConfigContract.KEY_TIME_ZONE_ID, selected.timeZoneId);
        changed.putExtra(ConfigContract.KEY_SKIP_STARTUP_LOGIN, skipStartupLogin);
        changed.putExtra(ConfigContract.EXTRA_SYNC_TOKEN, syncToken);
        sendBroadcast(changed);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private boolean synchronizeTargetConfigs(
            List<TikTokTarget> targets,
            RegionProfile selected,
            boolean overrideEnabled,
            boolean skipStartupLogin) {
        String syncToken = UUID.randomUUID().toString();
        Set<String> pendingPackages = Collections.synchronizedSet(new HashSet<>());
        for (TikTokTarget target : targets) {
            pendingPackages.add(target.packageName);
        }
        CountDownLatch appliedLatch = new CountDownLatch(targets.size());
        BroadcastReceiver appliedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!ConfigContract.ACTION_PROFILE_APPLIED.equals(intent.getAction())
                        || !syncToken.equals(intent.getStringExtra(
                                ConfigContract.EXTRA_SYNC_TOKEN))) {
                    return;
                }
                String packageName = intent.getStringExtra(
                        ConfigContract.EXTRA_TARGET_PACKAGE);
                if (packageName != null && pendingPackages.remove(packageName)) {
                    appliedLatch.countDown();
                }
            }
        };
        IntentFilter filter = new IntentFilter(ConfigContract.ACTION_PROFILE_APPLIED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(appliedReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(appliedReceiver, filter);
        }

        try {
            for (TikTokTarget target : targets) {
                if (!runRootCommand("am start -n " + target.launchComponent)) {
                    return false;
                }
            }
            for (int attempt = 0; attempt < 12 && !pendingPackages.isEmpty(); attempt++) {
                List<String> pendingSnapshot;
                synchronized (pendingPackages) {
                    pendingSnapshot = new ArrayList<>(pendingPackages);
                }
                for (String packageName : pendingSnapshot) {
                    sendProfileChanged(
                            packageName,
                            selected,
                            overrideEnabled,
                            skipStartupLogin,
                            syncToken);
                }
                try {
                    if (appliedLatch.await(300L, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return pendingPackages.isEmpty();
        } finally {
            unregisterReceiver(appliedReceiver);
        }
    }

    private static boolean restartTikTok(List<TikTokTarget> targets) {
        if (!forceStopTikTok(targets)) {
            return false;
        }
        return runRootCommand("am start -n " + targets.get(0).launchComponent);
    }

    private static boolean forceStopTikTok(List<TikTokTarget> targets) {
        boolean stopped = true;
        for (TikTokTarget target : targets) {
            if (!runRootCommand("am force-stop " + target.packageName)) {
                stopped = false;
            }
        }
        return stopped;
    }

    private static boolean runRootCommand(String command) {
        try {
            java.lang.Process process = new ProcessBuilder(
                    "su",
                    "-c",
                    command)
                    .redirectErrorStream(true)
                    .start();
            try (InputStream output = process.getInputStream()) {
                byte[] buffer = new byte[256];
                while (output.read(buffer) != -1) {
                    // Drain su output so waitFor cannot block on a full pipe.
                }
            }
            return process.waitFor() == 0;
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static final class TikTokTarget {
        final String packageName;
        final String launchComponent;

        TikTokTarget(String packageName, String launchComponent) {
            this.packageName = packageName;
            this.launchComponent = launchComponent;
        }
    }

    private RegionProfile getSelectedProfile() {
        RegionProfile profile = getSelectedBaseProfile();
        RegionTimeZone timeZone = getSelectedTimeZone(profile.id);
        return profile.withTimeZone(timeZone.timeZoneId);
    }

    private RegionProfile getSelectedBaseProfile() {
        RadioButton selected = findViewById(profileGroup.getCheckedRadioButtonId());
        if (selected == null || selected.getTag() == null) {
            return RegionProfile.find("US");
        }
        return RegionProfile.find(selected.getTag().toString());
    }

    private void restoreSavedSelection() {
        boolean enabled = preferences.getBoolean(ConfigContract.KEY_ENABLED, true);
        String profileId = preferences.getString(ConfigContract.KEY_PROFILE_ID, "US");
        selectedTimeZones.put(
                profileId,
                RegionTimeZone.resolve(
                        profileId,
                        preferences.getString(ConfigContract.KEY_TIME_ZONE_ID, null))
                        .timeZoneId);
        Integer viewId = profileViewIds.get(profileId);

        enabledSwitch.setChecked(enabled);
        skipStartupLoginSwitch.setChecked(preferences.getBoolean(
                ConfigContract.KEY_SKIP_STARTUP_LOGIN,
                true));
        if (viewId != null && profileGroup.getCheckedRadioButtonId() != viewId) {
            profileGroup.check(viewId);
        }
        updateTimeZoneOptions();
        setOptionsEnabled(enabled);
        updateActiveProfile();
    }

    private void setOptionsEnabled(boolean enabled) {
        for (int index = 0; index < profileGroup.getChildCount(); index++) {
            profileGroup.getChildAt(index).setEnabled(enabled);
        }
        timeZoneSpinner.setEnabled(enabled);
    }

    private void updateActiveProfile() {
        if (!enabledSwitch.isChecked()) {
            activeProfileView.setText(R.string.active_follow_system);
            return;
        }
        RegionProfile profile = getSelectedProfile();
        RegionTimeZone timeZone = getSelectedTimeZone(profile.id);
        activeProfileView.setText(getString(
                R.string.active_profile,
                profile.displayName,
                profile.region,
                timeZone.displayName));
    }

    private void updateTimeZoneOptions() {
        if (timeZoneSpinner == null || profileGroup == null) {
            return;
        }
        if (spinnerProfileId != null && visibleTimeZones.length > 0) {
            int selectedPosition = timeZoneSpinner.getSelectedItemPosition();
            if (selectedPosition >= 0 && selectedPosition < visibleTimeZones.length) {
                selectedTimeZones.put(
                        spinnerProfileId,
                        visibleTimeZones[selectedPosition].timeZoneId);
            }
        }

        RegionProfile profile = getSelectedBaseProfile();
        spinnerProfileId = profile.id;
        visibleTimeZones = RegionTimeZone.all(profile.id);
        List<String> labels = new ArrayList<>();
        for (RegionTimeZone timeZone : visibleTimeZones) {
            labels.add(timeZone.displayName);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeZoneSpinner.setAdapter(adapter);

        String preferredTimeZoneId = selectedTimeZones.get(profile.id);
        RegionTimeZone selectedTimeZone = RegionTimeZone.resolve(
                profile.id,
                preferredTimeZoneId);
        for (int index = 0; index < visibleTimeZones.length; index++) {
            if (visibleTimeZones[index].timeZoneId.equals(selectedTimeZone.timeZoneId)) {
                timeZoneSpinner.setSelection(index, false);
                selectedTimeZones.put(profile.id, selectedTimeZone.timeZoneId);
                break;
            }
        }
    }

    private RegionTimeZone getSelectedTimeZone(String profileId) {
        if (profileId.equals(spinnerProfileId) && visibleTimeZones.length > 0) {
            int position = timeZoneSpinner.getSelectedItemPosition();
            if (position >= 0 && position < visibleTimeZones.length) {
                return visibleTimeZones[position];
            }
        }
        return RegionTimeZone.resolve(profileId, selectedTimeZones.get(profileId));
    }

    private TextView text(String value, int sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setLetterSpacing(0f);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
