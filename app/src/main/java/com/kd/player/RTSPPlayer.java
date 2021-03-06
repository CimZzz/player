package com.kd.player;

import android.view.SurfaceHolder;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/15 14:05:12
 * Project : taoke_android
 * Since Version : Alpha
 */
public class RTSPPlayer extends Player {
    private final String rtspPath;
    private IjkMediaPlayer mediaPlayer;

    public RTSPPlayer(String rtspPath) {
        this.rtspPath = rtspPath;
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

        // 支持硬解 1：开启 O:关闭
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", 1);
        // 设置播放前的探测时间 1,达到首屏秒开效果
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1);
        /**
         * 播放延时的解决方案
         */
        // 如果是rtsp协议，可以优先用tcp(默认是用udp)
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_transport", "tcp");
        // 设置播放前的最大探测时间 （100未测试是否是最佳值）
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", 100L);
        // 每处理一个packet之后刷新io上下文
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1L);
        // 需要准备好后自动播放
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);
        // 不额外优化（使能非规范兼容优化，默认值0 ）
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "fast", 1);
        // 是否开启预缓冲，一般直播项目会开启，达到秒开的效果，不过带来了播放丢帧卡顿的体验
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering",  0);
        // 自动旋屏
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0);
        // 处理分辨率变化
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0);
        // 最大缓冲大小,单位kb
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "max-buffer-size", 20);
        // 默认最小帧数2
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 2);
        // 最大缓存时长
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,  "max_cached_duration", 3); //300
        // 是否限制输入缓存数
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,  "infbuf", 1);
        // 缩短播放的rtmp视频延迟在1s内
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "nobuffer");
        // 播放前的探测Size，默认是1M, 改小一点会出画面更快
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 200); //1024L)
        // 播放重连次数
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"reconnect",5);
        // TODO:
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
        // 设置是否开启环路过滤: 0开启，画面质量高，解码开销大，48关闭，画面质量差点，解码开销小
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48L);
        // 跳过帧 ？？
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_frame", 0);
        // 视频帧处理不过来的时候丢弃一些帧达到同步的效果
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 5);
        
        try {
            mediaPlayer.setDataSource(rtspPath);
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
