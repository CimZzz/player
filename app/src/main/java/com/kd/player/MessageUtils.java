package com.kd.player;

import com.alibaba.fastjson.JSONObject;

import java.net.DatagramPacket;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/15 18:28:51
 * Project : taoke_android
 * Since Version : Alpha
 */
public class MessageUtils {
    public static DatagramPacket transformJSON(JSONObject object) {
        byte[] bytes = object.toJSONString().getBytes();
        return new DatagramPacket(bytes, 0, bytes.length);
    }
}
