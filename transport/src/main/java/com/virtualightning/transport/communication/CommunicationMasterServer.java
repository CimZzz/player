package com.virtualightning.transport.communication;

import android.os.SystemClock;

import com.alibaba.fastjson.JSONObject;
import com.virtualightning.transport.MessageLooper;
import com.virtualightning.transport.entity.DeviceBean;
import com.virtualightning.transport.environment.TransportConstants;
import com.virtualightning.transport.utils.LogUtils;

import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/15 16:27:19
 * Project : taoke_android
 * Since Version : Alpha
 */
public class CommunicationMasterServer {
    public static boolean ALLOW_LOG = true;
    private MessageLooper<CommunicateCallback, CommunicationEvent> messageLooper;
    private final String uuid;
    private final ResBundle resBundle;
    private final CommunicateCallback callback;

    public CommunicationMasterServer(CommunicateCallback callback) {
        this.callback = callback;
        this.uuid = "master:" + UUID.randomUUID().toString();
        this.resBundle = new ResBundle();
        this.resBundle.uuid = uuid;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 4:27 PM
    // 启动 Master 广播服务器
    ///////////////////////////////////////////////////////////////////////////
    public void start() {
        if (messageLooper == null) {
            messageLooper = new MessageLooper<>(callback, new MessageCallback(resBundle));
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
    // Add by CimZzz on 4:37 PM
    // 发送寻找设备广播
    ///////////////////////////////////////////////////////////////////////////
    public void search() {
        messageLooper.sendMessage(CommunicationEvent.Event_Find);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 4:25 PM
    // 设备发现回调
    ///////////////////////////////////////////////////////////////////////////
    public interface CommunicateCallback {
        void onFoundDevice(DeviceBean deviceBean);
        void onMissDevice(DeviceBean deviceBean);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 4:47 PM
    // Communication 资源
    ///////////////////////////////////////////////////////////////////////////
    private static class ResBundle {
        boolean isClose = false;
        String uuid;
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
    private static class MessageCallback implements MessageLooper.MessageCallback<CommunicateCallback, CommunicationEvent> {
        private final WeakReference<ResBundle> bundleRef;
        private final HashMap<String, DeviceBean> deviceMap = new HashMap<>();
        private long refreshTick = 0;
        private long deviceTick = 0;

        private MessageCallback(ResBundle bundle) {
            this.bundleRef = new WeakReference<>(bundle);
        }

        private void sendFindDeviceBroadcast(ResBundle resBundle) throws Exception {
            JSONObject object = new JSONObject();
            object.put("type", TransportConstants.COM_TYPE_FIND);
            byte[] buffer = object.toJSONString().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            packet.setAddress(resBundle.multicastAddr);
            packet.setPort(TransportConstants.Multicast_Port);
            resBundle.socket.send(packet);
            if(ALLOW_LOG) {
                LogUtils.log("发送 Find 广播");
            }
        }

        @Override
        public void handleMessage(CommunicateCallback callback, CommunicationEvent message) throws Exception {
            switch (message.getEventType()) {

                ///////////////////////////////////////////////////////////////////////////
                // Add by CimZzz on 4:46 PM
                // 定时器触发回调
                ///////////////////////////////////////////////////////////////////////////
                case CommunicationEvent.Type_Tick: {
                    // 5 秒一次自动发送发现设备广播
                    refreshTick += 1;
                    if (refreshTick >= 5) {
                        refreshTick = 0;
                        ResBundle resBundle = bundleRef.get();
                        if (resBundle != null) {
                            sendFindDeviceBroadcast(resBundle);
                        }
                    }
                    // 10 秒一次检查设备激活时间
                    deviceTick += 1;
                    if (deviceTick >= 10) {
                        deviceTick = 0;
                        Iterator<Map.Entry<String, DeviceBean>> iterable = deviceMap.entrySet().iterator();
                        long curTime = SystemClock.elapsedRealtime();
                        while (iterable.hasNext()) {
                            Map.Entry<String, DeviceBean> entry = iterable.next();
                            DeviceBean deviceBean = entry.getValue();
                            if (curTime - entry.getValue().getActivateTime() >= TransportConstants.Device_Expire_Time) {
                                callback.onMissDevice(deviceBean);
                                iterable.remove();
                                if(ALLOW_LOG) {
                                    LogUtils.log("设备 " + entry.getKey() + " 已过期");
                                }
                            }
                        }
                    }
                    break;
                }

                ///////////////////////////////////////////////////////////////////////////
                // Add by CimZzz on 4:46 PM
                // 发送搜索设备广播
                ///////////////////////////////////////////////////////////////////////////
                case CommunicationEvent.Type_Find: {
                    ResBundle resBundle = bundleRef.get();
                    if (resBundle != null) {
                        sendFindDeviceBroadcast(resBundle);
                    }
                    break;
                }

                ///////////////////////////////////////////////////////////////////////////
                // Add by CimZzz on 4:48 PM
                // 收到广播回调
                ///////////////////////////////////////////////////////////////////////////
                case CommunicationEvent.Type_Receive: {
                    JSONObject eventObj = (JSONObject) message.getEventData();
                    switch (eventObj.getString("type")) {
                        case TransportConstants.COM_TYPE_HERE: {
                            // 发现设备
                            String deviceId = eventObj.getString(TransportConstants.KEY_UUID);
                            DeviceBean deviceBean = deviceMap.get(deviceId);
                            if (deviceBean != null) {
                                deviceBean.setActivateTime(SystemClock.elapsedRealtime());
                                if(ALLOW_LOG) {
                                    LogUtils.log("设备 " + deviceId + " 已存在，更新时间");
                                }
                            } else {
                                deviceBean = new DeviceBean();
                                deviceBean.setId(deviceId);
                                deviceBean.setHost(eventObj.getString("host"));
                                deviceBean.setPort(eventObj.getIntValue("port"));
                                deviceBean.setActivateTime(SystemClock.elapsedRealtime());
                                deviceMap.put(deviceId, deviceBean);
                                // 发送通知注册新设备
                                callback.onFoundDevice(deviceBean);
                                if(ALLOW_LOG) {
                                    LogUtils.log("发现新设备 " + deviceId);
                                }
                            }
                            break;
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
            object.put("host", bundle.recvPacket.getAddress().getHostName());
            object.put("port", bundle.recvPacket.getPort());
            return new CommunicationEvent(CommunicationEvent.Type_Receive, object);
        }
    }
}