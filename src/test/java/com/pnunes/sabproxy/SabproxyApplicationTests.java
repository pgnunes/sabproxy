package com.pnunes.sabproxy;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SabproxyApplicationTests {

    private static String AD_HTTP_URL_TEST = "http://pubads.g.doubleclick.net";
    private static String AD_HTTPS_URL_TEST = "https://pagead2.googlesyndication.com/pagead/show_companion_ad.js";

    private static int PROXY_PORT = SABPServer.PROXY_PORT;
    private static String PROXY_ADDRESS = "127.0.0.1";
    private Proxy localTestProxy = null;

    @Test
    public void testHTTPAdBlock() {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_ADDRESS, PROXY_PORT));
        String bodyResponse = "";
        try {
            URLConnection conn = new URL(AD_HTTP_URL_TEST).openConnection(proxy);
            InputStream in = null;
            in = conn.getInputStream();
            String encoding = conn.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            bodyResponse = IOUtils.toString(in, encoding);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(SABPServer.PROXY_AD_BLOCK_TEXT, bodyResponse);
    }


    @Test
    public void testHTTPSAdBlock() {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_ADDRESS, PROXY_PORT));
        String bodyResponse = "";
        try {
            URLConnection conn = new URL(AD_HTTPS_URL_TEST).openConnection(proxy);
            InputStream in = null;
            in = conn.getInputStream();
            String encoding = conn.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            bodyResponse = IOUtils.toString(in, encoding);
        } catch (IOException e) {
            // Handler should be closed causing SSLHandshakeException
        }
        assertEquals("", bodyResponse);

    }


}
