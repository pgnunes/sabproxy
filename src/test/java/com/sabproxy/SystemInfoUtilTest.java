package com.sabproxy;

import com.sabproxy.util.SystemInfoUtil;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by pnunes on 5/30/17.
 */
public class SystemInfoUtilTest {
    @Test
    public void systemInfo() throws Exception {
        // only make sure we're getting some result here as this will depend on the running system...
        SystemInfoUtil sysinfo = new SystemInfoUtil();
        String networkParam = sysinfo.getNetworkParameters();
        String mem = sysinfo.getMemory();
        String netInterfaces = sysinfo.getNetworkInterfaces();
        String os = sysinfo.getOS();
        String cpu = sysinfo.getProcessor();
        String sensors = sysinfo.getSensorsInfo();

        assertThat(networkParam, CoreMatchers.containsString("Host name:"));
        assertThat(networkParam, CoreMatchers.containsString("Domain name:"));
        assertThat(networkParam, CoreMatchers.containsString("DNS servers:"));

        assertThat(mem, CoreMatchers.containsString("Total: "));

        assertThat(netInterfaces, CoreMatchers.containsString("Interface: "));
        assertThat(netInterfaces, CoreMatchers.containsString("MAC Address: "));
        assertThat(netInterfaces, CoreMatchers.containsString("IPv4: "));
        assertThat(netInterfaces, CoreMatchers.containsString("Received: "));
        assertThat(netInterfaces, CoreMatchers.containsString("Transmitted: "));

        assertNotNull(os);
        assertNotNull(cpu);
        assertNotNull(sensors);
    }

}