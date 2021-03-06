package com.virtualightning.transport;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageLooper<T, E> implements Runnable  {
    private final WeakReference<T> baseObj;
    private final ConcurrentLinkedQueue<E> messageQueue;
    private final MessageCallback<T, E> callback;
    private MessageRefuseCallback<E> messageRefuseCallback;
    private final Object locker = new Object();
    private boolean isRunning = true;
    private boolean isCloseUntilCompleted = false;

    private List<TickRunnable<T, E>> tickRunnableList = new LinkedList<>();
    private List<ProduceRunnable<E>> produceRunnableList = new LinkedList<>();

    public MessageLooper(T baseObj, MessageCallback<T, E> callback) {
        this.baseObj = new WeakReference<>(baseObj);
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.callback = callback;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:24
    // 发送消息给 MessageLooper
    ///////////////////////////////////////////////////////////////////////////
    public void sendMessage(E message) {
        synchronized (locker) {
            if(!isRunning) {
                if(messageRefuseCallback != null) {
                    try { messageRefuseCallback.refuseMessage(message); } catch (Exception ignore) { }
                }
                return;
            }
            messageQueue.add(message);
            locker.notifyAll();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:24
    // 设置拒绝消息回调
    ///////////////////////////////////////////////////////////////////////////
    public void setMessageRefuseCallback(MessageRefuseCallback<E> messageRefuseCallback) {
        this.messageRefuseCallback = messageRefuseCallback;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:24
    // 启动周期定时器
    ///////////////////////////////////////////////////////////////////////////
    public Runnable beginTick(long periodTime, MessageTickCallback<E> tickCallback) {
        if(!isRunning) {
            return null;
        }
        TickRunnable<T, E> tickRunnable = new TickRunnable<>(this, tickCallback, periodTime);
        tickRunnableList.add(tickRunnable);
        return tickRunnable;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:24
    // 启动延迟定时器，只执行一次
    ///////////////////////////////////////////////////////////////////////////
    public Runnable beginDelay(long periodTime, MessageTickCallback<E> tickCallback) {
        if(!isRunning) {
            return null;
        }
        DelayRunnable<T, E> tickRunnable = new DelayRunnable<>(this, tickCallback, periodTime);
        tickRunnableList.add(tickRunnable);
        return tickRunnable;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:25
    // 启动可关闭的周期定时器
    ///////////////////////////////////////////////////////////////////////////
    public Runnable beginCloseableTick(long periodTime, MessageCloseableTickCallback<E> tickCallback) {
        if(!isRunning) {
            return null;
        }
        CloseableTickRunnable<T, E> tickRunnable = new CloseableTickRunnable<>(this, tickCallback, periodTime);
        tickRunnableList.add(tickRunnable);
        return tickRunnable;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:25
    // 启动消息生产者
    ///////////////////////////////////////////////////////////////////////////
    public Runnable beginProduce(ProduceChain<?, E> chain) {
        if(!isRunning) {
            return null;
        }
        ProduceRunnable<E> produceRunnable = new ProduceRunnable<>(this, chain);
        produceRunnableList.add(produceRunnable);
        return produceRunnable;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:25
    // 启动消息列表生产者，会以 List 的信息返回消息，并依次解决
    ///////////////////////////////////////////////////////////////////////////
    public Runnable beginProduce(ListProduceChain<?, E> chain) {
        if(!isRunning) {
            return null;
        }
        ProduceRunnable<E> produceRunnable = new ProduceRunnable<>(this, chain);
        produceRunnableList.add(produceRunnable);
        return produceRunnable;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:26
    // 清空目前全部未执行消息
    ///////////////////////////////////////////////////////////////////////////
    public void clear() {
        messageQueue.clear();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:26
    // 关闭 MessageLooper，不再接受消息，但是会继续执行完剩余全部待处理的消息
    ///////////////////////////////////////////////////////////////////////////
    public void closeUntilMessageHandleCompleted() {
        synchronized (locker) {
            isCloseUntilCompleted = true;
            isRunning = false;
            for(TickRunnable<T, E> runnable : tickRunnableList) {
                runnable.close();
            }
            for(ProduceRunnable<E> runnable : produceRunnableList) {
                runnable.close();
            }
            tickRunnableList.clear();
            locker.notifyAll();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:27
    // 关闭 MessageLooper，不会继续执行剩余消息
    ///////////////////////////////////////////////////////////////////////////
    public void close() {
        messageQueue.clear();
        synchronized (locker) {
            isCloseUntilCompleted = false;
            isRunning = false;
            for(TickRunnable<T, E> runnable : tickRunnableList) {
                runnable.close();
            }
            for(ProduceRunnable<E> runnable : produceRunnableList) {
                runnable.close();
            }
            tickRunnableList.clear();
            locker.notifyAll();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:27
    // MessageLooper 执行回调
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void run() {
        while(isRunning) {
            while((isRunning || !isCloseUntilCompleted) && !messageQueue.isEmpty()) {
                final E message = messageQueue.poll();
                final T dataObj = baseObj.get();
                if (dataObj == null) {
                    isRunning = false;
                    break;
                }
                try {
                    this.callback.handleMessage(dataObj, message);
                } catch (Exception e) { }
            }

            synchronized (locker) {
                if(!isRunning) {
                    break;
                }
                try {
                    locker.wait();
                } catch (Exception e) {
                }
            }
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:27
    // MessageLooper 消息回调
    ///////////////////////////////////////////////////////////////////////////
    public interface MessageCallback<T, E> {
        void handleMessage(T dataObj, E message) throws Exception;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:28
    // MessageLooper 定时器执行回调
    ///////////////////////////////////////////////////////////////////////////
    public interface MessageTickCallback<E> {
        E generateTickMessage();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:28
    // MessageLooper 可关闭定时器执行回调
    ///////////////////////////////////////////////////////////////////////////
    public interface MessageCloseableTickCallback<E> {
        E generateTickMessage(TickCloser closer);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:28
    // MessageLooper 拒绝消息回调
    ///////////////////////////////////////////////////////////////////////////
    public interface MessageRefuseCallback<E> {
        void refuseMessage(E message) throws Exception;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:28
    // 定时器方法循环
    ///////////////////////////////////////////////////////////////////////////
    private static class TickRunnable<T, E> implements Runnable {
        final WeakReference<MessageLooper<T, E>> messageLooperRef;
        final MessageTickCallback<E> tickCallback;
        final long periodTime;
        long lastTime = -1L;
        boolean isRunning = true;

        private TickRunnable(MessageLooper<T, E> messageLooper, MessageTickCallback<E> tickCallback, long periodTime) {
            this.messageLooperRef = new WeakReference<>(messageLooper);
            this.tickCallback = tickCallback;
            this.periodTime = periodTime;
        }

        public void close() {
            isRunning = false;
            messageLooperRef.clear();
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    long currentTime = System.currentTimeMillis();
                    if(lastTime == -1L) {
                        lastTime = currentTime;
                    }
                    else if(currentTime - lastTime >= periodTime) {
                        lastTime = currentTime;
                        E message = tickCallback.generateTickMessage();
                        MessageLooper<T, E> messageLooper = messageLooperRef.get();
                        if (messageLooper == null) {
                            isRunning = false;
                            break;
                        }

                        if (message != null) {
                            messageLooper.sendMessage(message);
                        }
                    }

                    long sleepTime = periodTime / 10;
                    Thread.sleep(sleepTime == 0 ? 1L : sleepTime);

                }
                catch (Exception ignored) { }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:29
    // 延时定时器方法循环
    ///////////////////////////////////////////////////////////////////////////
    private static class DelayRunnable<T, E> extends TickRunnable<T, E> {
        private DelayRunnable(MessageLooper<T, E> messageLooper, MessageTickCallback<E> tickCallback, long periodTime) {
            super(messageLooper, tickCallback, periodTime);
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    long currentTime = System.currentTimeMillis();
                    if(lastTime == -1L) {
                        lastTime = currentTime;
                    }
                    else if(currentTime - lastTime >= periodTime) {
                        lastTime = currentTime;
                        E message = tickCallback.generateTickMessage();
                        MessageLooper<T, E> messageLooper = messageLooperRef.get();
                        if (messageLooper == null) {
                            isRunning = false;
                            break;
                        }

                        if (message != null) {
                            messageLooper.sendMessage(message);
                        }

                        close();
                        return;
                    }

                    long sleepTime = periodTime / 10;
                    Thread.sleep(sleepTime == 0 ? 1L : sleepTime);

                }
                catch (Exception ignored) { }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:29
    // 定时器关闭器
    ///////////////////////////////////////////////////////////////////////////
    public static class TickCloser {
        private boolean isClosed;

        public void markClose() {
            isClosed = true;
        }

        public boolean isClosed() {
            return isClosed;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/20 上午11:29
    // 可关闭定时器关闭回调
    ///////////////////////////////////////////////////////////////////////////
    private static class CloseableTickRunnable<T, E> extends TickRunnable<T, E> {
        private final MessageCloseableTickCallback<E> tickCallback;
        private final TickCloser tickCloser = new TickCloser();

        private CloseableTickRunnable(MessageLooper<T, E> messageLooper, MessageCloseableTickCallback<E> tickCallback, long periodTime) {
            super(messageLooper, null, periodTime);
            this.tickCallback = tickCallback;
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    long currentTime = System.currentTimeMillis();
                    if(lastTime == -1L) {
                        lastTime = currentTime;
                    }
                    else if(currentTime - lastTime >= periodTime) {
                        lastTime = currentTime;
                        E message = tickCallback.generateTickMessage(tickCloser);
                        if(tickCloser.isClosed()) {
                            close();
                            break;
                        }
                        MessageLooper<T, E> messageLooper = messageLooperRef.get();
                        if (messageLooper == null) {
                            close();
                            break;
                        }

                        if (message != null) {
                            messageLooper.sendMessage(message);
                        }
                    }

                    long sleepTime = periodTime / 10;
                    Thread.sleep(sleepTime == 0 ? 1L : sleepTime);

                }
                catch (Exception ignored) { }
            }
        }
    }

    public interface ProduceCallback<A, B> {
        B produce(A obj) throws Exception;
    }

    public interface ListProduceCallback<A, B> {
        List<B> produce(A obj) throws Exception;
    }

    public interface ChildProduceCallback<A, B, C> {
        B produce(A obj, C data) throws Exception;
    }

    public interface ListChildProduceCallback<A, B, C> {
        List<B> produce(A obj, C data) throws Exception;
    }

    public static class ProduceChain<A, B> {
        private final WeakReference<A> objectRef;
        private final ProduceCallback<A, B> callback;

        public ProduceChain(A dataObject, ProduceCallback<A, B> callback) {
            this.objectRef = new WeakReference<>(dataObject);
            this.callback = callback;
        }

        public final <C, D> ProduceChain<C, D> nextChain(C dataObject, ChildProduceCallback<C, D, B> callback) {
            return new ChildProduceChain<>(dataObject, this, callback);
        }

        public final <C, D> ListProduceChain<C, D> nextListChain(C dataObject, ListChildProduceCallback<C, D, B> callback) {
            return new ListChildProduceChain<C, D, B>(dataObject, this, callback);
        }

        B produce() throws Exception {
            A obj = getObj();
            if(obj == null) {
                return null;
            }
            return callback.produce(obj);
        };

        void close() {
            objectRef.clear();
        }

        final A getObj() {
            return objectRef.get();
        }

    }

    private static class ChildProduceChain<A, B, C> extends ProduceChain<A, B> {
        private final ChildProduceCallback<A, B, C> callback;
        private final ProduceChain<?, C> parentChain;
        ChildProduceChain(A dataObject, ProduceChain<?, C> parentChain, ChildProduceCallback<A, B, C> callback) {
            super(dataObject, null);
            this.parentChain = parentChain;
            this.callback = callback;
        }

        @Override
        B produce() throws Exception {
            C message = parentChain.produce();
            if(message == null) {
                return null;
            }
            A obj = getObj();
            if(obj == null) {
                return null;
            }
            return callback.produce(obj, message);
        }

        @Override
        void close() {
            parentChain.close();
            super.close();
        }
    }


    public static class ListProduceChain<A, B> extends ProduceChain<A, List<B>>  {
        private final ListProduceCallback<A, B> callback;
        public ListProduceChain(A dataObject, ListProduceCallback<A, B> callback) {
            super(dataObject, null);
            this.callback = callback;
        }

        @Override
        List<B> produce() throws Exception {
            A obj = getObj();
            if(obj == null) {
                return null;
            }
            return callback.produce(obj);
        }
    }

    private static class ListChildProduceChain<A, B, C> extends ListProduceChain<A, B> {
        private final ListChildProduceCallback<A, B, C> callback;
        private final ProduceChain<?, C> parentChain;
        ListChildProduceChain(A dataObject, ProduceChain<?, C> parentChain, ListChildProduceCallback<A, B, C> callback) {
            super(dataObject, null);
            this.parentChain = parentChain;
            this.callback = callback;
        }

        @Override
        List<B> produce() throws Exception {
            C message = parentChain.produce();
            if(message == null) {
                return null;
            }
            A obj = getObj();
            if(obj == null) {
                return null;
            }
            return callback.produce(obj, message);
        }

        @Override
        void close() {
            parentChain.close();
            super.close();
        }
    }

    private static class ProduceRunnable<E> implements Runnable {
        private final WeakReference<MessageLooper<?, E>> messageLooperRef;
        private final ProduceChain<?, E> produceChain;
        private final ProduceChain<?, List<E>> listProduceChain;
        private boolean isRunning = true;

        private ProduceRunnable(MessageLooper<?, E> messageLooper, ProduceChain<?, E> produceChain) {
            this.messageLooperRef = new WeakReference<MessageLooper<?, E>>(messageLooper);
            this.produceChain = produceChain;
            this.listProduceChain = null;
        }

        private ProduceRunnable(MessageLooper<?, E> messageLooper, ListProduceChain<?, E> produceChain) {
            this.messageLooperRef = new WeakReference<MessageLooper<?, E>>(messageLooper);
            this.produceChain = null;
            this.listProduceChain = produceChain;
        }

        public void close() {
            isRunning = false;
            messageLooperRef.clear();
            if(produceChain != null) {
                produceChain.close();
            }
            if(listProduceChain != null) {
                listProduceChain.close();
            }
        }


        @Override
        public void run() {
            while (isRunning) {
                try {
                    if(produceChain != null) {
                        E message = produceChain.produce();
                        MessageLooper<?, E> messageLooper = messageLooperRef.get();
                        if (messageLooper == null) {
                            isRunning = false;
                            break;
                        }
                        if (message != null) {
                            messageLooper.sendMessage(message);
                        }
                    }
                    else if(listProduceChain != null) {
                        List<E> messageList = listProduceChain.produce();
                        MessageLooper<?, E> messageLooper = messageLooperRef.get();
                        if (messageLooper == null) {
                            isRunning = false;
                            break;
                        }
                        if(messageList != null) {
                            for(E message : messageList) {
                                messageLooper.sendMessage(message);
                            }
                        }
                    }
                }
                catch (Exception ignore) { }
            }
        }
    }
}
