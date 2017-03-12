package com.pnunes.sabproxy;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {
    private static String appDirName = ".sabproxy";

    public static void initializeUserSettings() {
        // create app folder
        File directory = new File(String.valueOf(getAppSettingFolder()));
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    private static String getUserHomeDir() {
        return System.getProperty("user.home");
    }

    public static String getAppSettingFolder() {
        return getUserHomeDir() + "/" + appDirName;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public static String dateDifference(Date startDate, Date endDate) {
        long different = endDate.getTime() - startDate.getTime();
        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;

        String dateDiff = elapsedDays + " days, " + elapsedHours + "h:" + elapsedMinutes + "m:" + elapsedSeconds + "s";
        return dateDiff;
    }
}
