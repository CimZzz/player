package com.virtualightning.transport.control;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.virtualightning.transport.environment.TransportConstants;
import com.virtualightning.transport.serversocket.ServerSocketThread;
import com.virtualightning.transport.serversocket.SocketThread;

import java.io.InputStream;
import java.net.Socket;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/05/17 17:59:24
 * Project : player
 * Since Version : Alpha
 */
public class ControlServer {
    private final ControlCallback callback;
    private ServerSocketThread serverSocketThread;

    public ControlServer(ControlCallback callback) {
        this.callback = callback;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 下午2:16
    // 启动 Control Server
    ///////////////////////////////////////////////////////////////////////////
    public void start() {
        if(serverSocketThread == null) {
            serverSocketThread = new ServerSocketThread(TransportConstants.Control_Port,
                    true, new SocketFactory(), new SocketMessageCallback(callback));
            new Thread(serverSocketThread).start();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 下午2:17
    // 关闭 Control Server
    ///////////////////////////////////////////////////////////////////////////
    public void close() {
        if(serverSocketThread != null) {
            serverSocketThread.close();
            serverSocketThread = null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 下午2:29
    // 控制消息回调
    ///////////////////////////////////////////////////////////////////////////
    public interface ControlCallback {
        void onReceiveControlEvent(ControlEvent controlEvent);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 下午2:17
    // Socket 消息回调
    ///////////////////////////////////////////////////////////////////////////
    private static class SocketMessageCallback implements ServerSocketThread.SocketMessageCallback {
        private final ControlCallback controlMessageCallback;

        private SocketMessageCallback(ControlCallback controlMessageCallback) {
            this.controlMessageCallback = controlMessageCallback;
        }

        @Override
        public void onEvent(int socketId, int type, Object dataObj) {
            controlMessageCallback.onReceiveControlEvent(new ControlEvent(type, dataObj));
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 下午2:17
    // 控制 Socket 工厂
    ///////////////////////////////////////////////////////////////////////////
    private static class SocketFactory implements ServerSocketThread.SocketThreadFactory {
        @Override
        public SocketThread generateSocketThread(int idCode, Socket socket) {
            return new ControlSocketThread(idCode, socket);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 下午2:17
    // 控制 Socket 线程
    ///////////////////////////////////////////////////////////////////////////
    private static class ControlSocketThread extends SocketThread {
        InputStream inputStream;

        ControlSocketThread(int idCode, Socket socket) {
            super(idCode, socket);
        }

        @Override
        protected void onClose() {
            sendResponse(ControlEvent.Type_Closed, null);
        }

        @Override
        protected void onInit() throws Exception {
            inputStream = socket.getInputStream();
            sendResponse(ControlEvent.Type_Started, null);
        }

        @Override
        protected void onRun() throws Exception {
            int oneBit = inputStream.read();
            int twoBit = inputStream.read();
            int threeBit = inputStream.read();
            int fourBit = inputStream.read();

            if(oneBit == -1 || twoBit == -1 || threeBit == -1 || fourBit == -1) {
                markLooperEnd();
                return;
            }

            int length = (oneBit & 0xFF) | ((twoBit & 0xFF) << 8) | ((threeBit & 0xFF) << 16) | ((fourBit & 0xFF) << 24);
            if(length > 0) {
                byte[] buffer = new byte[length];
                int readLength = 0;
                while(readLength <= length) {
                    int tempLength = inputStream.read(buffer, readLength, length - readLength);
                    if(tempLength == -1) {
                        markLooperEnd();
                        return;
                    }
                    readLength += length;
                }

                JSONObject object = JSON.parseObject(new String(buffer, 0, length));
                int eventType = object.getIntValue("type");
                Object eventData = object.get("data");
                sendResponse(eventType, eventData);
            }
        }
    }
}
