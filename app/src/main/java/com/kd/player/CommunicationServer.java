package com.kd.player;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;

import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/15 16:27:19
 * Project : taoke_android
 * Since Version : Alpha
 */
public class CommunicationServer {
    private MessageLooper<PriHandler, CommunicationEvent> messageLooper;
    private final String id;
    private final PriHandler handler;
    private final ResBundle resBundle;

    public CommunicationServer(String id, CommunicationCallback callback) {
        this.id = id;
        this.handler = new PriHandler(callback);
        this.resBundle = new ResBundle();

        this.resBundle.id = id;
    }

    public void start() {
        if (messageLooper == null) {
            messageLooper = new MessageLooper<>(handler, new MessageCallback(resBundle));
            new Thread(messageLooper).start();
            new Thread(messageLooper.beginCloseableTick(1000L, new ActivateTick(resBundle))).start();
            new Thread(messageLooper.beginProduce(new MessageLooper.ProduceChain<>(resBundle, new EventProduce()))).start();
        }
    }

    public void close() {
        if(messageLooper != null) {
            messageLooper.close();
        }
        try {
            if(resBundle.socket != null) {
                resBundle.socket.close();
            }
        } catch (Exception e) {

        }
        resBundle.isClose = true;
    }

    public interface CommunicationCallback {
        void onCommunicate(JSONObject object);
    }

    private static class PriHandler extends Handler {
        private CommunicationCallback callback;

        private PriHandler(CommunicationCallback callback) {
            super(Looper.getMainLooper());
            this.callback = callback;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.obj instanceof JSONObject) {
                callback.onCommunicate((JSONObject) msg.obj);
            }
        }
    }

    private static class ActivateTick implements MessageLooper.MessageCloseableTickCallback<CommunicationEvent> {
        private final WeakReference<ResBundle> bundleRef;

        private ActivateTick(ResBundle bundle) {
            this.bundleRef = new WeakReference<>(bundle);
        }

        @Override
        public CommunicationEvent generateTickMessage(MessageLooper.TickCloser closer) {
            ResBundle resBundle = bundleRef.get();
            if(resBundle == null || !resBundle.isInit) {
                closer.markClose();
                return null;
            }

            return new CommunicationEvent(CommunicationEvent.Event_Tick);
        }
    }

    private static class MessageCallback implements MessageLooper.MessageCallback<PriHandler, CommunicationEvent> {
        private final WeakReference<ResBundle> bundleRef;

        private MessageCallback(ResBundle bundle) {
            this.bundleRef = new WeakReference<>(bundle);
        }


        @Override
        public void handleMessage(PriHandler dataObj, CommunicationEvent message) throws Exception {
            switch (message.eventType) {
                case CommunicationEvent.Event_JSON:
                    dataObj.obtainMessage(100, message.eventData).sendToTarget();
                    break;
                case CommunicationEvent.Event_Tick: {
                    ResBundle bundle = bundleRef.get();
                    if(bundle.socket != null) {
                        JSONObject object = new JSONObject();
                        object.put("id", bundle.id);
                        object.put("type", "requestInit");
                        DatagramPacket packet = MessageUtils.transformJSON(object);
                        packet.setAddress(bundle.multicastAddr);
                        packet.setPort(bundle.multicastPort);
//                        bundle.socket.send(packet);
                    }
                    break;
                }
            }
        }
    }

    private static class ResBundle {
        boolean isClose = false;
        boolean isInit = true;
        String id;
        InetAddress multicastAddr;
        int multicastPort = 1909;
        MulticastSocket socket;
        DatagramPacket recvPacket;
    }


    private static class EventProduce implements MessageLooper.ProduceCallback<ResBundle, CommunicationEvent> {

        @Override
        public CommunicationEvent produce(ResBundle bundle) throws Exception {
            if(bundle.isClose) {
                return null;
            }
            if(bundle.socket == null) {
                bundle.multicastAddr = InetAddress.getByName("239.240.241.242");
                bundle.socket = new MulticastSocket(bundle.multicastPort);
                bundle.socket.joinGroup(bundle.multicastAddr);
                bundle.recvPacket = new DatagramPacket(new byte[1024], 1024);
            }
            Log.v("CimZzz", "start");
            bundle.socket.receive(bundle.recvPacket);
            JSONObject object = JSONObject.parseObject(new String(bundle.recvPacket.getData(), 0, bundle.recvPacket.getLength()));
            if("init".equals(object.getString("type"))) {
                bundle.isInit = false;
            }
            Log.v("CimZzz", object.toJSONString());
            return new CommunicationEvent(CommunicationEvent.Event_JSON, object);
        }
    }
}