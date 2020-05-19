package com.virtualightning.transport.communication;

import com.alibaba.fastjson.JSONObject;
import com.virtualightning.transport.MessageLooper;
import com.virtualightning.transport.environment.TransportConstants;
import com.virtualightning.transport.utils.LogUtils;

import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.UUID;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/15 16:27:19
 * Project : taoke_android
 * Since Version : Alpha
 */
public class CommunicationSlaveServer {
    public static boolean ALLOW_LOG = true;
    private MessageLooper<CommunicationSlaveServer, CommunicationEvent> messageLooper;
    private final String uuid;
    private final ResBundle resBundle;

    public CommunicationSlaveServer(int screenWidth, int screenHeight, float dpi) {
        this.uuid = "slave:" + UUID.randomUUID().toString();
        this.resBundle = new ResBundle();
        this.resBundle.uuid = uuid;
        this.resBundle.screenWidth = screenWidth;
        this.resBundle.screenHeight = screenHeight;
        this.resBundle.dpi = dpi;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 4:27 PM
    // 启动 Master 广播服务器
    ///////////////////////////////////////////////////////////////////////////
    public void start() {
        if (messageLooper == null) {
            messageLooper = new MessageLooper<>(this, new MessageCallback(resBundle));
            new Thread(messageLooper).start();
            new Thread(messageLooper.beginCloseableTick(1000L, new ActivateTick())).start();
            new Thread(messageLooper.beginProduce(new MessageLooper.ProduceChain<>(resBundle, new EventProduce()))).start();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 4:28 PM
    // 关闭 Master 广播服务器
    ///////////////////////////////////////////////////////////////////////////
    public void close() {
        resBundle.isClose = true;
        if(messageLooper != null) {
            messageLooper.close();
            messageLooper = null;
        }
        if(resBundle.socket != null) {
            try {
                    resBundle.socket.close();
            } catch (Exception ignored) { }
            resBundle.socket = null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 4:47 PM
    // Communication 资源
    ///////////////////////////////////////////////////////////////////////////
    private static class ResBundle {
        boolean isClose = false;
        String uuid;
        int screenWidth;
        int screenHeight;
        float dpi;
        InetAddress multicastAddr;
        MulticastSocket socket;
        DatagramPacket recvPacket;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Modify by CimZzz on 4:38 PM
    // 定时器回调
    ///////////////////////////////////////////////////////////////////////////
    private static class ActivateTick implements MessageLooper.MessageCloseableTickCallback<CommunicationEvent> {

        @Override
        public CommunicationEvent generateTickMessage(MessageLooper.TickCloser closer) {
            return CommunicationEvent.Event_Tick;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Modify by CimZzz on 4:39 PM
    // 消息回调
    ///////////////////////////////////////////////////////////////////////////
    private static class MessageCallback implements MessageLooper.MessageCallback<CommunicationSlaveServer, CommunicationEvent> {
        private final WeakReference<ResBundle> bundleRef;
        private long refreshTick = 0;

        private MessageCallback(ResBundle bundle) {
            this.bundleRef = new WeakReference<>(bundle);
        }

        private void sendHereBroadcast(ResBundle resBundle, InetAddress inetAddress, int port) throws Exception {
            JSONObject object = new JSONObject();
            object.put("type", TransportConstants.COM_TYPE_HERE);
            object.put(TransportConstants.KEY_UUID, resBundle.uuid);
            object.put(TransportConstants.KEY_SCREEN_WIDTH, resBundle.screenWidth);
            object.put(TransportConstants.KEY_SCREEN_HEIGHT, resBundle.screenHeight);
            object.put(TransportConstants.KEY_SCREEN_DPI, resBundle.dpi);
            byte[] buffer = object.toJSONString().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            packet.setAddress(inetAddress != null ? inetAddress : resBundle.multicastAddr);
            packet.setPort(port != 0 ? port : TransportConstants.Multicast_Port);
            resBundle.socket.send(packet);
            if(ALLOW_LOG) {
                LogUtils.log("发送 Here 广播");
            }
        }

        @Override
        public void handleMessage(CommunicationSlaveServer dataObj, CommunicationEvent message) throws Exception {
            switch (message.getEventType()) {
                case CommunicationEvent.Type_Tick: {
                    refreshTick ++;
                    if(refreshTick >= 5) {
                        refreshTick = 0;
                        ResBundle resBundle = bundleRef.get();
                        if(resBundle != null) {
                            sendHereBroadcast(resBundle, null, 0);
                        }
                    }
                    break;
                }

                case CommunicationEvent.Type_Receive: {
                    JSONObject eventObj = (JSONObject) message.getEventData();
                    if(eventObj.getString("type").equals(TransportConstants.COM_TYPE_FIND)) {
                        if(ALLOW_LOG) {
                            LogUtils.log("收到 Find 广播");
                        }
                        ResBundle resBundle = bundleRef.get();
                        if(resBundle != null) {
                            sendHereBroadcast(resBundle, InetAddress.getByName(eventObj.getString("host")), eventObj.getIntValue("port"));
                        }
                    }
                    break;
                }
            }
        }
    }


    private static class EventProduce implements MessageLooper.ProduceCallback<ResBundle, CommunicationEvent> {

        @Override
        public CommunicationEvent produce(ResBundle bundle) throws Exception {
            if(bundle.isClose) {
                return null;
            }
            if(bundle.socket == null) {
                bundle.multicastAddr = InetAddress.getByName(TransportConstants.Multicast_Addr);
                bundle.socket = new MulticastSocket(TransportConstants.Multicast_Port);
                bundle.socket.joinGroup(bundle.multicastAddr);
                bundle.recvPacket = new DatagramPacket(new byte[1024], 1024);
            }
            bundle.socket.receive(bundle.recvPacket);
            JSONObject object = JSONObject.parseObject(new String(bundle.recvPacket.getData(), 0, bundle.recvPacket.getLength()));
            object.put("host", bundle.recvPacket.getAddress().getHostAddress());
            object.put("port", bundle.recvPacket.getPort());
            return new CommunicationEvent(CommunicationEvent.Type_Receive, object);
        }
    }
}