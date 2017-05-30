package com.sabproxy;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Updater {
    private final Logger log = LoggerFactory.getLogger(Updater.class);
    private String currentPublicVersionURL = "http://sabproxy.com/release/latest.txt";
    private String installationDir = "/opt/sabproxy/";
    private String sabproxyUserAgent = "SABProxy-UA/" + Utils.class.getPackage().getImplementationVersion();
    private String sabproxyUpdaterUserAgent = "SABProxyUpdater-UA/" + Utils.class.getPackage().getImplementationVersion();
    public static String tempUpdateFailLogFile = System.getProperty("java.io.tmpdir") + "/" + "sabproxy-update.log";
    public static String tempUpgradeFlagFile = System.getProperty("java.io.tmpdir") + "/" + "sabproxy-upgrade.log";
    private String tempDistFile = System.getProperty("java.io.tmpdir") + "/" + "sabproxy.jar";

    public Updater() {

    }

    private RequestConfig requestConfig(int timeoutMillis) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(timeoutMillis)
                .setConnectTimeout(timeoutMillis)
                .setSocketTimeout(timeoutMillis)
                .build();
        return requestConfig;
    }

    public String getLatestDistJar() {
        CloseableHttpClient httpclient = HttpClients.custom()
                .setUserAgent(sabproxyUserAgent)
                .build();
        CloseableHttpResponse response = null;
        HttpEntity entity = null;
        HttpGet httpGet = new HttpGet(currentPublicVersionURL);
        httpGet.setConfig(requestConfig(10000));
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

        return version.trim();
    }

    public String getLatestVersion() {
        return getLatestDistJar().replace("sabproxy-", "").replace(".jar", "").trim();
    }

    public boolean upgradable() {
        HttpClient client = HttpClientBuilder.create().build();
        String latestVersion = getLatestVersion();
        if (latestVersion.length() > 0) {
            HttpResponse response = null;
            try {
                response = client.execute(new HttpGet(currentPublicVersionURL.replace("latest.txt", getLatestDistJar())));
            } catch (IOException e) {
                log.error("Failed to check latest version dist available: " + e.getMessage());
                return false;
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.error("Couldn't get latest version dist available (http status code: " + response.getStatusLine().getStatusCode() + ")");
                return false;
            }
            return true;
        }
        return false;
    }

    public void upgrade() {
        String downloadUpdateFile = currentPublicVersionURL.replace("latest.txt", getLatestDistJar());
        try {
            // remove temp file if already exists
            File updateFile = new File(tempDistFile);
            if (updateFile.exists()) {
                FileUtils.forceDelete(updateFile);
            }
            // remove temp log file if exists
            File updatelogFile = new File(tempUpdateFailLogFile);
            if (updatelogFile.exists()) {
                FileUtils.forceDelete(updatelogFile);
            }

            // remove temp log file if exists
            File upgradeFlagFile = new File(tempUpgradeFlagFile);
            if (upgradeFlagFile.exists()) {
                FileUtils.forceDelete(upgradeFlagFile);
            }
            FileUtils.fileWrite(tempUpgradeFlagFile, "Upgrade in progress. Downloading " + downloadUpdateFile);

            CloseableHttpClient client = HttpClients.custom()
                    .setUserAgent(sabproxyUpdaterUserAgent)
                    .build();
            HttpGet request = new HttpGet(downloadUpdateFile);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();

            FileOutputStream fos = new FileOutputStream(updateFile);

            int inByte;
            while ((inByte = is.read()) != -1) {
                fos.write(inByte);
            }
            is.close();
            fos.close();
            client.close();
            log.info("Download completed.");

            // replace the current executable (copyFileToDirectory will overwrite)
            log.info("Upgrading SABProxy...");
            FileUtils.copyFileToDirectory(tempDistFile, sabproxyUserAgent);

            // shutdown running instance - systemd will restart service using the updated version
            FileUtils.forceDelete(tempUpgradeFlagFile);
            log.info("Upgrade complete. Restarting...");
            System.exit(1);

        } catch (Exception e) {
            String error = "Failed upgrade: " + e.getMessage();
            log.error(error);
            try {
                FileUtils.forceDelete(tempUpgradeFlagFile);
                FileUtils.fileWrite(tempUpdateFailLogFile, error);
            } catch (IOException e1) {
                log.error("Failed to write upgrade log file: " + e1.getMessage());
            }
        }
    }
}
