package com.kd.player.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/05/16 22:32:34
 * Project : rtsp_android
 * Since Version : Alpha
 */
public final class NetUtils {

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();

                if (intf.getName().toLowerCase().equals("wlan0")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf
                            .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()) {
                            String ipaddress = inetAddress.getHostAddress()
                                    .toString();

                            if (!ipaddress.contains("::")) { // 判断当前为ipV4地址
                                return ipaddress;
                            }
                        }
                    }
                }
            }
        } catch (SocketException ignored) { }
        return null;
    }
}
