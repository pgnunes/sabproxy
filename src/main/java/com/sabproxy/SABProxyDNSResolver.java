package com.sabproxy;

import com.sabproxy.util.AdServers;
import org.littleshoot.proxy.HostResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SABProxyDNSResolver implements HostResolver {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private AdServers adServers = null;

    private static final String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

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

        // check if it's an IP request
        if (isIP(host)) {
            return new InetSocketAddress(host, port);
        } else {
            InetAddress addr = InetAddress.getByName(host);
            return new InetSocketAddress(addr, port);
        }
    }

    public boolean isIP(String ip) {
        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }

}
