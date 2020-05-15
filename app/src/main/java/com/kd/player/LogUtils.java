package com.kd.player;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/15 16:16:30
 * Project : taoke_android
 * Since Version : Alpha
 */
public class LogUtils {
    public static LogCallback logCallback;

    public static void log(String message) {
        if(logCallback != null) {
            logCallback.onLog(message);
        }
    }

    public interface LogCallback {
        void onLog(String message);
    }
}
