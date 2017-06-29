package com.sabproxy.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdServers {
    public static String AD_SERVERS_FILE = "adservers.txt";
    public static int AD_SERVERS_FILE_VALID_DAYS = 1;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private String DOMAIN_PATTERN = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$";
    private Pattern domainPattern = Pattern.compile(DOMAIN_PATTERN);
    private Collection<String> adServers = new HashSet<>();
    private int sessionRequests = 0;
    private int sessionBlockedAds = 0;
    private HitCounter hitCounter = new HitCounter();
    private String[] hostsSources;

    public AdServers(String[] hostsSources) {
        this.hostsSources = hostsSources;
        adServers = new ArrayList<String>();

        updateAdServersList(false);
        loadListFromHostsFileFormat(getAdServersListFile());
    }

    public int getNumberOfLoadedAdServers() {
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
        if (hitCounter.getTopHits().isEmpty()) {
            HitCounter emptyHitCounter = new HitCounter();
            emptyHitCounter.addHit("No data");
            return emptyHitCounter.getTopHits();
        }

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
        String property = "java.io.tmpdir";
        String tempDir = System.getProperty(property);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        boolean downloadSuccess = false; // will be false if only ALL downloads fail
        try {
            FileUtils.touch(new File(getAdServersListFile()));
        } catch (IOException e) {
            log.error("Failed to create ad hosts file " + getAdServersListFile() + ". " + e.getMessage());
        }
        // hostsSources
        File tempDownloadFile;
        File tempHostsFile = new File(tempDir + "/" + RandomStringUtils.random(10));

        for (int i = 0; i < hostsSources.length; i++) {
            tempDownloadFile = new File(tempDir + "/" + RandomStringUtils.random(10));
            try {
                HttpGet httpGet = new HttpGet(hostsSources[i]);
                // keep sites like adaway happy as they return a 403 if no user agent
                httpGet.addHeader("SABPUser-Agent", "Mozilla/5.0 (Windows Me; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.96 Safari/537.36");
                httpGet.addHeader("Referer", "http://sabproxy.com");
                CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity ent = httpResponse.getEntity();
                FileUtils.copyInputStreamToFile(ent.getContent(), tempDownloadFile);
                httpGet.releaseConnection();

                downloadSuccess = true;
                log.info("Downloaded list from: " + hostsSources[i]);
                // merge into temp hosts file
                FileUtils.writeByteArrayToFile(tempHostsFile, FileUtils.readFileToByteArray(tempDownloadFile), true);
                tempDownloadFile.delete();
            } catch (IOException e) {
                log.error("Failed to save ads servers list from: " + hostsSources[i] + ". Reason: " + e.getMessage());
            }
        }

        // create/update local sabproxy hosts file
        try {
            FileUtils.writeByteArrayToFile(new File(getAdServersListFile()), FileUtils.readFileToByteArray(tempHostsFile), false);
        } catch (Exception e) {
            log.error("Failed to save write ads servers list! " + e.getMessage());
            downloadSuccess = false;
        } finally {
            // cleanup
            tempHostsFile.delete();
        }

        return downloadSuccess;
    }

    public String[] getHostsSources() {
        return hostsSources;
    }


}