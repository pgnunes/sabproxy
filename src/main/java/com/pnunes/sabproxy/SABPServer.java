package com.pnunes.sabproxy;

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

        String topDomainsText = "<strong>Top Ad Domains</strong><br>";
        for (Map.Entry<String, Integer> entry : topDomains.entrySet()) {
            topDomainsText += entry.getValue() + " " + entry.getKey() + "<br>";
        }

        int trafficAdsPercentage = 0;
        if (adServers.getSessionRequests() > 0) {
            trafficAdsPercentage = adServers.getSessionBlockedAds() * 100 / adServers.getSessionRequests();
        }

        return "<h1>SABProxy - Simple Ad Block Proxy</h1>" +
                "<h2>Session Stats</h2>" +
                "<p>" +
                "<small>" +
                "<strong>Up Time: </strong>" + Utils.dateDifference(startDate, new Date()) + "<br><br>" +
                "<strong>Traffic (" + trafficAdsPercentage + "% Ads)</strong><br>" +
                "Requests:&nbsp;&nbsp;&nbsp; " + adServers.getSessionRequests() + "<br>" +
                "Blocked Ads: " + adServers.getSessionBlockedAds() + "<br>" +
                "<br>" + topDomainsText +
                "<br>" +
                "</small>" +
                "</p>";
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
