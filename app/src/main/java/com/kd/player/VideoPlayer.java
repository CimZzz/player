package com.kd.player;

import android.content.Context;
import android.net.Uri;
import android.view.SurfaceHolder;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/15 14:05:12
 * Project : taoke_android
 * Since Version : Alpha
 */
public class VideoPlayer extends Player {
    private final Context context;
    private final Uri uri;
    private final String url;
    private IjkMediaPlayer mediaPlayer;

    public VideoPlayer(String url) {
        this.context = null;
        this.uri = null;
        this.url = url;
    }

    public VideoPlayer(Context context, Uri uri) {
        this.context = context;
        this.uri = uri;
        this.url = null;
    }

    @Override
    public void start(SurfaceHolder display) {
        mediaPlayer = new IjkMediaPlayer();
        mediaPlayer.setDisplay(display);
        mediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer iMediaPlayer) {
                mediaPlayer.start();
            }
        });
        configMediaPlayer(mediaPlayer);

        try {
            if(uri != null) {
                mediaPlayer.setDataSource(context, uri);
            }
            else {
                mediaPlayer.setDataSource(url);
            }
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            onError(e);
        }
    }

    @Override
    public void close() {
        if(mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Exception ignored) { }
            try {
                mediaPlayer.release();
            } catch (Exception ignored) { }

            mediaPlayer = null;
        }
    }
}
