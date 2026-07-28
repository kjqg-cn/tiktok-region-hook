package com.local.tiktokregion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MainActivity extends Activity {
    private static final int ACCENT = Color.rgb(254, 44, 85);
    private static final int TEXT_PRIMARY = Color.rgb(22, 24, 29);
    private static final int TEXT_SECONDARY = Color.rgb(93, 95, 100);
    private static final int DIVIDER = Color.rgb(229, 229, 232);

    private SharedPreferences preferences;
    private Switch enabledSwitch;
    private Switch skipStartupLoginSwitch;
    private RadioGroup profileGroup;
    private TextView activeProfileView;
    private Button applyButton;
    private Button clearButton;
    private final Map<String, Integer> profileViewIds = new HashMap<>();

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
        profileGroup.setOnCheckedChangeListener((group, checkedId) -> updateActiveProfile());
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
                .putBoolean(
                        ConfigContract.KEY_SKIP_STARTUP_LOGIN,
                        skipStartupLoginSwitch.isChecked())
                .commit();
        if (!saved) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        updateActiveProfile();

        List<String> installedPackages = findInstalledTikTokPackages();
        if (installedPackages.isEmpty()) {
            Toast.makeText(this, R.string.tiktok_not_installed, Toast.LENGTH_SHORT).show();
            return;
        }

        for (String packageName : installedPackages) {
            sendProfileChanged(packageName, selected);
        }

        setRestartButtonsEnabled(false);
        String launchPackage = installedPackages.get(0);
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(launchPackage);
        if (launchIntent == null || launchIntent.getComponent() == null) {
            setRestartButtonsEnabled(true);
            Toast.makeText(this, R.string.tiktok_not_installed, Toast.LENGTH_SHORT).show();
            return;
        }
        String launchComponent = launchIntent.getComponent().flattenToShortString();
        new Thread(() -> {
            boolean restarted = restartTikTok(installedPackages, launchComponent);
            runOnUiThread(() -> {
                setRestartButtonsEnabled(true);
                if (restarted) {
                    return;
                }
                Toast.makeText(
                        this,
                        R.string.force_stop_failed,
                        Toast.LENGTH_SHORT).show();
                Intent fallbackLaunch = getPackageManager()
                        .getLaunchIntentForPackage(launchPackage);
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

    private List<String> findInstalledTikTokPackages() {
        List<String> installedPackages = new ArrayList<>();
        for (String packageName : ConfigContract.TARGET_PACKAGES) {
            if (getPackageManager().getLaunchIntentForPackage(packageName) != null) {
                installedPackages.add(packageName);
            }
        }
        return installedPackages;
    }

    private void sendProfileChanged(String packageName, RegionProfile selected) {
        Intent changed = new Intent(ConfigContract.ACTION_PROFILE_CHANGED);
        changed.setPackage(packageName);
        changed.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        changed.putExtra(ConfigContract.KEY_ENABLED, enabledSwitch.isChecked());
        changed.putExtra(ConfigContract.KEY_PROFILE_ID, selected.id);
        changed.putExtra(
                ConfigContract.KEY_SKIP_STARTUP_LOGIN,
                skipStartupLoginSwitch.isChecked());
        sendBroadcast(changed);
    }

    private static boolean restartTikTok(
            List<String> packageNames,
            String launchComponent) {
        StringBuilder command = new StringBuilder();
        for (String packageName : packageNames) {
            if (command.length() > 0) {
                command.append(" && ");
            }
            command.append("am force-stop ").append(packageName);
        }
        command.append(" && am start -n ").append(launchComponent);

        try {
            java.lang.Process process = new ProcessBuilder(
                    "su",
                    "-c",
                    command.toString())
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

    private RegionProfile getSelectedProfile() {
        RadioButton selected = findViewById(profileGroup.getCheckedRadioButtonId());
        if (selected == null || selected.getTag() == null) {
            return RegionProfile.find("US");
        }
        return RegionProfile.find(selected.getTag().toString());
    }

    private void restoreSavedSelection() {
        boolean enabled = preferences.getBoolean(ConfigContract.KEY_ENABLED, true);
        String profileId = preferences.getString(ConfigContract.KEY_PROFILE_ID, "US");
        Integer viewId = profileViewIds.get(profileId);

        enabledSwitch.setChecked(enabled);
        skipStartupLoginSwitch.setChecked(preferences.getBoolean(
                ConfigContract.KEY_SKIP_STARTUP_LOGIN,
                true));
        if (viewId != null && profileGroup.getCheckedRadioButtonId() != viewId) {
            profileGroup.check(viewId);
        }
        setOptionsEnabled(enabled);
        updateActiveProfile();
    }

    private void setOptionsEnabled(boolean enabled) {
        for (int index = 0; index < profileGroup.getChildCount(); index++) {
            profileGroup.getChildAt(index).setEnabled(enabled);
        }
    }

    private void updateActiveProfile() {
        if (!enabledSwitch.isChecked()) {
            activeProfileView.setText(R.string.active_follow_system);
            return;
        }
        RegionProfile profile = getSelectedProfile();
        activeProfileView.setText(getString(
                R.string.active_profile,
                profile.displayName,
                profile.region));
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
