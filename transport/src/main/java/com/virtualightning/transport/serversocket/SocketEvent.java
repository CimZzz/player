package com.virtualightning.transport.serversocket;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/18 16:04:22
 * Project : taoke_android
 * Since Version : Alpha
 */
public class SocketEvent {
    public static final int Type_Close = 0;
    public static final int Type_Socket = 1;
    public static final int Type_Socket_Close = 2;
    public static final int Type_Modify_Resource = 3;
    public static final int Type_Get_Resource = 4;
    public static final int Type_Send = 5;
    public static final int Type_Response = 6;


    private final int eventType;
    private final Object eventData;
    private final Object otherData;

    public SocketEvent(int eventType) {
        this(eventType, null);
    }

    public SocketEvent(int eventType, Object eventData) {
        this(eventType, eventData, null);
    }

    public SocketEvent(int eventType, Object eventData, Object otherData) {
        this.eventType = eventType;
        this.eventData = eventData;
        this.otherData = otherData;
    }

    public int getEventType() {
        return eventType;
    }

    public Object getEventData() {
        return eventData;
    }

    public Object getOtherData() {
        return otherData;
    }
}
