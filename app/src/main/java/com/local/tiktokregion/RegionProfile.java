package com.local.tiktokregion;

public final class RegionProfile {
    private static final RegionProfile[] PROFILES = {
            new RegionProfile("US", "美国", "US", "us", "310260", "310", "260", "T-Mobile"),
            new RegionProfile("GB", "英国", "GB", "gb", "23430", "234", "30", "EE"),
            new RegionProfile("JP", "日本", "JP", "jp", "44010", "440", "10", "NTT DOCOMO"),
            new RegionProfile("KR", "韩国", "KR", "kr", "45008", "450", "08", "KT"),
            new RegionProfile("SG", "新加坡", "SG", "sg", "52501", "525", "01", "Singtel"),
            new RegionProfile("DE", "德国", "DE", "de", "26201", "262", "01", "Telekom.de"),
            new RegionProfile("FR", "法国", "FR", "fr", "20801", "208", "01", "Orange F"),
            new RegionProfile("CA", "加拿大", "CA", "ca", "302720", "302", "720", "Rogers"),
            new RegionProfile("AU", "澳大利亚", "AU", "au", "50501", "505", "01", "Telstra")
    };

    public final String id;
    public final String displayName;
    public final String region;
    public final String countryIso;
    public final String mccMnc;
    public final String mcc;
    public final String mnc;
    public final String carrier;

    public RegionProfile(
            String id,
            String displayName,
            String region,
            String countryIso,
            String mccMnc,
            String mcc,
            String mnc,
            String carrier) {
        this.id = id;
        this.displayName = displayName;
        this.region = region;
        this.countryIso = countryIso;
        this.mccMnc = mccMnc;
        this.mcc = mcc;
        this.mnc = mnc;
        this.carrier = carrier;
    }

    public static RegionProfile[] all() {
        return PROFILES.clone();
    }

    public static RegionProfile find(String id) {
        for (RegionProfile profile : PROFILES) {
            if (profile.id.equals(id)) {
                return profile;
            }
        }
        return PROFILES[0];
    }
}
