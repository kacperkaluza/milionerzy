package com.kaluzaplotecka.milionerzy.view.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetworkUtils {
    
    private NetworkUtils() {} // Prevent instantiation

    public static String getRealIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // Skip loopback and down interfaces
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // We want IPv4 and not loopback
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            // Fallback
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Nieznane (sprawdź ustawienia sieci)";
        }
    }
}
