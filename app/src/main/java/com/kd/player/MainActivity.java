package com.kd.player;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.virtualightning.transport.communication.CommunicationSlaveServer;
import com.virtualightning.transport.control.ControlEvent;
import com.virtualightning.transport.control.ControlServer;
import com.virtualightning.transport.utils.LogUtils;

import java.lang.ref.WeakReference;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final int Play_Type_None = 0;
    private static final int Play_Type_RTSP = 1;
    private static final int Play_Type_Video = 2;

    private View rootView;
    private SurfaceView surfaceView;

    private Player player;
    private MainHandler mainHandler;
    private CommunicationSlaveServer slaveServer;
    private ControlServer controlServer;

    private boolean isDestroyed;
    private boolean isSurfaceCreate;
    private boolean isControlStart;
    private int playType = Play_Type_None;

    private String playUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootView = findViewById(R.id.rootView);
        surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);
        surfaceView.setVisibility(View.INVISIBLE);

        mainHandler = new MainHandler(this);
        startCommunicationServer();
        startControlServer();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isSurfaceCreate = true;
        checkStatus();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isSurfaceCreate = false;
        checkStatus();
    }
    
    @Override
    protected void onDestroy() {
        isDestroyed = true;
        checkStatus();
        super.onDestroy();
    }

    /***************************************************************************
     *
     * 私有方法
     *
     **************************************************************************/

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2:16 PM
    // 处理消息
    ///////////////////////////////////////////////////////////////////////////
    private void handleControlEvent(ControlEvent controlEvent) {
        switch(controlEvent.getEventType()) {
            case ControlEvent.Type_Started:
                LogUtils.log("开始控制");
                isControlStart = true;
                break;
            case ControlEvent.Type_Closed:
                LogUtils.log("停止控制");
                isControlStart = false;
                checkStatus();
                break;
            case ControlEvent.Type_Play_RTSP:
                LogUtils.log("播放 RTSP");
                surfaceView.setVisibility(View.VISIBLE);
                playType = Play_Type_RTSP;
                playUrl = (String) controlEvent.getEventData();
                checkStatus();
                break;
            case ControlEvent.Type_Stop_RTSP:
                LogUtils.log("停止播放 RTSP");
                if(playType == Play_Type_RTSP) {
                    playType = Play_Type_None;
                    checkStatus();
                }
                break;
            case ControlEvent.Type_Play_Video:
                LogUtils.log("播放视频");
                surfaceView.setVisibility(View.VISIBLE);
                playType = Play_Type_Video;
                playUrl = (String) controlEvent.getEventData();
                checkStatus();
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2:29 PM
    // 检查当前运行状态
    ///////////////////////////////////////////////////////////////////////////
    private void checkStatus() {
        if(isDestroyed) {
            mainHandler.close();
            closeCommunicationServer();
            closeControlServer();
            closePlayer();
            return;
        }

        if(!isControlStart) {
            playType = Play_Type_None;
            closePlayer();
            return;
        }

        if(playType == Play_Type_None) {
            closePlayer();
            return;
        }

        if(isSurfaceCreate) {
            switch (playType) {
                case Play_Type_RTSP:
                    startRTSP();
                    break;
                case Play_Type_Video:
                    startVideo();
                    break;
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 3:16 PM
    // 关闭播放器
    ///////////////////////////////////////////////////////////////////////////
    private void closePlayer() {
        if(player != null) {
            player.close();
            player = null;
            surfaceView.setVisibility(View.INVISIBLE);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 3:18 PM
    // 开启 CommunicationServer
    ///////////////////////////////////////////////////////////////////////////
    private void startCommunicationServer() {
        if(slaveServer == null) {
            CommunicationSlaveServer.ALLOW_LOG = false;
            slaveServer = new CommunicationSlaveServer();
            slaveServer.start();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 3:18 PM
    // 关闭 CommunicationServer
    ///////////////////////////////////////////////////////////////////////////
    private void closeCommunicationServer() {
        if(slaveServer != null) {
            slaveServer.close();
            slaveServer = null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 7:40 PM
    // 开启 ControlServer
    ///////////////////////////////////////////////////////////////////////////
    private void startControlServer() {
        if(controlServer == null) {
            controlServer = new ControlServer(new ControlCallback(mainHandler));
            controlServer.start();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 7:41 PM
    // 关闭 ControlServer
    ///////////////////////////////////////////////////////////////////////////
    private void closeControlServer() {
        if(controlServer != null) {
            controlServer.close();
            controlServer = null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 7:50 PM
    // 开启 RTSP 播放器
    ///////////////////////////////////////////////////////////////////////////
    private void startRTSP() {
        if(player != null) {
            player.close();
        }
        player = new RTSPPlayer(playUrl);
        player.setVideoSizeChangedListener(new VideoChangeCallback());
        player.start(surfaceView.getHolder());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 7:50 PM
    // 开启 Http 播放器
    ///////////////////////////////////////////////////////////////////////////
    private void startVideo() {
        if(player != null) {
            player.close();
        }
        player = new VideoPlayer(playUrl);
        player.setVideoSizeChangedListener(new VideoChangeCallback());
        player.start(surfaceView.getHolder());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 10:16 PM
    // 视频尺寸变化回调
    ///////////////////////////////////////////////////////////////////////////
    private class VideoChangeCallback implements IjkMediaPlayer.OnVideoSizeChangedListener {
        @Override
        public void onVideoSizeChanged(IMediaPlayer iMediaPlayer, int videoWidth, int videoHeight, int i2, int i3) {
            int surfaceWidth, surfaceHeight;
            if(getResources().getConfiguration().orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
                surfaceWidth=rootView.getWidth();
                surfaceHeight=rootView.getHeight();
            }else {
                surfaceWidth=rootView.getHeight();
                surfaceHeight=rootView.getWidth();
            }

            //根据视频尺寸去计算->视频可以在sufaceView中放大的最大倍数。
            float max;
            if (getResources().getConfiguration().orientation== ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                //竖屏模式下按视频宽度计算放大倍数值
                max = Math.max((float) videoWidth / (float) surfaceWidth,(float) videoHeight / (float) surfaceHeight);
            } else{
                //横屏模式下按视频高度计算放大倍数值
                max = Math.max(((float) videoWidth/(float) surfaceHeight),(float) videoHeight/(float) surfaceWidth);
            }

            //视频宽高分别/最大倍数值 计算出放大后的视频尺寸
            videoWidth = (int) Math.ceil((float) videoWidth / max);
            videoHeight = (int) Math.ceil((float) videoHeight / max);

            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) surfaceView.getLayoutParams();
            layoutParams.width = videoWidth;
            layoutParams.height = videoHeight;
            surfaceView.setLayoutParams(layoutParams);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 7:50 PM
    // 控制指令回调
    ///////////////////////////////////////////////////////////////////////////
    private static class ControlCallback implements ControlServer.ControlCallback {
        private final MainHandler mainHandler;

        private ControlCallback(MainHandler mainHandler) {
            this.mainHandler = mainHandler;
        }

        @Override
        public void onReceiveControlEvent(ControlEvent controlEvent) {
            mainHandler.obtainMessage(0, controlEvent).sendToTarget();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2:14 PM
    // 主线程中 MainActivity 消息回调处理器
    ///////////////////////////////////////////////////////////////////////////
    public static class MainHandler extends Handler {
        private final WeakReference<MainActivity> activityRef;

        private MainHandler(MainActivity activity) {
            super(Looper.getMainLooper());
            this.activityRef = new WeakReference<>(activity);
        }

        private void close() {
            activityRef.clear();
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if(msg.obj instanceof ControlEvent) {
                MainActivity activity = activityRef.get();
                if (activity != null) {
                    activity.handleControlEvent((ControlEvent) msg.obj);
                }
            }
        }
    }
}
