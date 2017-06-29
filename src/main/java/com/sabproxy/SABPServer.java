package com.sabproxy;

import org.codehaus.plexus.util.FileUtils;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

@Controller
@EnableAutoConfiguration
@SpringBootApplication
public class SABPServer {
    private static Date startDate = new Date();
    private AdServers adServers;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${application.name}")
    private String app_name = "";

    @Value("${application.github.user}")
    private String github_user = "";

    @Value("${application.github.repo}")
    private String github_repo = "";

    @Value("${application.url}")
    private String app_url = "";

    @Value("${application.hosts.sources}")
    private String[] hostsSources;

    @Value("${application.port.proxy}")
    private String app_port_proxy = "";


    @GetMapping("/")
    public String index(Map<String, Object> model) {
        Map<String, Integer> topDomains = adServers.getBlockedDomainsHits();

        String topDomainsName = "";
        String topDomainsData = "";
        String randomColor = "";
        String randomColorHighLight = "";
        for (Map.Entry<String, Integer> entry : topDomains.entrySet()) {
            topDomainsName += "'" + entry.getKey() + "', ";
            topDomainsData += entry.getValue() + ", ";
            randomColor += "randomColorGenerator(), ";
            randomColorHighLight += "randomColorGenerator(), ";
        }

        int trafficAdsPercentage = 0;
        if (adServers.getSessionRequests() > 0) {
            trafficAdsPercentage = adServers.getSessionBlockedAds() * 100 / adServers.getSessionRequests();
        }

        // Ad Source table data
        int topXsources = 20;
        int c = 1;
        String currClass = "odd";
        String topDomainsTableData = "";
        for (Map.Entry<String, Integer> entry : topDomains.entrySet()) {
            topDomainsTableData += "<tr class=\"" + currClass + "\">\n";
            topDomainsTableData += "<td>" + c + "</td>\n";
            topDomainsTableData += "<td>" + entry.getKey() + "</td>\n";
            topDomainsTableData += "<td class=\"center\">" + entry.getValue() + "</td>\n";
            topDomainsTableData += "</tr>\n";

            if (currClass.equals("odd")) {
                currClass = "even";
            } else {
                currClass = "odd";
            }
            c++;
            if (c > topXsources) {
                break;
            }
        }

        model.put("topDomainsName", topDomainsName);
        model.put("topDomainsData", topDomainsData);
        model.put("randomColor", randomColor);
        model.put("randomColorHighLight", randomColorHighLight);
        model.put("trafficAdsPercentage", trafficAdsPercentage);
        model.put("trafficAdsRequests", adServers.getSessionBlockedAds());
        model.put("trafficRequests", adServers.getSessionRequests());
        model.put("uptime", Utils.dateDifference(startDate, new Date()));

        model.put("top20DomainsTableData", topDomainsTableData);

        model.put("app_name", this.app_name);
        model.put("application.github.user", this.github_user);
        model.put("application.github.repo", this.github_repo);
        model.put("application.url", this.app_url);

        return "index";
    }

    @GetMapping("/update.html")
    public String update(Map<String, Object> model) {
        model.put("app_name", this.app_name);
        model.put("application.github.user", this.github_user);
        model.put("application.github.repo", this.github_repo);
        model.put("application.url", this.app_url);

        String app_version = this.getClass().getPackage().getImplementationVersion().trim();
        model.put("application.version", app_version);

        Updater updater = new Updater();
        String latestVersion = updater.getLatestVersion();
        if (latestVersion.contains("ERROR")) {
            latestVersion = "<p class=\"text-danger\">" + latestVersion + "</p>";
        } else if (latestVersion.equals(app_version)) {
            latestVersion = "<p class=\"text-info\">No updates available.</p>";
        } else {
            latestVersion = "<p>Latest version:&nbsp;&nbsp;&nbsp; " + latestVersion + "</p>";
            //latestVersion += "<p class=\"text-info\"> <button type=\"button\" class=\"btn btn-primary\">Update Now</button> </p>";
            if (updater.upgradable()) {
                latestVersion += "<p class=\"text-info\"> <button class=\"btn btn-primary btn-block updatebutton\">Update Now</button> </p>";
            }
        }
        model.put("application.latestVersion", latestVersion);

        return "update";
    }

