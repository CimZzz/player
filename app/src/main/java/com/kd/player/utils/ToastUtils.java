package com.kd.player.utils;

import android.widget.Toast;

import com.kd.player.CustomApplication;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/05/17 12:53:45
 * Project : rtsp_android
 * Since Version : Alpha
 */
public final class ToastUtils {
    private static Toast toast;

    public static void sendToast(Object message) {
        if(toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(CustomApplication.context, message != null ? message.toString() : "null", Toast.LENGTH_SHORT);
        toast.show();
    }
}
