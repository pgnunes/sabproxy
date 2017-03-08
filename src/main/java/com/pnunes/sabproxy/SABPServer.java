package com.pnunes.sabproxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
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

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.Values.*;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@RestController
@EnableAutoConfiguration
@SpringBootApplication
public class SABPServer{
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static AdServers adServers = null;
    private static int PROXY_PORT = 3129;
    private static Date startDate = new Date();

    public static void main(String[] args) {
        SpringApplication.run(SABPServer.class, args);
        adServers = new AdServers();
        Utils.initializeUserSettings();
        adServers.updateAdServersList(false);
        adServers.loadListFromHostsFileFormat(adServers.getAdServersListFile());
    }

    @RequestMapping("/")
    public String index() {
        Map<String, Integer> topDomains = adServers.getBlockedDomainsHits();

        String topDomainsText = "<strong>Top Ad Domains</strong><br>";
        for (Map.Entry<String, Integer> entry : topDomains.entrySet()) {
            topDomainsText += entry.getValue()+" "+entry.getKey()+"<br>";
        }

        int trafficAdsPercentage = 0;
        if(adServers.getSessionRequests() > 0) {
            trafficAdsPercentage = adServers.getSessionBlockedAds() * 100 / adServers.getSessionRequests();
        }

        return "<h1>SABProxy - Simple Ad Block Proxy</h1>" +
                "<h2>Session Stats</h2>" +
                "<p>" +
                "<small>" +
                "<strong>Up Time: </strong>" +Utils.dateDifference(startDate, new Date())+"<br><br>" +
                "<strong>Traffic ("+trafficAdsPercentage+"% Ads)</strong><br>" +
                "Requests:&nbsp;&nbsp;&nbsp; "+adServers.getSessionRequests()+"<br>" +
                "Blocked Ads: "+adServers.getSessionBlockedAds()+"<br>" +
                "<br>" + topDomainsText +
                "<br>" +
                "</small>" +
                "</p>";
    }

    @Bean
    public HttpProxyServer httpProxy(){
        HttpProxyServer server =
                DefaultHttpProxyServer.bootstrap()
                        .withPort(PROXY_PORT)
                        .withFiltersSource(new HttpFiltersSourceAdapter() {
                            boolean isAdRequest = false;

                            public String getDomain(String url) {
                                if(url == null || url.length() == 0)
                                    return "";

                                int doubleslash = url.indexOf("//");
                                if(doubleslash == -1)
                                    doubleslash = 0;
                                else
                                    doubleslash += 2;

                                int end = url.indexOf('/', doubleslash);
                                end = end >= 0 ? end : url.length();

                                int port = url.indexOf(':', doubleslash);
                                end = (port > 0 && port < end) ? port : end;

                                return url.substring(doubleslash, end);
                            }

                            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                                isAdRequest = false;
                                String httpReqDomain = getDomain(originalRequest.getUri().toString());

                                if(adServers.contains(httpReqDomain)){
                                    log.info("["+adServers.getSessionBlockedAds()+"] Blocking Ad from: "+httpReqDomain);
                                    isAdRequest = true;
                                }

                                return new HttpFiltersAdapter(originalRequest) {
                                    @Override
                                    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                                        if(isAdRequest){ // close the connection
                                            /**
                                             ByteBuf buffer = Unpooled.wrappedBuffer("".getBytes());
                                             HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
                                             HttpHeaders.setHeader(response, Names.CONNECTION, Values.CLOSE);

                                             return response;
                                             */

                                            if(originalRequest.getUri().contains(":443")){
                                                HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
                                                response.headers().set(CONNECTION, CLOSE);
                                                return response;
                                            }

                                            //HttpResponse response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND);
                                            String textResponse = "SABProxy - Blocked AD";
                                            ByteBuf buffer = Unpooled.wrappedBuffer(textResponse.getBytes());
                                            HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
                                            response.headers().set(CONNECTION, CLOSE);
                                            response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
                                            response.headers().set(CONTENT_LENGTH, textResponse.getBytes().length);
                                            //response.headers().set(CACHE_CONTROL, NO_CACHE);
                                            response.headers().set(CACHE_CONTROL, "max-age=0");
                                            response.headers().set(VIA, httpReqDomain);
                                            return response;
                                        }

                                        // business as usual
                                        return null;
                                    }

                                    @Override
                                    public HttpObject serverToProxyResponse(HttpObject httpObject) {
                                        return httpObject;
                                    }
                                };
                            }
                        }).withAllowLocalOnly(false)
                        .start();
        return server;
    }


    public ServerSocket test(){
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
                    log.info("SABProxy starting on port: "+localport);
                    server = new ServerSocket(localport);
                    while (true) {
                        new ThreadProxy(server.accept(), host, remoteport, adServers);
                    }
                } catch (Exception e) {
                    log.error("Failed to create proxy socket listener: "+e.getMessage());
                }
            }
        };
        Thread serverThread = new Thread(serverTask);
        serverThread.start();

        return server;
    }

}
