package com.virtualightning.transport;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/18 18:49:59
 * Project : taoke_android
 * Since Version : Alpha
 */
public class ObjectFuture {
    private final Object locker = new Object();
    private boolean isCompleted;
    private Object result;

    public Object get() {
        synchronized (locker) {
            if(!isCompleted) {
                try { locker.wait(); } catch (Exception ignore) { }
            }
        }

        return result;
    }


    public void complete(Object data) {
        synchronized (locker) {
            if(isCompleted) {
                return;
            }
            isCompleted = true;
            result = data;
            locker.notifyAll();
        }
    }
}
