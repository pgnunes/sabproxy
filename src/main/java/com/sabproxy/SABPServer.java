package com.sabproxy;

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

import java.util.Date;
import java.util.Map;

@Controller
@SpringBootApplication
public class SABPServer {
    protected static int PROXY_PORT = 3129;
    private static AdServers adServers = new AdServers();
    private static Date startDate = new Date();
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${application.name}")
    private String app_name = "";

    @Value("${application.github.user}")
    private String github_user = "";

    @Value("${application.github.repo}")
    private String github_repo = "";

    @Value("${application.url}")
    private String app_url = "";

    @GetMapping("/")
    public String index(Map<String, Object> model) {
        Map<String, Integer> topDomains = adServers.getBlockedDomainsHits();

        String topDomainsName = "";
        String topDomainsData = "";
        String randomColor = "";
        String randomColorHighLight = "";
        for (Map.Entry<String, Integer> entry : topDomains.entrySet()) {
            topDomainsName += "'"+entry.getKey() + "', ";
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
            topDomainsTableData +="<tr class=\""+currClass+"\">\n";
            topDomainsTableData +="<td>"+c+"</td>\n";
            topDomainsTableData +="<td>"+entry.getKey()+"</td>\n";
            topDomainsTableData +="<td class=\"center\">"+entry.getValue()+"</td>\n";
            topDomainsTableData +="</tr>\n";

            if(currClass.equals("odd")){
                currClass = "even";
            }else{
                currClass = "odd";
            }
            c++;
            if(c > topXsources){
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


    @Bean
    public HttpProxyServer httpProxy() {
        log.info("Starting proxy on port: " + PROXY_PORT);
        HttpProxyServer server =
                DefaultHttpProxyServer.bootstrap()
                        .withPort(PROXY_PORT)
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
