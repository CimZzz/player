package com.virtualightning.transport.serversocket;

import com.virtualightning.transport.MessageLooper;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/18 15:25:23
 * Project : taoke_android
 * Since Version : Alpha
 */
public class ServerSocketThread implements Runnable {
    private final int serverPort;
    private MessageLooper<ServerSocketThread, Socket> messageLooper;
    private boolean isRunning = true;
    private ServerSocket serverSocket;

    public ServerSocketThread(int serverPort) {
        this.serverPort = serverPort;
        messageLooper = new MessageLooper<>(this, new MessageLooper.MessageCallback<ServerSocketThread, Socket>() {
            @Override
            public void handleMessage(ServerSocketThread dataObj, Socket message) throws Exception {

            }
        });
    }

    public void close() {
        synchronized (this) {
            isRunning = false;
        }
//        messageLooper.sendMessage();
    }


    @Override
    public void run() {
        synchronized (this) {
            if(!isRunning) {
                return;
            }
            new Thread(messageLooper).start();
        }

        while (isRunning) {
            try {
                if(serverSocket == null) {
                    serverSocket = new ServerSocket(serverPort);
                    synchronized (this) {
                        if(!isRunning) {
                            try {
                                serverSocket.close();
                            } catch (Exception ignore) { }
                            serverSocket = null;
                            return;
                        }
                    }
                }

                Socket socket = serverSocket.accept();
                synchronized (this) {
                    if(!isRunning) {
                        try {
                            socket.close();
                        } catch (Exception ignore) { }
                        return;
                    }
                }
                messageLooper.sendMessage(socket);
            } catch (Exception ignore) { }
        }
    }
//    private static class ServerMessageCallback implements MessageLooper.MessageCallback<ServerSocketThread, Socket>
}
