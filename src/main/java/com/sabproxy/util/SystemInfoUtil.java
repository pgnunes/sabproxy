package com.sabproxy.util;

import org.apache.commons.io.FileUtils;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.Sensors;
import oshi.software.os.NetworkParams;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;

import java.util.Arrays;

public class SystemInfoUtil {

    private SystemInfo si;
    private HardwareAbstractionLayer hal;
    private OperatingSystem os;

    public SystemInfoUtil() {
        si = new SystemInfo();
        hal = si.getHardware();
        os = si.getOperatingSystem();

    }

    public String getComputerSystem() {
        return hal.getComputerSystem().toString();
    }

    public String getOS() {
        return os.toString();
    }

    public String getProcessor() {
        return hal.getProcessor().toString();
    }

    public String getMemory() {
        String meminfo = "";
        meminfo += "Total:     " + FileUtils.byteCountToDisplaySize(hal.getMemory().getTotal()) + "\n";
        meminfo += "Available: " + FileUtils.byteCountToDisplaySize(hal.getMemory().getAvailable());

        return meminfo;
    }

    public String getNetworkInterfaces() {
        NetworkIF[] netIFS = hal.getNetworkIFs();
        String netInfo = "";
        for (NetworkIF net : netIFS) {
            netInfo += "Interface: " + net.getName() + "\n";
            netInfo += "MAC Address: " + net.getMacaddr() + "\n";
            netInfo += "IPv4: " + Arrays.toString(net.getIPv4addr()) + "\n";
            if (net.getIPv6addr().length > 0) {
                netInfo += "IPv6: " + Arrays.toString(net.getIPv6addr()) + "\n";
            }
            netInfo += "Received: " + net.getPacketsRecv() + " packets / " + FormatUtil.formatBytes(net.getBytesRecv()) + " (" + net.getInErrors() + " err)\n";
            netInfo += "Transmitted: " + net.getPacketsSent() + " packets / " + FormatUtil.formatBytes(net.getBytesSent()) + " (" + net.getOutErrors() + " err)\n";
            netInfo += "\n";
        }
        return netInfo;
    }

    public String getNetworkParameters() {
        NetworkParams networkParams = os.getNetworkParams();
        String netParams = "Host name: " + networkParams.getHostName() + "\n";
        netParams += "Domain name: " + networkParams.getDomainName() + "\n";
        netParams += "DNS servers: " + Arrays.toString(networkParams.getDnsServers()) + "\n";
        if (!networkParams.getIpv4DefaultGateway().equals("")) {
            netParams += "IPv4 Gateway: " + networkParams.getIpv4DefaultGateway() + "\n";
        }
        if (!networkParams.getIpv6DefaultGateway().equals("")) {
            netParams += "IPv6 Gateway: " + networkParams.getIpv6DefaultGateway() + "\n";
        }

        return netParams;
    }

    public String getSensorsInfo() {
        Sensors sensors = hal.getSensors();
        String sensorsInfo = "CPU Temperature: " + sensors.getCpuTemperature() + "°C\n";
        if (sensors.getFanSpeeds().length > 0) {
            sensorsInfo += "Fan Speeds: " + Arrays.toString(sensors.getFanSpeeds()) + "\n";
        }
        if (sensors.getCpuVoltage() > 0) {
            sensorsInfo += "CPU Voltage: " + sensors.getCpuVoltage() + "\n";
        }
        return sensorsInfo;
    }


}
