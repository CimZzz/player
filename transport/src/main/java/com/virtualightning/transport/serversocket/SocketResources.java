package com.virtualightning.transport.serversocket;

import java.util.HashMap;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/18 18:38:17
 * Project : taoke_android
 * Since Version : Alpha
 */
public class SocketResources {
    private HashMap<String, Object> resourcesMap = new HashMap<>();

    public void put(String key, Object object) {
        resourcesMap.put(key, object);
    }

    public Object get(String key) {
        return resourcesMap.get(key);
    }
}
