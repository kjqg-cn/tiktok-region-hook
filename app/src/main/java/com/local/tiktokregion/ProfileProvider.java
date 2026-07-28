package com.local.tiktokregion;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public final class ProfileProvider extends ContentProvider {
    private static final String[] COLUMNS = {
            ConfigContract.COLUMN_ENABLED,
            ConfigContract.COLUMN_PROFILE_ID,
            ConfigContract.COLUMN_REGION,
            ConfigContract.COLUMN_COUNTRY_ISO,
            ConfigContract.COLUMN_MCC_MNC,
            ConfigContract.COLUMN_MCC,
            ConfigContract.COLUMN_MNC,
            ConfigContract.COLUMN_CARRIER,
            ConfigContract.COLUMN_SKIP_STARTUP_LOGIN
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        SharedPreferences preferences = getAttachedContext().getSharedPreferences(
                ConfigContract.PREFERENCES,
                0);
        boolean enabled = preferences.getBoolean(ConfigContract.KEY_ENABLED, true);
        boolean skipStartupLogin = preferences.getBoolean(
                ConfigContract.KEY_SKIP_STARTUP_LOGIN,
                true);
        RegionProfile profile = RegionProfile.find(preferences.getString(
                ConfigContract.KEY_PROFILE_ID,
                "US"));

        MatrixCursor cursor = new MatrixCursor(COLUMNS, 1);
        cursor.addRow(new Object[]{
                enabled ? 1 : 0,
                profile.id,
                profile.region,
                profile.countryIso,
                profile.mccMnc,
                profile.mcc,
                profile.mnc,
                profile.carrier,
                skipStartupLogin ? 1 : 0
        });
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.com.local.tiktokregion.profile";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    @Override
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    private android.content.Context getAttachedContext() {
        android.content.Context context = getContext();
        if (context == null) {
            throw new IllegalStateException("Provider is not attached");
        }
        return context;
    }
}
