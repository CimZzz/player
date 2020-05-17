package com.kd.player;

import android.app.Application;
import android.content.Context;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/05/17 12:54:10
 * Project : rtsp_android
 * Since Version : Alpha
 */
public class CustomApplication extends Application  {
    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }
}
