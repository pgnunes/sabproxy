package com.pnunes.sabproxy;

import java.net.ServerSocket;

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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

@RestController
@EnableAutoConfiguration
@SpringBootApplication
public class SABPServer{
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private static AdServers adServers = null;
	private static int PROXY_PORT = 3129;
	
	public static void main(String[] args) {
		SpringApplication.run(SABPServer.class, args);
		adServers = new AdServers();
		Utils.initializeUserSettings();
		adServers.updateAdServersList();
		adServers.loadListFromHostsFileFormat(adServers.getAdServersListFile());
	}

	@RequestMapping("/")
	public String index() {
		return "Greetings from SABProxy!";
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
							log.info("Blocking Ad from: "+httpReqDomain);
							isAdRequest = true;
						}

						return new HttpFiltersAdapter(originalRequest) {                	
							@Override
							public HttpResponse clientToProxyRequest(HttpObject httpObject) {
								if(isAdRequest){ // close the connection
									ByteBuf buffer = Unpooled.wrappedBuffer("".getBytes());
							        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
							        HttpHeaders.setHeader(response, Names.CONNECTION, Values.CLOSE);
							        
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
