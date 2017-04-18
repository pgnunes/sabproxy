package com.sabproxy;

import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

@RestController
@EnableAutoConfiguration
@SpringBootApplication
public class SABPServer {
    protected static int PROXY_PORT = 3129;
    private static AdServers adServers = new AdServers();
    private static Date startDate = new Date();
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) {
        SpringApplication.run(SABPServer.class, args);
    }

    @RequestMapping("/")
    public String index() {
        Map<String, Integer> topDomains = adServers.getBlockedDomainsHits();

        String topDomainsText = "";
        String topDomainsName = "";
        String topDomainsData = "";
        String randomColor = "";
        String randomColorHighLight = "";
        for (Map.Entry<String, Integer> entry : topDomains.entrySet()) {
            topDomainsText += entry.getValue() + " " + entry.getKey() + "<br>";
            topDomainsName += "\"" + entry.getKey() + "\",\n";
            topDomainsData += entry.getValue() + ", ";
            randomColor += "randomColorGenerator(), ";
            randomColorHighLight += "randomColorGenerator(), ";
        }

        int trafficAdsPercentage = 0;
        if (adServers.getSessionRequests() > 0) {
            trafficAdsPercentage = adServers.getSessionBlockedAds() * 100 / adServers.getSessionRequests();
        }

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<title>SABProxy Stats</title>" +
                "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/jquery/3.2.1/jquery.min.js\"></script>\n" +
                "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.5.0/Chart.min.js\"></script>\n" +
                "</head>\n" +
                "<body>\n" +
                "<h1>SABProxy - Simple Ad Block Proxy</h1>\n" +
                "<h2>Session Stats</h2>\n" +
                "<small>\n" +
                "<strong>Up Time: </strong>" + Utils.dateDifference(startDate, new Date()) + "<br><br>\n" +
                "<strong>Traffic (" + trafficAdsPercentage + "% Ads)</strong><br>\n" +
                "Requests:&nbsp;&nbsp;&nbsp; " + adServers.getSessionRequests() + "<br>\n" +
                "Blocked Ads: " + adServers.getSessionBlockedAds() + "<br>\n" +

                "<br><strong>Top Ad Domains</strong><br>\n" +
//                "<div class=\"chart\" style=\"position: relative; height: 60vh;\">\n" +
                "<div class=\"chart\" style=\"float:left\">\n" +
                "   <canvas id=\"ads_domains\"></canvas>\n" +
                "</div>\n" +

                "<script>\n" +
                "   var randomColorGenerator = function () {\n" +
                "       return '#' + (Math.random().toString(16) + '0000000').slice(2, 8);\n" +
                "   };\n" +
                "   var data = {\n" +
                "       labels: [\n" +
                topDomainsName +
                "       ],\n" +
                "       datasets: [\n" +
                "           {\n" +
                "               data: [" + topDomainsData + "],\n" +
                "               backgroundColor: [\n" +
                randomColor +
                "               ],\n" +
                "               hoverBackgroundColor: [\n" +
                randomColorHighLight +
                "               ]\n" +
                "          }]\n" +
                "   };\n" +

                "   var options = {\n" +
                "       responsive: true,\n" +
                "       maintainAspectRatio: false,\n" +
                "       legend: {\n" +
                "           display: false\n" +
                "       },\n" +
                "       scaleBeginAtZero: true\n" +
                "   }\n" +
                "   var ctx = \"ads_domains\";\n" +
                "   var adDomainsPieChart = new Chart(ctx,{\n" +
                "       type: 'pie',\n" +
                "       data: data,\n" +
                "       options: options\n" +
                "   });\n" +
                "   document.getElementById(\"ads_domains\").style.height = '200px';\n" +

                "</script>\n" +

                "<br>\n" +
                "<div style=\"float: left; clear:left\">\n" +
                "<br>" + topDomainsText +
                "</div>" +
                "</small>\n" +
                "</body>\n" +
                "</html>";
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

    /**
     public ServerSocket test() {
     // hardcoded just to test...
     String proxyHost = "188.92.214.253";
     String proxyPort = "8080";
     int proxyLocalPort = PROXY_PORT;

     ServerSocket server = null;
     Runnable serverTask = new Runnable() {
    @SuppressWarnings("resource")
    @Override public void run() {
    try {
    ServerSocket server = null;
    String host = proxyHost;
    int remoteport = Integer.parseInt(proxyPort);
    int localport = proxyLocalPort;

    // Print a start-up message
    log.info("Connecting to proxy " + host + ":" + remoteport);
    log.info("SABProxy starting on port: " + localport);
    server = new ServerSocket(localport);
    while (true) {
    new ThreadProxy(server.accept(), host, remoteport, adServers);
    }
    } catch (Exception e) {
    log.error("Failed to create proxy socket listener: " + e.getMessage());
    }
    }
    };
     Thread serverThread = new Thread(serverTask);
     serverThread.start();

     return server;
     }
     */
}
