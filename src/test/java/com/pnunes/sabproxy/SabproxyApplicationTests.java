package com.pnunes.sabproxy;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SabproxyApplicationTests {

    private static String AD_HTTP_URL_TEST = "http://pubads.g.doubleclick.net";
    private static String AD_HTTPS_URL_TEST = "https://pagead2.googlesyndication.com/pagead/show_companion_ad.js";

    private static int PROXY_PORT = SABPServer.PROXY_PORT;
    private static String PROXY_ADDRESS = "127.0.0.1";

    private static HttpHost proxy = new HttpHost(PROXY_ADDRESS, PROXY_PORT, "http");

    @Test
    public void testHTTPAdBlock() {
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

        assertEquals(HttpStatus.OK.value(), statusResponse);
        assertEquals(SABPServer.PROXY_AD_BLOCK_TEXT, bodyResponse);
    }

    @Test
    public void testHTTPAdBlockProxiedVsNonProxied() {
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

        assertEquals(SABPServer.PROXY_AD_BLOCK_TEXT, bodyResponseSABProxied);
        assertEquals(200, statusResponseSABProxied);
    }

    @Test
    public void testHTTPSAdBlock() {
        String bodyResponseSABProxied = "";
        int statusResponseSABProxied = 0;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String exceptionReason = "Remote host closed connection during handshake";
        IOException exception = null;

        try {
            HttpGet httpget = new HttpGet(AD_HTTPS_URL_TEST);
            RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
            httpget.setConfig(config);
            CloseableHttpResponse responseSABProxied = httpclient.execute(httpget);
            bodyResponseSABProxied = EntityUtils.toString(responseSABProxied.getEntity());
            statusResponseSABProxied = responseSABProxied.getStatusLine().getStatusCode();

        } catch (IOException e) {
            //e.printStackTrace();
            exception = e;
        }

        boolean isExpectedResponse = false;
        if (bodyResponseSABProxied.startsWith("<!doctype html><html lang=\"en\" ng-app=\"doubleclick\"")) {
            isExpectedResponse = true;
        }

        assertEquals(0, statusResponseSABProxied);
        assertEquals(exceptionReason, exception.getMessage());

    }


}
