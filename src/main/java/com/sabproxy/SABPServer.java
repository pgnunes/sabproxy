package com.sabproxy;

import com.sabproxy.util.AdServers;
import com.sabproxy.util.SystemInfoUtil;
import com.sabproxy.util.Updater;
import com.sabproxy.util.Utils;
import org.codehaus.plexus.util.FileUtils;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

@SpringBootApplication
public class SABPServer {
    private static Date startDate = new Date();
    private static AdServers adServers;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    @Value("${application.name}")
    private String app_name = "";

    @Value("${application.url}")
    private String app_url = "";

    @Value("${application.github.user}")
    private String github_user = "";

    @Value("${application.github.repo}")
    private String github_repo = "";

    @Value("${application.hosts.sources}")
    private String[] hostsSources;

    @Value("${application.port.proxy}")
    private String app_port_proxy = "";

    public static void main(String[] args) {
        SpringApplication.run(SABPServer.class, args);
    }

    public static AdServers getAdServers(){
        return adServers;
    }

    @Controller
    @RequestMapping("/")
    class HomeController{
        @RequestMapping(method = RequestMethod.GET)
        ModelAndView
        index(){
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
            if(topDomains.isEmpty()){
                topDomainsTableData = "No data yet.";
            }else {
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
            }

            ModelAndView mav = new ModelAndView("index");
            mav.addObject("topDomainsName", topDomainsName);
            mav.addObject("topDomainsData", topDomainsData);
            mav.addObject("randomColor", randomColor);
            mav.addObject("randomColorHighLight", randomColorHighLight);
            mav.addObject("trafficAdsPercentage", trafficAdsPercentage);
            mav.addObject("trafficAdsRequests", adServers.getSessionBlockedAds());
            mav.addObject("trafficRequests", adServers.getSessionRequests());
            mav.addObject("uptime", Utils.dateDifference(startDate, new Date()));
            mav.addObject("top20DomainsTableData", topDomainsTableData);
            mav.addObject("app_name", app_name);
            mav.addObject("sidebar_links", getSideBarLinks("index"));
            mav.addObject("footer", getFooter());

            return mav;
        }
    }

    @Controller
    @RequestMapping("/sysinfo.html")
    class SysInfo{
        @RequestMapping(method = RequestMethod.GET)
        ModelAndView
        sysinfo(){
            SystemInfoUtil sysinfo = new SystemInfoUtil();

            ModelAndView mav = new ModelAndView("sysinfo");
            mav.addObject("systeminfo_os", sysinfo.getOS());
            mav.addObject("systeminfo_processor", sysinfo.getProcessor());
            mav.addObject("systeminfo_memory", sysinfo.getMemory().replace("\n", " | "));
            mav.addObject("systeminfo_network_interfaces", sysinfo.getNetworkInterfaces().replace("\n", "<br/>"));
            mav.addObject("systeminfo_network_parameters", sysinfo.getNetworkParameters().replace("\n", "<br/>"));
            mav.addObject("systeminfo_sensors", sysinfo.getSensorsInfo().replace("\n", "<br/>"));
            mav.addObject("sidebar_links", getSideBarLinks("sysinfo"));
            mav.addObject("app_name", app_name);
            mav.addObject("footer", getFooter());

            return mav;
        }
    }

    @Controller
    @RequestMapping("/blockeddomains.html")
    class BlockedDomainsController{
        @RequestMapping(method = RequestMethod.GET)
        ModelAndView
        blockeddomains(){
            String htmlHostsSources = "";
            String[] hostsSources = adServers.getHostsSources();
            for (int i = 0; i < hostsSources.length; i++) {
                htmlHostsSources += "<p><a target=\"_blank\" href=\"" + hostsSources[i] + "\"><em class=\"fa fa-external-link\"></em></a>&nbsp;" + hostsSources[i] + "</p>";
            }

            ModelAndView mav = new ModelAndView("blockeddomains");
            mav.addObject("blocked_domains_total", adServers.getNumberOfLoadedAdServers());
            mav.addObject("blocked_domains_sources", htmlHostsSources);
            mav.addObject("blocked_domains_sources_number", hostsSources.length);
            mav.addObject("blocked_domains_lastupdated", adServers.getLastUpdated());
            mav.addObject("sidebar_links", getSideBarLinks("blockeddomains"));
            mav.addObject("app_name", app_name);
            mav.addObject("footer", getFooter());

            return mav;
        }
    }

