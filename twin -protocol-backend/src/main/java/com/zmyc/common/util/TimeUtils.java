package com.zmyc.common.util;

import java.time.LocalDate;
import java.time.ZoneId;

public class TimeUtils {

    public static final long ONE_DAY_SECONDS = 86400L;

    public static Long getTodayZeroTimestamp() {
        LocalDate today = LocalDate.now();
        return today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    }

    public static Long getYesterdayZeroTimestamp() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return yesterday.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    }

    public static Long getTodayEndTimestamp() {
        return getTodayZeroTimestamp() + ONE_DAY_SECONDS;
    }

    public static boolean isMoreThan48HoursAgo(Long lastReleaseDate) {
        long currentTime = System.currentTimeMillis() / 1000;
        return (currentTime - lastReleaseDate) > (2 * ONE_DAY_SECONDS);
    }

    public static long now() {
        return System.currentTimeMillis() / 1000;
    }

    public static Long getTodayEightAmTimestamp() {
        Long todayZeroTimestamp = getTodayZeroTimestamp();
        return todayZeroTimestamp + 8 * 3600;
    }

    public static Long getTodaTimestampPlusHours(Long hours) {
        Long todayZeroTimestamp = getTodayZeroTimestamp();
        return todayZeroTimestamp + hours * 3600;
    }
}
