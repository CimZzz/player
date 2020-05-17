package com.kd.player.utils;

import android.Manifest;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/05/17 00:50:17
 * Project : rtsp_android
 * Since Version : Alpha
 */
public final class StringUtils {
    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 12:51 AM
    // 翻译权限
    ///////////////////////////////////////////////////////////////////////////
    public static String translatePermission(String permission) {
        switch (permission) {
            case Manifest.permission.RECORD_AUDIO:
                return "录音权限";
            case Manifest.permission.FOREGROUND_SERVICE:
                return "前台服务权限";
        }

        return permission;
    }
}
