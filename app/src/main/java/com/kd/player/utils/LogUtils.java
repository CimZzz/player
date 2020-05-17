package com.kd.player.utils;

import android.util.Log;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/05/16 22:30:16
 * Project : rtsp_android
 * Since Version : Alpha
 */
public final class LogUtils {
    private static final String DEFAULT_TAG = "CimZzz";

    public static void log(Object message) {
        log(DEFAULT_TAG, message);
    }

    public static void log(String tag, Object message) {
        if(message == null) {
            Log.v(tag, "null");
        }
        else {
            Log.v(tag, message.toString());
        }
    }
}
