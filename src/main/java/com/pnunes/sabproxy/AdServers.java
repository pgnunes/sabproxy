package com.pnunes.sabproxy;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdServers {
    public static String AD_SERVERS_FILE = "adservers.txt";
    public static int AD_SERVERS_FILE_VALID_DAYS = 1;
    private static String AD_SERVERS_SOURCE_URL = "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/fakenews-gambling/hosts";
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private String DOMAIN_PATTERN = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$";
    private Pattern domainPattern = Pattern.compile(DOMAIN_PATTERN);

    private List<String> adServers = new ArrayList<String>();
    private int sessionRequests = 0;
    private int sessionBlockedAds = 0;
    private HitCounter hitCounter = new HitCounter();

    public AdServers() {
        adServers = new ArrayList<String>();

        Utils.initializeUserSettings();
        updateAdServersList(false);
        loadListFromHostsFileFormat(getAdServersListFile());
    }

    public int numberOfLoadedAdServers() {
        return adServers.size();
    }

    public boolean contains(String host) {
        sessionRequests++;
        if (host == null || host.equals("")) {
            return false;
        } else if (adServers.contains(host)) {
            sessionBlockedAds++;
            hitCounter.addHit(host);
            return true;
        }
        return false;
    }

    public int getSessionBlockedAds() {
        return sessionBlockedAds;
    }

    public int getSessionRequests() {
        return sessionRequests;
    }

    public Map<String, Integer> getBlockedDomainsHits() {
        return hitCounter.getTopHits();
    }

    public void loadListFromHostsFileFormat(String adServersHostFile) {
        adServers = new ArrayList<String>();
        LineIterator it = null;
        log.info("Loading Ad Server list from: " + adServersHostFile);
        try {
            it = FileUtils.lineIterator(new File(adServersHostFile), "UTF-8");

            while (it.hasNext()) {
                String line = it.nextLine();

                if (!getAdServerFromHostsLine(line).equals("")) {
                    adServers.add(getAdServerFromHostsLine(line));
                }
            }
            log.info("Loaded " + adServers.size() + " ad servers.");
        } catch (Exception e) {
            log.error("Failed to load ad servers from file " + adServersHostFile + ". " + e.getMessage());
        } finally {
            LineIterator.closeQuietly(it);
        }
    }


    private String getAdServerFromHostsLine(String line) {
        if (line.startsWith("#") || line.isEmpty()) {
            return "";
        }

        // remove IPV4 from string
        String noIPLine = line.replaceAll("(\\d+.){3}\\d+", "").trim();

        if (domainPattern.matcher(noIPLine).find()) {
            Matcher matcher = domainPattern.matcher(noIPLine);
            while (matcher.find()) {
                return matcher.group();
            }
        }

        return "";
    }

    public String getAdServersListFile() {
        return Utils.getAppSettingFolder() + "/" + AD_SERVERS_FILE;
    }

    public void updateAdServersList(boolean forceUpdate) {
        log.info("Trying to update ad servers list...");

        // check if file exists
        File adServersHostFile = new File(getAdServersListFile());
        if (adServersHostFile.exists()) {
            // check if its older than x days
            long timeDiff = new Date().getTime() - adServersHostFile.lastModified();
            if (timeDiff > AD_SERVERS_FILE_VALID_DAYS * 24 * 60 * 60 * 1000 || forceUpdate) { // update file only if older than AD_SERVERS_FILE_VALID_DAYS
                if (downloadAdServersList()) {
                    log.info("Ad servers list successfully updated.");
                }
            } else {
                log.info("Ad servers list less than " + AD_SERVERS_FILE_VALID_DAYS + " day(s) old. Not updating.");
            }
        } else {
            if (downloadAdServersList()) {
                log.info("Ad servers list successfully updated.");
            }
        }
    }

    private boolean downloadAdServersList() {
        boolean downloadSuccess = true;
        try {
            FileUtils.copyURLToFile(new URL(AD_SERVERS_SOURCE_URL), new File(getAdServersListFile()));
        } catch (MalformedURLException e) {
            log.error("Can't update ads servers list from: " + AD_SERVERS_SOURCE_URL + ". " + e.getMessage());
            downloadSuccess = false;
        } catch (IOException e) {
            log.error("Failed to save ads servers list. " + e.getMessage());
            downloadSuccess = false;
        }

        return downloadSuccess;
    }


}