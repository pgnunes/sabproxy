package com.sabproxy;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SABPServerTest {

    private static String AD_HTTP_URL_TEST = "http://pubads.g.doubleclick.net";
    private static String AD_HTTPS_URL_TEST = "https://pagead2.googlesyndication.com/pagead/show_companion_ad.js";
    private static String IP_REQUEST_SABPROXY_COM = "http://72.14.188.14";

    @Value("${application.port.proxy}")
    private String app_port_proxy = "";

    private static String PROXY_ADDRESS = "127.0.0.1";

    private static String WEB_SERVER_STRING = "<title> SABProxy </title>";

    @LocalServerPort
    int port;

    @Test
    public void testHTTPAdBlock() {
        HttpHost proxy = new HttpHost(PROXY_ADDRESS, Integer.valueOf(app_port_proxy), "http");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String bodyResponse = "";
        int statusResponse = 0;

        HttpGet httpget = new HttpGet(AD_HTTP_URL_TEST);
        CloseableHttpResponse response = null;
        RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
        httpget.setConfig(config);
        try {
            response = httpclient.execute(httpget);
            bodyResponse = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
        }
        statusResponse = response.getStatusLine().getStatusCode();

        assertEquals(HttpStatus.BAD_GATEWAY.value(), statusResponse);
        assertEquals("Bad Gateway: /", bodyResponse);
    }

    @Test
    public void testHTTPAdBlockProxiedVsNonProxied() {
        HttpHost proxy = new HttpHost(PROXY_ADDRESS, Integer.valueOf(app_port_proxy), "http");
        String bodyResponse = "";
        String bodyResponseSABProxied = "";

        int statusResponse = 0;
        int statusResponseSABProxied = 0;

        CloseableHttpClient httpclient = HttpClients.createDefault();

        try {
            HttpGet httpget = new HttpGet(AD_HTTP_URL_TEST);
            CloseableHttpResponse response = httpclient.execute(httpget);
            bodyResponse = EntityUtils.toString(response.getEntity());
            statusResponse = response.getStatusLine().getStatusCode();

            RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
            httpget.setConfig(config);
            CloseableHttpResponse responseSABProxied = httpclient.execute(httpget);
            bodyResponseSABProxied = EntityUtils.toString(responseSABProxied.getEntity());
            statusResponseSABProxied = responseSABProxied.getStatusLine().getStatusCode();

        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean expectedResponse = false;
        if (bodyResponse.startsWith("<!doctype html><html lang=\"en\" ng-app=\"doubleclick\"")) {
            expectedResponse = true;
        }
        assertEquals(true, expectedResponse);
        assertEquals(200, statusResponse);

        assertEquals("Bad Gateway: /", bodyResponseSABProxied);
        assertEquals(502, statusResponseSABProxied);
    }

    @Test
    public void testHTTPSAdBlock() {
        HttpHost proxy = new HttpHost(PROXY_ADDRESS, Integer.valueOf(app_port_proxy), "http");
        int statusResponseSABProxied = 0;
        CloseableHttpClient httpclient = HttpClients.createDefault();

        try {
            HttpGet httpget = new HttpGet(AD_HTTPS_URL_TEST);
            RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
            httpget.setConfig(config);
            CloseableHttpResponse responseSABProxied = httpclient.execute(httpget);
            statusResponseSABProxied = responseSABProxied.getStatusLine().getStatusCode();

        } catch (IOException e) {
            //e.printStackTrace();
        }

        assertEquals(502, statusResponseSABProxied);

    }

    @Test
    public void testSABProxyStatsPage() {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        int statusResponse = 0;

        URI webServerURI = null;
        try {
            webServerURI = new URI("http", null, PROXY_ADDRESS, port, "/", "", "anchor");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        HttpGet httpget = new HttpGet(webServerURI);

        CloseableHttpResponse response = null;

        try {
            response = httpclient.execute(httpget);
        } catch (IOException e) {
            e.printStackTrace();
        }
        statusResponse = response.getStatusLine().getStatusCode();

        assertEquals(HttpStatus.OK.value(), statusResponse);
    }

    @Test
    public void testIPHandling() {
        HttpHost proxy = new HttpHost(PROXY_ADDRESS, Integer.valueOf(app_port_proxy), "http");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String bodyResponse = "";
        int statusResponse = 0;

        HttpGet httpget = new HttpGet(IP_REQUEST_SABPROXY_COM);
        CloseableHttpResponse response = null;
        RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
        httpget.setConfig(config);
        try {
            response = httpclient.execute(httpget);
            bodyResponse = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
        }
        statusResponse = response.getStatusLine().getStatusCode();

        assertEquals(HttpStatus.OK.value(), statusResponse);
        assertTrue(bodyResponse.contains("sabproxy"));
    }

}
