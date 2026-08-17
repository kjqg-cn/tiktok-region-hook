package com.local.tiktokregion;

public final class RegionTimeZone {
    private static final RegionTimeZone[] US = {
            new RegionTimeZone("洛杉矶（太平洋时间）", "America/Los_Angeles"),
            new RegionTimeZone("纽约（东部时间）", "America/New_York"),
            new RegionTimeZone("芝加哥（中部时间）", "America/Chicago"),
            new RegionTimeZone("丹佛（山地时间）", "America/Denver")
    };
    private static final RegionTimeZone[] GB = {
            new RegionTimeZone("伦敦", "Europe/London")
    };
    private static final RegionTimeZone[] JP = {
            new RegionTimeZone("东京", "Asia/Tokyo")
    };
    private static final RegionTimeZone[] KR = {
            new RegionTimeZone("首尔", "Asia/Seoul")
    };
    private static final RegionTimeZone[] SG = {
            new RegionTimeZone("新加坡", "Asia/Singapore")
    };
    private static final RegionTimeZone[] DE = {
            new RegionTimeZone("柏林", "Europe/Berlin")
    };
    private static final RegionTimeZone[] FR = {
            new RegionTimeZone("巴黎", "Europe/Paris")
    };
    private static final RegionTimeZone[] CA = {
            new RegionTimeZone("多伦多（东部时间）", "America/Toronto"),
            new RegionTimeZone("温哥华（太平洋时间）", "America/Vancouver"),
            new RegionTimeZone("卡尔加里（山地时间）", "America/Edmonton")
    };
    private static final RegionTimeZone[] AU = {
            new RegionTimeZone("悉尼（东部时间）", "Australia/Sydney"),
            new RegionTimeZone("布里斯班（东部时间）", "Australia/Brisbane"),
            new RegionTimeZone("阿德莱德（中部时间）", "Australia/Adelaide"),
            new RegionTimeZone("珀斯（西部时间）", "Australia/Perth")
    };

    public final String displayName;
    public final String timeZoneId;

    private RegionTimeZone(String displayName, String timeZoneId) {
        this.displayName = displayName;
        this.timeZoneId = timeZoneId;
    }

    public static RegionTimeZone[] all(String profileId) {
        return source(profileId).clone();
    }

    public static RegionTimeZone resolve(String profileId, String timeZoneId) {
        RegionTimeZone[] choices = source(profileId);
        if (timeZoneId != null) {
            for (RegionTimeZone choice : choices) {
                if (choice.timeZoneId.equals(timeZoneId)) {
                    return choice;
                }
            }
        }
        return choices[0];
    }

    private static RegionTimeZone[] source(String profileId) {
        if ("GB".equals(profileId)) {
            return GB;
        }
        if ("JP".equals(profileId)) {
            return JP;
        }
        if ("KR".equals(profileId)) {
            return KR;
        }
        if ("SG".equals(profileId)) {
            return SG;
        }
        if ("DE".equals(profileId)) {
            return DE;
        }
        if ("FR".equals(profileId)) {
            return FR;
        }
        if ("CA".equals(profileId)) {
            return CA;
        }
        if ("AU".equals(profileId)) {
            return AU;
        }
        return US;
    }
}
