package com.virtualightning.transport.entity;

import com.alibaba.fastjson.JSONObject;

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
    private int screenWidth;
    private int screenHeight;
    private float dpi;
    private long activateTime;

    public void initWithJSON(JSONObject object) {
        setHost(object.getString("host"));
        setPort(object.getIntValue("port"));
        setScreenWidth(object.getIntValue("screenWidth"));
        setScreenHeight(object.getIntValue("screenHeight"));
        setDpi(object.getFloatValue("dpi"));
    }

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

    public int getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    public float getDpi() {
        return dpi;
    }

    public void setDpi(float dpi) {
        this.dpi = dpi;
    }

    public long getActivateTime() {
        return activateTime;
    }

    public void setActivateTime(long activateTime) {
        this.activateTime = activateTime;
    }
}
