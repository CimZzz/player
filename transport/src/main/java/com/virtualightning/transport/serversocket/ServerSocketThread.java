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
public class ServerSocketThread<T> implements Runnable {
    private final int serverPort;
    private MessageLooper<ServerSocketThread<T>, SocketEvent> messageLooper;
    private boolean isRunning = true;
    private ServerSocket serverSocket;

    public ServerSocketThread(int serverPort, SocketThreadFactory socketThreadFactory, SocketMessageCallback socketMessageCallback) {
        this.serverPort = serverPort;
        messageLooper = new MessageLooper<>(this, new ServerMessageCallback<T>(socketThreadFactory, socketMessageCallback));
        messageLooper.setMessageRefuseCallback(new ServerRefuseCallback());
    }

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
                        try {
                            socket.close();
                        } catch (Exception ignore) {
                        }
                        return;
                    }
                }
//                messageLooper.sendMessage(socket);
            } catch (Exception ignore) {
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:29
    // Socket 回调方法
    ///////////////////////////////////////////////////////////////////////////
    public interface SocketMessageCallback {
        void onEvent(int type, Object dataObj);
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
    private static class ServerMessageCallback<T> implements MessageLooper.MessageCallback<ServerSocketThread<T>, SocketEvent> {
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
        public void handleMessage(ServerSocketThread<T> dataObj, SocketEvent message) throws Exception {
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
                        SocketThread socketThread = socketThreadFactory.generateSocketThread(idCode, socket);
                        socketThread.setParentMessageLooper(dataObj.messageLooper);
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

                case SocketEvent.Type_Send_Response: {
                    if(socketMessageCallback != null) {
                        socketMessageCallback.onEvent((Integer) message.getEventData(), message.getOtherData());
                    }
                    break;
                }
            }
        }
    }
}
