package com.pnunes.sabproxy;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class AdServers {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static String AD_SERVERS_FILE = "adservers.txt";
    private static String AD_SERVERS_SOURCE_URL = "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/fakenews-gambling/hosts";

    private String DOMAIN_PATTERN = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$";
    private Pattern domainPattern = Pattern.compile(DOMAIN_PATTERN);

    private List<String> adServers = new ArrayList<String>();
    private int sessionRequests = 0;
    private int sessionBlockedAds = 0;
    private HitCounter hitCounter = new HitCounter();

    public AdServers(){
        adServers = new ArrayList<String>();
    }

    public int numberOfLoadedAdServers(){
        return adServers.size();
    }

    public boolean contains(String domain){
        sessionRequests++;
        if(domain == null || domain.equals("")){
            return false;
        }else if(adServers.contains(domain)){
            sessionBlockedAds++;
            hitCounter.addHit(domain);
            return true;
        }

        return false;
    }

    public int getSessionBlockedAds(){
        return sessionBlockedAds;
    }

    public int getSessionRequests(){
        return sessionRequests;
    }

    public Map<String, Integer> getBlockedDomainsHits(){
        return hitCounter.getTopHits();
    }

    public void loadListFromHostsFileFormat(String adServersHostFile){
        adServers = new ArrayList<String>();
        LineIterator it = null;
        log.info("Loading Ad Server list from: "+adServersHostFile);
        try {
            it = FileUtils.lineIterator(new File(adServersHostFile), "UTF-8");

            while (it.hasNext()) {
                String line = it.nextLine();

                if(!getAdServerFromHostsLine(line).equals("")){
                    adServers.add(getAdServerFromHostsLine(line));
                }
            }
            log.info("Loaded "+adServers.size()+" ad servers.");
        }catch(Exception e){
            log.error("Failed to load ad servers from file "+adServersHostFile+". "+e.getMessage());
        }finally {
            LineIterator.closeQuietly(it);
        }
    }


    private String getAdServerFromHostsLine(String line){
        if(line.startsWith("#") || line.isEmpty()){
            return "";
        }

        // remove IPV4 from string
        String noIPLine = line.replaceAll("(\\d+.){3}\\d+", "").trim();

        if(domainPattern.matcher(noIPLine).find()){
            Matcher matcher = domainPattern.matcher(noIPLine);
            while(matcher.find()) {
                return matcher.group();
            }
        }

        return "";
    }

    public String getAdServersListFile(){
        return Utils.getAppSettingFolder()+"/"+AD_SERVERS_FILE;
    }

    public void updateAdServersList(){
        log.info("Trying to update ad servers list...");
        try {
            FileUtils.copyURLToFile(new URL(AD_SERVERS_SOURCE_URL), new File(getAdServersListFile()));
        } catch (MalformedURLException e) {
            log.warn("Can't update ads servers list from: "+AD_SERVERS_SOURCE_URL+". "+e.getMessage());
        } catch (IOException e) {
            log.warn("Failed to save ads servers list. "+e.getMessage());
        }
        log.info("Ad servers list updated.");
    }

}