package com.virtualightning.transport.serversocket;

import com.virtualightning.transport.MessageLooper;
import com.virtualightning.transport.ObjectFuture;

import java.lang.ref.WeakReference;
import java.net.Socket;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/18 17:20:31
 * Project : taoke_android
 * Since Version : Alpha
 */
public abstract class SocketThread implements Runnable {
    protected final int idCode;
    protected Socket socket;
    private WeakReference<MessageLooper<?, SocketEvent>> parentMessageLooper;

    private boolean isRunning = true;
    private boolean isLooperRunning = true;

    public SocketThread(int idCode, Socket socket) {
        this.socket = socket;
        this.idCode = idCode;
    }

    void setParentMessageLooper(MessageLooper<?, SocketEvent> parentMessageLooper) {
        this.parentMessageLooper = new WeakReference<MessageLooper<?, SocketEvent>>(parentMessageLooper);
    }

    public void close() {
        synchronized (this) {
            if(!isRunning) {
                return;
            }
            isRunning = false;
            isLooperRunning = false;
        }
        onClose();
        MessageLooper<?, SocketEvent> messageLooper = parentMessageLooper.get();
        if(messageLooper != null) {
            messageLooper.sendMessage(new SocketEvent(SocketEvent.Type_Socket_Close, idCode));
            parentMessageLooper.clear();
        }
        if(socket != null) {
            try { socket.close(); } catch (Exception ignore) { }
            socket = null;
        }
    }

    @Override
    public final void run() {
        boolean isInit = true;
        try { onInit(); } catch (Exception ignore) { isInit = false; }
        if(isInit) {
            while (isLooperRunning && isRunning) {
                try { onRun(); } catch (Exception ignore) { }
            }
        }
        close();
    }


    protected final void markLooperEnd() {
        isLooperRunning = false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:50
    // 处理收到消息
    ///////////////////////////////////////////////////////////////////////////
    public void receiveMessage(Object data) {

    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:15
    // 检查是否仍在运行中
    ///////////////////////////////////////////////////////////////////////////
    protected final boolean isRunning() {
        return isRunning;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:16
    // 处理额外关闭逻辑
    ///////////////////////////////////////////////////////////////////////////
    protected abstract void onClose();

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 下午2:08
    // 初始化回调
    ///////////////////////////////////////////////////////////////////////////
    protected abstract void onInit() throws Exception;

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:17
    // Looper 执行逻辑回调
    // 抛出异常不会中断 Looper
    ///////////////////////////////////////////////////////////////////////////
    protected abstract void onRun() throws Exception;


    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:18
    // 从 SocketResources 中获取字符串
    ///////////////////////////////////////////////////////////////////////////
    protected final String getStringResource(String key, String defaultValue) {
        ObjectFuture objectFuture = sendResourceEvent(key);
        if(objectFuture == null) {
            return defaultValue;
        }

        Object obj = objectFuture.get();
        if(obj instanceof String) {
            return (String) obj;
        }
        return defaultValue;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:18
    // 从 SocketResources 中获取字符串
    ///////////////////////////////////////////////////////////////////////////
    protected final Integer getIntResource(String key, Integer defaultValue) {
        ObjectFuture objectFuture = sendResourceEvent(key);
        if(objectFuture == null) {
            return defaultValue;
        }

        Object obj = objectFuture.get();
        if(obj instanceof Integer) {
            return (Integer) obj;
        }
        return defaultValue;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:18
    // 从 SocketResources 中获取字符串
    ///////////////////////////////////////////////////////////////////////////
    protected final Float getFloatResource(String key, Float defaultValue) {
        ObjectFuture objectFuture = sendResourceEvent(key);
        if(objectFuture == null) {
            return defaultValue;
        }

        Object obj = objectFuture.get();
        if(obj instanceof Float) {
            return (Float) obj;
        }
        return defaultValue;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:18
    // 从 SocketResources 中获取字符串
    ///////////////////////////////////////////////////////////////////////////
    protected final Object getResource(String key, Object defaultValue) {
        ObjectFuture objectFuture = sendResourceEvent(key);
        if(objectFuture == null) {
            return defaultValue;
        }

        Object obj = objectFuture.get();
        if(obj != null) {
            return obj;
        }
        return defaultValue;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:40
    // 发送响应
    ///////////////////////////////////////////////////////////////////////////
    protected final void sendResponse(int type, Object data) {
        MessageLooper<?, SocketEvent> messageLooper = parentMessageLooper.get();
        if(messageLooper != null) {
            messageLooper.sendMessage(new SocketEvent(SocketEvent.Type_Send, new Object[]{idCode, type, data}));
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/18 下午7:21
    // 发送资源事件
    ///////////////////////////////////////////////////////////////////////////
    private ObjectFuture sendResourceEvent(String key) {
        MessageLooper<?, SocketEvent> messageLooper = parentMessageLooper.get();
        if(messageLooper != null) {
            ObjectFuture objectFuture = new ObjectFuture();
            messageLooper.sendMessage(new SocketEvent(SocketEvent.Type_Get_Resource, key, objectFuture));
            return objectFuture;
        }
        return null;
    }

}
