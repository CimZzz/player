package com.virtualightning.transport.serversocket;

import android.util.SparseArray;

import com.virtualightning.transport.MessageLooper;
import com.virtualightning.transport.ObjectFuture;

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
    private MessageLooper<ServerSocketThread, SocketEvent> messageLooper;
    private boolean isRunning = true;
    private ServerSocket serverSocket;

    public ServerSocketThread(int serverPort, SocketThreadFactory socketThreadFactory, SocketMessageCallback socketMessageCallback) {
        this.serverPort = serverPort;
        messageLooper = new MessageLooper<>(this, new ServerMessageCallback(socketThreadFactory, socketMessageCallback));
        messageLooper.setMessageRefuseCallback(new ServerRefuseCallback());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:53
    // 外部调用关闭方法
    ///////////////////////////////////////////////////////////////////////////
    public void close() {
        synchronized (this) {
            isRunning = false;
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (Exception ignore) {
                }
                serverSocket = null;
            }
        }
        messageLooper.sendMessage(new SocketEvent(SocketEvent.Type_Close));
        messageLooper.closeUntilMessageHandleCompleted();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:53
    // 外部对 Socket 发送消息
    ///////////////////////////////////////////////////////////////////////////
    public void sendMessage(int socketId, Object data) {
        messageLooper.sendMessage(new SocketEvent(SocketEvent.Type_Response, socketId, data));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:54
    // 修改 SocketResources
    ///////////////////////////////////////////////////////////////////////////
    public void modifySocketResources(ModifyResourceCallback callback) {
        messageLooper.sendMessage(new SocketEvent(SocketEvent.Type_Modify_Resource, callback));
    }

    @Override
    public void run() {
        synchronized (this) {
            if (!isRunning) {
                return;
            }
            new Thread(messageLooper).start();
        }

        while (isRunning) {
            try {
                if (serverSocket == null) {
                    serverSocket = new ServerSocket(serverPort);
                    synchronized (this) {
                        if (!isRunning) {
                            try {
                                serverSocket.close();
                            } catch (Exception ignore) {
                            }
                            serverSocket = null;
                            return;
                        }
                    }
                }

                Socket socket = serverSocket.accept();
                synchronized (this) {
                    if (!isRunning) {
                        try { socket.close(); } catch (Exception ignore) { }
                        return;
                    }
                }
            } catch (Exception ignore) { try { Thread.sleep(1); } catch (Exception ignore2) { }}
        }
        close();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:29
    // Socket 回调方法
    ///////////////////////////////////////////////////////////////////////////
    public interface SocketMessageCallback {
        void onEvent(int socketId, int type, Object dataObj);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午6:42
    // 修改资源回调
    ///////////////////////////////////////////////////////////////////////////
    public interface ModifyResourceCallback {
        void onModify(SocketResources socketResources);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:11
    // Socket Thread 工厂
    ///////////////////////////////////////////////////////////////////////////
    public interface SocketThreadFactory {
        SocketThread generateSocketThread(int idCode, Socket socket);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午5:19
    // 拒绝服务回调
    ///////////////////////////////////////////////////////////////////////////
    private static class ServerRefuseCallback implements MessageLooper.MessageRefuseCallback<SocketEvent> {
        @Override
        public void refuseMessage(SocketEvent message) throws Exception {
            switch (message.getEventType()) {
                case SocketEvent.Type_Socket: {
                    Socket socket = (Socket) message.getEventData();
                    if (socket != null) {
                        socket.close();
                    }
                    break;
                }

                case SocketEvent.Type_Get_Resource: {
                    ObjectFuture objectFuture = (ObjectFuture) message.getOtherData();
                    objectFuture.complete(null);
                    break;
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午5:19
    // Socket 消息处理回调
    ///////////////////////////////////////////////////////////////////////////
    private static class ServerMessageCallback implements MessageLooper.MessageCallback<ServerSocketThread, SocketEvent> {
        private final SocketThreadFactory socketThreadFactory;
        private final SocketMessageCallback socketMessageCallback;

        private SparseArray<SocketThread> socketThreadArr = new SparseArray<>();
        private SocketResources socketResources = new SocketResources();
        private int socketCode = 0;

        public ServerMessageCallback(SocketThreadFactory socketThreadFactory, SocketMessageCallback socketMessageCallback) {
            this.socketThreadFactory = socketThreadFactory;
            this.socketMessageCallback = socketMessageCallback;
        }

        @Override
        public void handleMessage(ServerSocketThread serverSocketThread, SocketEvent message) throws Exception {
            switch (message.getEventType()) {
                case SocketEvent.Type_Close: {
                    int length = socketThreadArr.size();
                    for(int i = 0 ; i < length ; i ++) {
                        SocketThread socketThread = socketThreadArr.valueAt(i);
                        socketThread.close();
                    }
                    socketThreadArr.clear();
                    break;
                }
                case SocketEvent.Type_Socket_Close: {
                    int codeId = (int) message.getEventData();
                    SocketThread socketThread = socketThreadArr.get(codeId);
                    if(socketThread != null) {
                        socketThread.close();
                        socketThreadArr.remove(codeId);
                    }
                    break;
                }

                case SocketEvent.Type_Socket: {
                    Socket socket = (Socket) message.getEventData();
                    if(socket != null) {
                        int idCode = socketCode ++;
                        if(socketCode == Integer.MAX_VALUE) {
                            socketCode = 0;
                        }
                        SocketThread socketThread = socketThreadFactory.generateSocketThread(idCode, socket);
                        socketThread.setParentMessageLooper(serverSocketThread.messageLooper);
                        socketThreadArr.put(idCode, socketThread);
                        new Thread(socketThread).start();
                    }
                    break;
                }

                case SocketEvent.Type_Modify_Resource: {
                    ModifyResourceCallback callback = (ModifyResourceCallback) message.getEventData();
                    callback.onModify(socketResources);
                    break;
                }

                case SocketEvent.Type_Get_Resource: {
                    String key = (String) message.getEventData();
                    ObjectFuture objectFuture = (ObjectFuture) message.getOtherData();
                    if(key != null && objectFuture != null) {
                        objectFuture.complete(socketResources.get(key));
                    }
                    break;
                }

                case SocketEvent.Type_Send: {
                    Object[] eventData = (Object[]) message.getEventData();
                    if(eventData != null && eventData.length == 3) {
                        if (socketMessageCallback != null) {
                            socketMessageCallback.onEvent((int)eventData[0], (int) eventData[1], eventData[2]);
                        }
                    }
                    break;
                }

                case SocketEvent.Type_Response: {
                    SocketThread socketThread = socketThreadArr.get((Integer) message.getEventData());
                    if(socketThread != null) {
                        socketThread.receiveMessage(message.getOtherData());
                    }
                }
            }
        }
    }
}
