package com.kd.player;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private SurfaceView surfaceView;
    private TextView tipsView;

    private String uuid;
    private Player player;
    private CommunicationServer server;
    private boolean isSurfaceCreate;
    private boolean isServerStart;

    private String rtspServerUrl;
    private String httpServerUrl;

    private JSONObject lastMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceView);
        tipsView = findViewById(R.id.tipsView);
        surfaceView.getHolder().addCallback(this);
        LogUtils.logCallback = new LogUtils.LogCallback() {
            @Override
            public void onLog(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tipsView.append(message + "\n");
                    }
                });
            }
        };

        findViewById(R.id.startBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                server.start();
            }
        });

        findViewById(R.id.changeBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        uuid = UUID.randomUUID().toString();
        server = new CommunicationServer(uuid, new CommunicationServer.CommunicationCallback() {
            @Override
            public void onCommunicate(JSONObject object) {
                if("init".equals(object.getString("type"))) {
                    rtspServerUrl = object.getString("rtsp");
                    httpServerUrl = object.getString("http");
                    isServerStart = true;
                    checkStart();
                }
                else handleMessage(object);
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isSurfaceCreate = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isSurfaceCreate = false;
        destroy();
    }

    @Override
    protected void onDestroy() {
        destroy();
        super.onDestroy();
    }

    private void destroy() {
        player.close();
        server.close();
    }

    private void handleMessage(JSONObject message) {
        lastMessage = message;
        switch (message.getString("type")) {
            case "rtsp":
                startRTSP();
                break;
            case "http":
                startHTTP();
                break;
        }
    }

    private void checkStart() {
        if(isSurfaceCreate && isServerStart) {
            if(lastMessage != null) {
                handleMessage(lastMessage);
            }
        }
    }

    private void startRTSP() {
        if(isSurfaceCreate && isServerStart) {
            if (player instanceof RTSPPlayer) {
                return;
            }
            if(player != null) {
                player.close();
            }
            player = new RTSPPlayer(rtspServerUrl);
            player.start(surfaceView.getHolder());
        }
    }

    private void startHTTP() {
        if(isSurfaceCreate && isServerStart) {
            if (player instanceof VideoPlayer) {
                return;
            }

            if(player != null) {
                player.close();
            }
            player = new VideoPlayer(httpServerUrl);
            player.start(surfaceView.getHolder());
        }
    }
}
