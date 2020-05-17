package com.virtualightning.transport.control;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.virtualightning.transport.MessageLooper;
import com.virtualightning.transport.environment.TransportConstants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/05/17 17:59:24
 * Project : player
 * Since Version : Alpha
 */
public class ControlServer {
    private MessageLooper<ControlCallback, ControlEvent> messageLooper;
    private final ControlCallback callback;
    private final ResBundle resBundle;

    public ControlServer(ControlCallback callback) {
        this.callback = callback;
        this.resBundle = new ResBundle();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 6:28 PM
    // 启动 Control 服务器
    ///////////////////////////////////////////////////////////////////////////
    public void start() {
        if(messageLooper == null) {
            messageLooper = new MessageLooper<>(callback, new MessageCallback(resBundle));
            new Thread(messageLooper).start();
            new Thread(messageLooper.beginProduce(new MessageLooper.ProduceChain<>(resBundle, new EventProduce()))).start();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 6:28 PM
    // 挂壁 Control 服务器
    ///////////////////////////////////////////////////////////////////////////
    public void close() {
        if(messageLooper != null) {
            messageLooper.close();
            messageLooper = null;
        }

        resBundle.isClose = true;

        if(resBundle.serverSocket != null) {
            try {
                resBundle.serverSocket.close();
            } catch (Exception ignore) { }
            resBundle.serverSocket = null;
        }

        if(resBundle.socket != null) {
            try {
                resBundle.socket.close();
            } catch (Exception ignore) { }
            resBundle.socket = null;
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 6:34 PM
    // 发送事件
    ///////////////////////////////////////////////////////////////////////////
    public void sendEvent(ControlEvent event) {
        messageLooper.sendMessage(new ControlEvent(ControlEvent.Type_Send, new Object[]{event.getEventType(), event.getEventData()}));
    }


    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 6:11 PM
    // Control 服务器资源
    ///////////////////////////////////////////////////////////////////////////
    private static class ResBundle {
        boolean isClose = false;
        ServerSocket serverSocket;
        Socket socket;
        InputStream reader;
        OutputStream writer;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 6:02 PM
    // 控制指令回调
    ///////////////////////////////////////////////////////////////////////////
    public interface ControlCallback {
        void onReceiveControlEvent(ControlEvent controlEvent);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 6:13 PM
    // 消息回调
    ///////////////////////////////////////////////////////////////////////////
    private static class MessageCallback implements MessageLooper.MessageCallback<ControlCallback, ControlEvent> {
        private final WeakReference<ResBundle> bundleRef;

        private MessageCallback(ResBundle bundle) {
            this.bundleRef = new WeakReference<>(bundle);
        }

        @Override
        public void handleMessage(ControlCallback callback, ControlEvent message) throws Exception {
            if(message.getEventType() == ControlEvent.Type_Send) {
                Object[] sendParams = (Object[]) message.getEventData();
                int eventType = (int) sendParams[0];
                Object eventData = sendParams[1];
                JSONObject object = new JSONObject();
                object.put("type", eventType);
                object.put("data", eventData);
                byte[] buffer = object.toJSONString().getBytes();
                ResBundle bundle = bundleRef.get();
                if(bundle != null && bundle.writer != null) {
                    int length = buffer.length;
                    bundle.writer.write(length & 0xFF);
                    bundle.writer.write((length >> 8) & 0xFF);
                    bundle.writer.write((length >> 16) & 0xFF);
                    bundle.writer.write((length >> 24) & 0xFF);
                    bundle.writer.write(buffer);
                    bundle.writer.flush();
                }
            }
            else {
                callback.onReceiveControlEvent(message);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 6:05 PM
    // 事件生产器
    ///////////////////////////////////////////////////////////////////////////
    private static class EventProduce implements MessageLooper.ProduceCallback<ResBundle, ControlEvent> {

        @Override
        public ControlEvent produce(ResBundle bundle) throws Exception {
            if(bundle.isClose) {
                if(bundle.serverSocket != null) {
                    try {
                        bundle.serverSocket.close();
                    } catch (Exception ignore) { }
                    bundle.serverSocket = null;
                }
                if(bundle.socket != null) {
                    try {
                        bundle.socket.close();
                    } catch (Exception ignore) { }
                    bundle.socket = null;
                }
                return null;
            }
            if(bundle.serverSocket == null) {
                bundle.serverSocket = new ServerSocket(TransportConstants.Control_Port);
            }
            if(bundle.socket == null) {
                bundle.socket = bundle.serverSocket.accept();
                bundle.reader = bundle.socket.getInputStream();
                bundle.writer = bundle.socket.getOutputStream();
                return ControlEvent.Event_Started;
            }

            if(bundle.socket.isClosed()) {
                bundle.socket = null;
                return ControlEvent.Event_Closed;
            }

            int oneBit = bundle.reader.read();
            int twoBit = bundle.reader.read();
            int threeBit = bundle.reader.read();
            int fourBit = bundle.reader.read();
            if(oneBit == -1 || twoBit == -1 || threeBit == -1 || fourBit == -1) {
                // EOF
                bundle.socket.close();
                return null;
            }
            int length = (oneBit & 0xFF) | ((twoBit & 0xFF) << 8) | ((threeBit & 0xFF) << 16) | ((fourBit & 0xFF) << 24);
            if(length > 0) {
                byte[] buffer = new byte[length];
                int readLength = 0;
                while(readLength <= length) {
                    int tempLength = bundle.reader.read(buffer, readLength, length - readLength);
                    if(tempLength == -1) {
                        bundle.socket.close();
                        return null;
                    }
                    readLength += length;
                }

                JSONObject object = JSON.parseObject(new String(buffer, 0, length));
                int eventType = object.getIntValue("type");
                Object eventData = object.get("data");
                return new ControlEvent(eventType, eventData);
            }

            return null;
        }
    }
}
