package com.virtualightning.transport.http;

import com.virtualightning.transport.MessageLooper;

import java.io.InputStream;
import java.net.ServerSocket;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/05/17 23:28:54
 * Project : player
 * Since Version : Alpha
 */
public class HttpServer {
    private MessageLooper<HttpServer, HttpEvent> messageLooper;
    private final ResBundle resBundle;

    public HttpServer(String path, Readable readable, HttpCallback callback) {
        this.httpCallback = callback;
        resBundle = new ResBundle();
        resBundle.path = path;
        resBundle.readable = readable;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 11:55 PM
    // 开启 Http Server
    ///////////////////////////////////////////////////////////////////////////
    public void start() {
        if(messageLooper == null) {
//            messageLooper
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 11:55 PM
    // 关闭 Http Server
    ///////////////////////////////////////////////////////////////////////////
    public void close() {
        if(!isRunning) {
            return;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 11:54 PM
    // Readable File
    ///////////////////////////////////////////////////////////////////////////
    public interface Readable {
        InputStream beginRead();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 12:11 AM
    // Http 回调
    ///////////////////////////////////////////////////////////////////////////
    public interface HttpCallback {
        void onStarted();
        void onClosed();
    }

    private static class ResBundle {
        String path;
        Readable readable;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 11:59 PM
    // Server Thread
    ///////////////////////////////////////////////////////////////////////////
    private static class ServerThread implements Runnable {
        private final ResBundle resBundle;
        private boolean isRunning = true;
        private ServerSocket serverSocket;

        private ServerThread(ResBundle resBundle) {
            this.resBundle = resBundle;
        }

        public void close() {
            isRunning = false;
            if(serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (Exception ignore) { }
            }
        }

        @Override
        public void run() {
            if(isRunning) {
                return;
            }
            try {
                serverSocket = new ServerSocket();
            } catch (Exception ignore) {

            }
            while (isRunning) {
            }
        }
    }
}
