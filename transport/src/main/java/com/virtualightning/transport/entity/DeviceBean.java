package com.virtualightning.transport.entity;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/05/17 16:24:35
 * Project : player
 * Since Version : Alpha
 */
public class DeviceBean {
    private String id;
    private String host;
    private int port;
    private long activateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getActivateTime() {
        return activateTime;
    }

    public void setActivateTime(long activateTime) {
        this.activateTime = activateTime;
    }
}