    @Controller
    @RequestMapping("/login.html")
    class MainController{
        @RequestMapping(method = RequestMethod.GET)
        ModelAndView
        login(){
            String app_version = "0.0.0";
            try{
                app_version = this.getClass().getPackage().getImplementationVersion().trim();
            }catch(Exception e){
                log.warn("Failed to get app version from jar...");
            }

            ModelAndView mav = new ModelAndView("login");
            mav.addObject("version", app_version);
            return mav;
        }
    }

    public String getFooter(){
        String footer = "<div class=\"footer-block buttons\">\n" +
                "                <iframe class=\"footer-github-btn\"\n" +
                "                        src=\"https://ghbtns.com/github-btn.html?user="+github_user+"&repo="+github_repo+"&type=star&count=true\"\n" +
                "                        frameborder=\"0\" scrolling=\"0\" width=\"140px\" height=\"20px\"></iframe>\n" +
                "                <iframe class=\"footer-github-btn\"\n" +
                "                        src=\"https://ghbtns.com/github-btn.html?user="+github_user+"&repo="+github_repo+"&type=watch&count=true&v=2\"\n" +
                "                        frameborder=\"0\" scrolling=\"0\" width=\"170px\" height=\"20px\"></iframe>\n" +
                "            </div>"+
                "<div class=\"footer-block author\">\n" +
                "                <ul>\n" +
                "                    <li>\n" +
                "                        <small>theme <a href=\"https://github.com/modularcode\">ModularCode</a></small>\n" +
                "                    </li>\n" +
                "                    <li>\n" +
                "                        <small><a href="+app_url+">"+app_name+"</a></small>\n" +
                "                    </li>\n" +
                "                </ul>\n" +
                "            </div>";
        return footer;
    }

    public String getSideBarLinks(String active){
        String links = "<ul class=\"nav metismenu\" id=\"sidebar-menu\">\n";
        if(active.equalsIgnoreCase("index")){
            links += "  <li class=\"active\">\n";
        }else{
            links += "  <li>\n";
        }

        links += "    <a href=\"/\"> <i class=\"fa fa-home\"></i> Dashboard </a>\n" +
                "  </li>\n" +
                "\n";

        if(active.equalsIgnoreCase("blockeddomains")){
            links += "  <li class=\"active\">\n";
        }else{
            links += "  <li>\n";
        }
        links += "    <a href=\"blockeddomains.html\"> <i class=\"fa fa-crosshairs\"></i> Blocked Domains </a>\n" +
                "  </li>\n";

        if(active.equalsIgnoreCase("sysinfo")){
            links += "  <li class=\"active\">\n";
        }else{
            links += "  <li>\n";
        }
        links += "    <a href=\"sysinfo.html\"> <i class=\"fa fa-info\"></i> System Information </a>\n" +
                "  </li>\n" +
                "  <li>\n" +
                "    <a href=\"logout\"> <i class=\"fa fa-sign-out\"></i> Logout </a>\n" +
                "  </li>\n" +
                "</ul>";
        return links;
    }

    @Bean
    public HttpProxyServer httpProxy() {
        log.info("Starting proxy on port: " + app_port_proxy);

        Utils.initializeUserSettings();
        SABPUser sabpUser = new SABPUser();
        sabpUser.initializeUser();
        adServers = new AdServers(hostsSources);

        HttpProxyServer server =
                DefaultHttpProxyServer.bootstrap()
                        .withPort(Integer.valueOf(app_port_proxy))
                        .withAllowLocalOnly(false)
                        .withTransparent(true)
                        .withServerResolver(new SABProxyDNSResolver(adServers))
                        .withName("SABProxy")
                        .start();

        return server;
    }

}
