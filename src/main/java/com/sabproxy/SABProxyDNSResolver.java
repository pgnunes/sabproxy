package com.sabproxy;

import com.sabproxy.util.AdServers;
import org.littleshoot.proxy.HostResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class SABProxyDNSResolver implements HostResolver {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private AdServers adServers = null;

    public SABProxyDNSResolver(AdServers adServers) {
        this.adServers = adServers;
    }

    @Override
    public InetSocketAddress resolve(String host, int port) throws UnknownHostException {
        if (adServers.contains(host)) {
            log.info("[" + adServers.getSessionBlockedAds() + "] Blocked Ad request from: " + host);

            // InetAddress serverInetAddr = InetAddress.getByName("127.0.0.1");
            // return new InetSocketAddress(serverInetAddr, 0);
            InetAddress serverInetAddr = InetAddress.getLocalHost();
            return new InetSocketAddress(serverInetAddr, 8080);
        }

        InetAddress addr = InetAddress.getByName(host);
        return new InetSocketAddress(addr, port);
    }

}
