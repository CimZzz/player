package com.kd.player;

import android.view.SurfaceHolder;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/15 14:07:35
 * Project : taoke_android
 * Since Version : Alpha
 */
public abstract class Player {
    private onErrorCallback errorListener;
    private IjkMediaPlayer.OnVideoSizeChangedListener videoSizeChangedListener;

    public abstract void start(SurfaceHolder display);

    public abstract void close();

    public void setErrorListener(onErrorCallback errorListener) {
        this.errorListener = errorListener;
    }

    public void setVideoSizeChangedListener(IjkMediaPlayer.OnVideoSizeChangedListener videoSizeChangedListener) {
        this.videoSizeChangedListener = videoSizeChangedListener;
    }

    protected final void onError(Exception e) {
        if(errorListener != null) {
            errorListener.onError(e);
        }
    }

    protected final void configMediaPlayer(IjkMediaPlayer mediaPlayer) {
        mediaPlayer.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
                if(errorListener != null) {
                    errorListener.onError(null);
                }
                return false;
            }
        });
        mediaPlayer.setOnVideoSizeChangedListener(videoSizeChangedListener);
    }

    public interface onErrorCallback {
        void onError(Exception e);
    }
}
