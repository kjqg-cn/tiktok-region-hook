package com.local.tiktokregion;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.MotionEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;

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
    private static final int BACKGROUND = Color.rgb(245, 246, 248);
    private long touchReadyAt;
    private boolean acceptedTouch;

    private SharedPreferences preferences;
    private Switch enabledSwitch;
    private Switch skipStartupLoginSwitch;
    private RadioGroup profileGroup;
    private LinearLayout timeZoneRow;
    private TextView timeZoneValue;
    private TextView activeProfileView;
    private TextView moduleStatusView;
    private boolean restartInProgress;
    private Button applyButton;
    private Button clearButton;
    private final Map<String, Integer> profileViewIds = new HashMap<>();
    private final Map<String, String> selectedTimeZones = new HashMap<>();
    private boolean aboutVisible;
    private PendingEditorState pendingEditorState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        preferences = getSharedPreferences(ConfigContract.PREFERENCES, Context.MODE_PRIVATE);
        setContentView(createContent());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Consume the entire launch gesture, including a trailing UP or rapid second tap.
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            acceptedTouch = event.getDownTime() >= touchReadyAt;
        }
        if (!acceptedTouch) {
            return true;
        }
        boolean handled = super.dispatchTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            acceptedTouch = false;
        }
        return handled;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Guard app entry, not focus returning from a picker or PopupMenu.
        acceptedTouch = false;
        touchReadyAt = SystemClock.uptimeMillis() + 500L;
        if (!aboutVisible && preferences != null && enabledSwitch != null && profileGroup != null) {
            restoreSavedSelection();
        }
    }

    @Override
    public void onBackPressed() {
        if (aboutVisible) {
            showMainPage();
            return;
        }
        super.onBackPressed();
    }

    private View createContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(16), dp(4), dp(12), dp(4));
        TextView title = text(getString(R.string.title), 22, TEXT_PRIMARY);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        toolbar.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageButton aboutButton = iconButton(R.drawable.ic_more_vertical, R.string.about_accessibility);
        aboutButton.setOnClickListener(this::showAboutMenu);
        toolbar.addView(aboutButton, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(toolbar, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(12), 0, dp(12), dp(8));
        moduleStatusView = text("", 13, TEXT_SECONDARY);
        moduleStatusView.setPadding(dp(12), dp(8), dp(12), dp(8));
        body.addView(moduleStatusView, matchWrap());

        activeProfileView = text("", 13, TEXT_SECONDARY);
        activeProfileView.setPadding(dp(4), dp(8), dp(4), dp(8));
        body.addView(activeProfileView, matchWrap());

        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.VERTICAL);
        settings.setBackground(rounded(Color.WHITE, 12));
        enabledSwitch = settingSwitch(R.string.enable_override);
        enabledSwitch.setChecked(preferences.getBoolean(ConfigContract.KEY_ENABLED, true));
        settings.addView(enabledSwitch, matchWrap());
        skipStartupLoginSwitch = settingSwitch(R.string.skip_startup_login);
        skipStartupLoginSwitch.setChecked(preferences.getBoolean(
                ConfigContract.KEY_SKIP_STARTUP_LOGIN, true));
        settings.addView(skipStartupLoginSwitch, matchWrap());

        timeZoneRow = new LinearLayout(this);
        timeZoneRow.setGravity(Gravity.CENTER_VERTICAL);
        timeZoneRow.setMinimumHeight(dp(48));
        timeZoneRow.setPadding(dp(12), dp(6), dp(12), dp(6));
        timeZoneRow.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.argb(20, 0, 0, 0)), null, rounded(Color.WHITE, 12)));
        TextView timeZoneLabel = text(getString(R.string.time_zone_city), 14, TEXT_PRIMARY);
        timeZoneRow.addView(timeZoneLabel);
        timeZoneValue = text("", 13, TEXT_SECONDARY);
        timeZoneValue.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        timeZoneValue.setPadding(dp(8), 0, 0, 0);
        timeZoneRow.addView(timeZoneValue, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        timeZoneRow.setOnClickListener(view -> showTimeZonePicker());
        settings.addView(timeZoneRow, matchWrap());
        body.addView(settings, matchWrap());

        TextView section = sectionLabel(R.string.region_presets);
        section.setPadding(dp(4), dp(12), 0, dp(6));
        body.addView(section, matchWrap());
        profileGroup = new RadioGroup(this);
        profileGroup.setOrientation(RadioGroup.VERTICAL);
        profileViewIds.clear();
        for (RegionProfile profile : RegionProfile.all()) {
            RadioButton option = new RadioButton(this);
            option.setId(View.generateViewId());
            option.setTag(profile.id);
            String label = getString(R.string.profile_row,
                    profile.displayName, profile.region, profile.carrier, profile.mccMnc);
            SpannableString styledLabel = new SpannableString(label);
            int detailStart = label.indexOf('\n') + 1;
            styledLabel.setSpan(new RelativeSizeSpan(0.85f), detailStart, label.length(), 0);
            option.setText(styledLabel);
            option.setTextColor(TEXT_PRIMARY);
            option.setTextSize(14);
            option.setGravity(Gravity.CENTER_VERTICAL);
            option.setMinHeight(dp(56));
            option.setPadding(dp(10), dp(6), dp(10), dp(6));
            StateListDrawable states = new StateListDrawable();
            GradientDrawable selectedBackground = rounded(Color.rgb(255, 241, 245), 12);
            selectedBackground.setStroke(dp(1), ACCENT);
            states.addState(new int[]{android.R.attr.state_checked}, selectedBackground);
            states.addState(new int[]{}, rounded(Color.WHITE, 12));
            option.setBackground(states);
            option.setButtonTintList(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{ACCENT, Color.rgb(142, 145, 151)}));
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(4);
            profileGroup.addView(option, params);
            profileViewIds.put(profile.id, option.getId());
        }
        body.addView(profileGroup, matchWrap());
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        clearButton = actionButton(R.string.clear_and_restart, false);
        clearButton.setOnClickListener(view -> clearRegionAndRestartTikTok());
        applyButton = actionButton(R.string.apply_and_open, true);
        applyButton.setOnClickListener(view -> applyAndRestartTikTok());
        LinearLayout actions = new LinearLayout(this);
        actions.setBaselineAligned(false);
        actions.setPadding(dp(12), dp(6), dp(12), dp(8));
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clearParams.setMarginEnd(dp(4));
        actions.addView(clearButton, clearParams);
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        applyParams.setMarginStart(dp(4));
        actions.addView(applyButton, applyParams);
        root.addView(actions, matchWrap());

        enabledSwitch.setOnCheckedChangeListener((button, checked) -> {
            setOptionsEnabled(checked);
            updateActiveProfile();
        });
        skipStartupLoginSwitch.setOnCheckedChangeListener((button, checked) -> updateActiveProfile());
        profileGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateTimeZoneOptions();
            updateActiveProfile();
        });
        restoreSavedSelection();
        return root;
    }

    private Switch settingSwitch(int labelRes) {
        Switch control = new Switch(this);
        control.setText(labelRes);
        control.setTextSize(14);
        control.setTextColor(TEXT_PRIMARY);
        control.setGravity(Gravity.CENTER_VERTICAL);
        control.setMinHeight(dp(48));
        control.setPadding(dp(12), dp(6), dp(12), dp(6));
        control.setSwitchPadding(dp(12));
        control.setThumbTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{ACCENT, Color.rgb(142, 145, 151)}));
        control.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.argb(20, 0, 0, 0)), null, rounded(Color.WHITE, 12)));
        return control;
    }

    private Button actionButton(int labelRes, boolean primary) {
        Button button = new Button(this);
        button.setText(labelRes);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setTextColor(primary ? Color.WHITE : TEXT_PRIMARY);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        styleActionButton(button, primary ? ACCENT : Color.rgb(232, 234, 239));
        return button;
    }

    private void showTimeZonePicker() {
        if (!timeZoneRow.isEnabled()) {
            return;
        }
        RegionProfile profile = getSelectedBaseProfile();
        RegionTimeZone[] choices = RegionTimeZone.all(profile.id);
        String[] labels = new String[choices.length];
        int selectedIndex = 0;
        String currentId = getSelectedTimeZone(profile.id).timeZoneId;
        for (int index = 0; index < choices.length; index++) {
            labels[index] = choices[index].displayName;
            if (choices[index].timeZoneId.equals(currentId)) {
                selectedIndex = index;
            }
        }
        AlertDialog picker = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.time_zone_picker_title, profile.displayName))
                .setSingleChoiceItems(labels, selectedIndex, (dialog, which) -> {
                    selectedTimeZones.put(profile.id, choices[which].timeZoneId);
                    updateTimeZoneOptions();
                    updateActiveProfile();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        picker.getWindow().setWindowAnimations(0);
        picker.show();
    }

    private ImageButton iconButton(int iconResId, int descriptionResId) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconResId);
        button.setContentDescription(getString(descriptionResId));
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setColorFilter(TEXT_PRIMARY);
        button.setMinimumWidth(dp(48));
        button.setMinimumHeight(dp(48));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        return button;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radius));
        return background;
    }

    private void styleActionButton(Button button, int color) {
        button.setStateListAnimator(null);
        button.setElevation(0f);
        button.setTranslationZ(0f);
        button.setBackgroundTintList(null);
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.argb(28, 0, 0, 0)),
                rounded(color, 14), null));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(48));
        button.setMinimumHeight(dp(48));
        button.setPadding(dp(6), dp(8), dp(6), dp(8));
    }

    private void showAboutMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add(0, 1, 0, R.string.about_menu);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showAboutPage();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void showAboutPage() {
        pendingEditorState = captureEditorState();
        aboutVisible = true;
        setContentView(createAboutContent());
    }

    private void showMainPage() {
        aboutVisible = false;
        setContentView(createContent());
    }

    private View createAboutContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), dp(12), dp(24), dp(12));
        ImageButton backButton = iconButton(
                R.drawable.ic_arrow_back,
                R.string.about_back);
        backButton.setOnClickListener(view -> showMainPage());
        toolbar.addView(backButton);
        TextView title = text(getString(R.string.about_title), 20, TEXT_PRIMARY);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        toolbar.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(toolbar);

        View divider = new View(this);
        divider.setBackgroundColor(DIVIDER);
        root.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)));

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(28), dp(20), dp(32));

        TextView moduleName = text(
                getString(R.string.about_module_name),
                24,
                TEXT_PRIMARY);
        moduleName.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        content.addView(moduleName);

        TextView moduleVersion = text(
                getString(R.string.about_module_version, getModuleVersion()),
                14,
                TEXT_SECONDARY);
        LinearLayout.LayoutParams versionParams = matchWrap();
        versionParams.topMargin = dp(8);
        content.addView(moduleVersion, versionParams);

        content.addView(sectionLabel(R.string.about_compatibility), sectionMargin());
        LinearLayout compatibility = new LinearLayout(this);
        compatibility.setOrientation(LinearLayout.VERTICAL);
        compatibility.setBackgroundColor(Color.rgb(247, 247, 248));
        compatibility.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView latestLabel = text(
                getString(R.string.about_latest_tiktok),
                15,
                TEXT_PRIMARY);
        compatibility.addView(latestLabel);
        TextView latestVersion = text(
                "Google Play " + ConfigContract.VERIFIED_LATEST_TIKTOK_VERSION,
                16,
                TEXT_PRIMARY);
        latestVersion.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams latestParams = matchWrap();
        latestParams.topMargin = dp(8);
        compatibility.addView(latestVersion, latestParams);
        TextView packages = text(getString(
                R.string.about_target_packages,
                ConfigContract.TARGET_MUSICAL_LY_PACKAGE,
                ConfigContract.TARGET_TRILL_PACKAGE), 13, TEXT_SECONDARY);
        LinearLayout.LayoutParams packagesParams = matchWrap();
        packagesParams.topMargin = dp(8);
        compatibility.addView(packages, packagesParams);
        TextView verificationScope = text(getString(R.string.about_verification_scope),
                13, TEXT_SECONDARY);
        LinearLayout.LayoutParams scopeParams = matchWrap();
        scopeParams.topMargin = dp(8);
        compatibility.addView(verificationScope, scopeParams);
        content.addView(compatibility);

        content.addView(sectionLabel(R.string.about_project), sectionMargin());
        content.addView(linkRow(
                R.string.about_source,
                R.string.about_source_summary,
                ConfigContract.SOURCE_URL));
        content.addView(linkRow(
                R.string.about_feedback,
                R.string.about_feedback_summary,
                ConfigContract.ISSUES_URL));
        content.addView(linkRow(
                R.string.about_license,
                R.string.about_license_summary,
                ConfigContract.LICENSE_URL));

        TextView disclaimer = text(
                "本项目为独立的 Root + LSPosed 设备自定义工具，与 TikTok、ByteDance 及相关平台不存在隶属、合作或授权关系。",
                13,
                TEXT_SECONDARY);
        disclaimer.setLineSpacing(0f, 1.35f);
        LinearLayout.LayoutParams disclaimerParams = matchWrap();
        disclaimerParams.topMargin = dp(42);
        content.addView(disclaimer, disclaimerParams);

        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f));
        return root;
    }

    private PendingEditorState captureEditorState() {
        if (enabledSwitch == null || skipStartupLoginSwitch == null || profileGroup == null) {
            return null;
        }
        RegionProfile profile = getSelectedBaseProfile();
        RegionTimeZone timeZone = getSelectedTimeZone(profile.id);
        return new PendingEditorState(
                enabledSwitch.isChecked(),
                skipStartupLoginSwitch.isChecked(),
                profile.id,
                timeZone.timeZoneId);
    }

    private TextView sectionLabel(int stringResId) {
        TextView label = text(getString(stringResId), 13, TEXT_SECONDARY);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return label;
    }

    private LinearLayout.LayoutParams sectionMargin() {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(28);
        params.bottomMargin = dp(8);
        return params;
    }

    private View linkRow(int titleResId, int summaryResId, String url) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(64));
        row.setPadding(0, dp(8), 0, dp(8));
        row.setContentDescription(getString(titleResId) + "，" + getString(summaryResId));
        row.setOnClickListener(view -> openExternalUrl(url));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(getString(titleResId), 16, TEXT_PRIMARY);
        copy.addView(title);
        TextView summary = text(getString(summaryResId), 13, TEXT_SECONDARY);
        LinearLayout.LayoutParams summaryParams = matchWrap();
        summaryParams.topMargin = dp(4);
        copy.addView(summary, summaryParams);
        row.addView(copy, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));

        TextView arrow = text("›", 28, TEXT_SECONDARY);
        row.addView(arrow, new LinearLayout.LayoutParams(
                dp(32),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private void openExternalUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Throwable throwable) {
            Toast.makeText(this, R.string.about_external_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private String getModuleVersion() {
        try {
            return getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionName;
        } catch (Throwable throwable) {
            return "unknown";
        }
    }

    private void clearRegionAndRestartTikTok() {
        if (!ModuleStatus.isActive() || restartInProgress) {
            return;
        }
        enabledSwitch.setChecked(false);
        applyAndRestartTikTok();
    }

    private void applyAndRestartTikTok() {
        if (!ModuleStatus.isActive() || restartInProgress) {
            return;
        }
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
        restartInProgress = !enabled;
        setOptionsEnabled(enabledSwitch.isChecked());
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
        RadioButton selected = profileGroup.findViewById(profileGroup.getCheckedRadioButtonId());
        if (selected == null || selected.getTag() == null) {
            return RegionProfile.find("US");
        }
        return RegionProfile.find(selected.getTag().toString());
    }

    private void restoreSavedSelection() {
        PendingEditorState pending = pendingEditorState;
        pendingEditorState = null;
        boolean enabled = pending != null
                ? pending.enabled
                : preferences.getBoolean(ConfigContract.KEY_ENABLED, true);
        boolean skipStartupLogin = pending != null
                ? pending.skipStartupLogin
                : preferences.getBoolean(ConfigContract.KEY_SKIP_STARTUP_LOGIN, true);
        String profileId = pending != null
                ? pending.profileId
                : preferences.getString(ConfigContract.KEY_PROFILE_ID, "US");
        String timeZoneId = pending != null
                ? pending.timeZoneId
                : preferences.getString(ConfigContract.KEY_TIME_ZONE_ID, null);
        selectedTimeZones.put(
                profileId,
                RegionTimeZone.resolve(
                        profileId,
                        timeZoneId)
                        .timeZoneId);
        Integer viewId = profileViewIds.get(profileId);

        enabledSwitch.setChecked(enabled);
        skipStartupLoginSwitch.setChecked(skipStartupLogin);
        if (viewId != null && profileGroup.getCheckedRadioButtonId() != viewId) {
            profileGroup.check(viewId);
        }
        updateTimeZoneOptions();
        setOptionsEnabled(enabled);
        updateActiveProfile();
    }

    private void setOptionsEnabled(boolean enabled) {
        boolean editable = ModuleStatus.isActive() && !restartInProgress;
        enabledSwitch.setEnabled(editable);
        skipStartupLoginSwitch.setEnabled(editable);
        for (int index = 0; index < profileGroup.getChildCount(); index++) {
            View option = profileGroup.getChildAt(index);
            option.setEnabled(editable && enabled);
            option.setAlpha(editable && enabled ? 1f : 0.5f);
        }
        timeZoneRow.setEnabled(editable && enabled);
        timeZoneRow.setAlpha(editable && enabled ? 1f : 0.5f);
        applyButton.setEnabled(editable);
        clearButton.setEnabled(editable);
        applyButton.setAlpha(editable ? 1f : 0.4f);
        clearButton.setAlpha(editable ? 1f : 0.4f);
        boolean active = ModuleStatus.isActive();
        moduleStatusView.setText(active ? R.string.module_active : R.string.module_inactive);
        moduleStatusView.setTextColor(active
                ? Color.rgb(18, 128, 76) : Color.rgb(180, 35, 24));
        moduleStatusView.setBackground(rounded(active
                ? Color.rgb(235, 248, 240) : Color.rgb(254, 243, 242), 10));
    }

    private void updateActiveProfile() {
        RegionProfile selected = getSelectedProfile();
        boolean pending = !preferences.contains(ConfigContract.KEY_PROFILE_ID)
                || enabledSwitch.isChecked()
                != preferences.getBoolean(ConfigContract.KEY_ENABLED, true)
                || !selected.id.equals(preferences.getString(ConfigContract.KEY_PROFILE_ID, "US"))
                || !selected.timeZoneId.equals(RegionTimeZone.resolve(selected.id,
                        preferences.getString(ConfigContract.KEY_TIME_ZONE_ID, null)).timeZoneId)
                || skipStartupLoginSwitch.isChecked()
                != preferences.getBoolean(ConfigContract.KEY_SKIP_STARTUP_LOGIN, true);
        String state = getString(pending ? R.string.config_pending : R.string.config_saved);
        if (!enabledSwitch.isChecked()) {
            activeProfileView.setText(getString(R.string.active_follow_system, state));
            return;
        }
        RegionProfile profile = getSelectedProfile();
        RegionTimeZone timeZone = getSelectedTimeZone(profile.id);
        activeProfileView.setText(getString(R.string.active_profile, state,
                profile.displayName, profile.region, timeZone.displayName));
    }

    private void updateTimeZoneOptions() {
        RegionProfile profile = getSelectedBaseProfile();
        RegionTimeZone selected = getSelectedTimeZone(profile.id);
        timeZoneValue.setText(getString(R.string.time_zone_value, selected.displayName));
        timeZoneRow.setContentDescription(getString(R.string.time_zone_city)
                + "，" + selected.displayName);
    }

    private RegionTimeZone getSelectedTimeZone(String profileId) {
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

    private static final class PendingEditorState {
        final boolean enabled;
        final boolean skipStartupLogin;
        final String profileId;
        final String timeZoneId;

        PendingEditorState(
                boolean enabled,
                boolean skipStartupLogin,
                String profileId,
                String timeZoneId) {
            this.enabled = enabled;
            this.skipStartupLogin = skipStartupLogin;
            this.profileId = profileId;
            this.timeZoneId = timeZoneId;
        }
    }
}
