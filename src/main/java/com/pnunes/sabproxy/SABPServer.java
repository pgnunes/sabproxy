package com.pnunes.sabproxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
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

import java.net.ServerSocket;
import java.util.Date;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;

@RestController
@EnableAutoConfiguration
@SpringBootApplication
public class SABPServer {
    private static AdServers adServers = new AdServers();
    protected static int PROXY_PORT = 3129;
    protected static String PROXY_AD_BLOCK_TEXT = "SABProxy - Blocked AD";
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
        HttpProxyServer server =
                DefaultHttpProxyServer.bootstrap()
                        .withPort(PROXY_PORT)
                        .withFiltersSource(getAdFilter()).withAllowLocalOnly(false)
                        .start();
        return server;
    }


    private HttpFiltersSourceAdapter getAdFilter() {
        HttpFiltersSourceAdapter adFilter = new HttpFiltersSourceAdapter() {
            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                return new HttpFiltersAdapter(originalRequest) {
                    @Override
                    public HttpResponse clientToProxyRequest(HttpObject httpObject) {

                        String httpReqDomain = getDomain(originalRequest.getUri().toString());
                        if (adServers.contains(httpReqDomain)) {
                            // HTTPS initiating CONNECT request (no more visibility from this point on - drop it)
                            if (originalRequest.getMethod() == HttpMethod.CONNECT) {
                                try {
                                    ctx.close();
                                } catch (Exception e) {
                                    // Handler intentionally closed
                                }
                            }

                            // HTTP
                            log.info("[" + adServers.getSessionBlockedAds() + "] Blocking Ad from: " + httpReqDomain);
                            ByteBuf buffer = Unpooled.wrappedBuffer(PROXY_AD_BLOCK_TEXT.getBytes());
                            HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
                            response.headers().set(CONNECTION, "close");

                            return response;
                        }
                        return null;
                    }

                };
            }

        };

        return adFilter;
    }


    private String getDomain(String url) {
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

    public ServerSocket test() {
        // hardcoded just to test...
        String proxyHost = "188.92.214.253";
        String proxyPort = "8080";
        int proxyLocalPort = PROXY_PORT;

        ServerSocket server = null;
        Runnable serverTask = new Runnable() {
            @SuppressWarnings("resource")
            @Override
            public void run() {
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

}
