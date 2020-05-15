package com.kd.player;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/5/15 17:58:27
 * Project : taoke_android
 * Since Version : Alpha
 */
public class CommunicationEvent {
    public static final int Event_JSON = 0;
    public static final int Event_Tick = 1;

    public final int eventType;
    public final Object eventData;

    public CommunicationEvent(int eventType) {
        this(eventType, null);
    }

    public CommunicationEvent(int eventType, Object eventData) {
        this.eventType = eventType;
        this.eventData = eventData;
    }
}