    @GetMapping("/upgrade.html")
    public String upgrade(Map<String, Object> model) throws IOException {
        Updater updater = new Updater();
        String runningVersion = this.getClass().getPackage().getImplementationVersion().trim();
        String latestVersion = updater.getLatestVersion();
        if (latestVersion.contains("ERROR")) {
            model.put("upgrade.info", "Can't upgrade!<br/>" + latestVersion);
        } else if (latestVersion.equals(runningVersion)) {
            model.put("upgrade.info", "Running latest version: " + latestVersion);
        } else if (updater.upgradable()) {
            model.put("upgrade.info", "SABProxy is upgrading and will restart automatically.<br/>This may take a while...");
            if (!FileUtils.fileExists(Updater.tempUpgradeFlagFile) && !FileUtils.fileExists(Updater.tempUpdateFailLogFile)) {
                Runnable upgradeSABProxy = () -> {
                    log.info("Upgrade started. This may take a while and SABProxy will restart automatically.");
                    updater.upgrade();
                };
                new Thread(upgradeSABProxy).start();
            } else if (FileUtils.fileExists(Updater.tempUpdateFailLogFile)) {
                model.put("upgrade.info", "Upgrade failed! <br/>Message: " + FileUtils.fileRead(Updater.tempUpdateFailLogFile));
            } else {
                model.put("upgrade.info", "Upgrade in progress. Please wait...");
            }
        }

        return "upgrade";
    }

    @GetMapping("/sysinfo.html")
    public String sysinfo(Map<String, Object> model) {
        model.put("app_name", this.app_name);
        model.put("application.github.user", this.github_user);
        model.put("application.github.repo", this.github_repo);
        model.put("application.url", this.app_url);

        SystemInfoUtil sysinfo = new SystemInfoUtil();
        model.put("systeminfo.os", sysinfo.getOS());
        model.put("systeminfo.processor", sysinfo.getProcessor());
        model.put("systeminfo.memory", sysinfo.getMemory().replace("\n", " | "));
        model.put("systeminfo.network.interfaces", sysinfo.getNetworkInterfaces().replace("\n", "<br/>"));
        model.put("systeminfo.network.parameters", sysinfo.getNetworkParameters().replace("\n", "<br/>"));
        model.put("systeminfo.sensors", sysinfo.getSensorsInfo().replace("\n", "<br/>"));

        return "sysinfo";
    }

    @GetMapping("/blocked-domains.html")
    public String blockedDomainsList(Map<String, Object> model) {
        model.put("app_name", this.app_name);
        model.put("application.github.user", this.github_user);
        model.put("application.github.repo", this.github_repo);
        model.put("application.url", this.app_url);

        model.put("blocked.domains.total", adServers.getNumberOfLoadedAdServers());
        String htmlHostsSources = "";
        String[] hostsSources = adServers.getHostsSources();
        for (int i = 0; i < hostsSources.length; i++) {
            htmlHostsSources += "<p><a target=\"_blank\" href=\"" + hostsSources[i] + "\"><em class=\"fa fa-external-link\"></em></a>&nbsp;" + hostsSources[i] + "</p>";
        }
        model.put("blocked.domains.sources", htmlHostsSources);
        model.put("blocked.domains.sources.number", hostsSources.length);

        return "blocked-domains";
    }


    @GetMapping("/login.html")
    public String login(Map<String, Object> model) {
        return "login";
    }

    @Bean
    public HttpProxyServer httpProxy() {
        log.info("Starting proxy on port: " + app_port_proxy);

        adServers = new AdServers(hostsSources);

        HttpProxyServer server =
                DefaultHttpProxyServer.bootstrap()
                        .withPort(Integer.valueOf(app_port_proxy))
                        .withAllowLocalOnly(false)
                        .withServerResolver(new SABProxyDNSResolver(adServers))
                        .withName("SABProxy")
                        .start();

        return server;
    }


    public static void main(String[] args) {
        SpringApplication.run(SABPServer.class, args);
    }

}
