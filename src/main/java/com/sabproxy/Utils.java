package com.sabproxy;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    private static String appDirName = ".sabproxy";
    private static String currentPublicVersionURL = "http://sabproxy.com/release/latest.txt";
    private static String sabproxyUserAgent = "SABProxy-UA/" + Utils.class.getPackage().getImplementationVersion();
    public static String UPDATE_ERROR = "Error";
    public static String UPDATE_NEW_VERSION = "New version available.";
    public static String UPDATE_UPDATED = "Updated";

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

        String diff = "";
        if (elapsedDays > 0) {
            diff += elapsedDays + " day(s) ";
        }
        if (elapsedHours > 0) {
            diff += elapsedHours + "h:";
        }
        if (elapsedMinutes > 0) {
            diff += elapsedMinutes + "m:";
        }

        diff += elapsedSeconds + "s";

        return diff;
    }

    public static String getDomain(String url) {
        if (url == null || url.length() == 0)
            return "";

        int doubleslash = url.indexOf("//");
        if (doubleslash == -1)
            doubleslash = 0;
        else
            doubleslash += 2;

        int end = url.indexOf('/', doubleslash);
        end = end >= 0 ? end : url.length();

        int port = url.indexOf(':', doubleslash);
        end = (port > 0 && port < end) ? port : end;

        return url.substring(doubleslash, end);
    }

    public static String getLatestVersion() {
        CloseableHttpClient httpclient = HttpClients.custom()
                .setUserAgent(sabproxyUserAgent)
                .build();
        CloseableHttpResponse response = null;
        HttpEntity entity = null;
        HttpGet httpGet = new HttpGet(currentPublicVersionURL);
        String version = "";
        try {
            response = httpclient.execute(httpGet);
            entity = response.getEntity();
            version = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(entity);
            response.close();
        } catch (IOException e) {
            version = "ERROR - Failed to check latest version: <br> <i>" + e.getMessage() + "</i>";
            log.error(version);
        }

        return version;
    }


    public static String updateCheck(String currVersion) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpGet httpget = new HttpGet(currentPublicVersionURL);
            log.info("Checking for updates...");

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                @Override
                public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status checking for updates: " + status);
                    }
                }

            };
            String responseBody = null;

            try {
                responseBody = httpclient.execute(httpget, responseHandler);
            } catch (IOException e) {
                log.error("Failed to check for updates: " + e.getMessage());
                return UPDATE_ERROR;
            }

            // simple string match. No proper check as it's not needed...
            if (!responseBody.equals(currVersion)) {
                return UPDATE_NEW_VERSION;
            } else {
                return UPDATE_UPDATED;
            }

        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                log.error("Failed to close connection checking for updates. " + e.getMessage());
            }
        }
    }
}
