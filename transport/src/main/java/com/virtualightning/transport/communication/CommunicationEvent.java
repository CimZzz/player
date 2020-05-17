package com.virtualightning.transport.communication;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/05/17 16:30:35
 * Project : player
 * Since Version : Alpha
 */
public class CommunicationEvent {
    public static final int Type_Find = 0;
    public static final int Type_Tick = 1;
    public static final int Type_Receive = 2;

    public static final CommunicationEvent Event_Tick = new CommunicationEvent(Type_Tick);
    public static final CommunicationEvent Event_Find = new CommunicationEvent(Type_Find);

    private final int eventType;
    private final Object eventData;

    public CommunicationEvent(int eventType) {
        this(eventType, null);
    }

    public CommunicationEvent(int eventType, Object eventData) {
        this.eventType = eventType;
        this.eventData = eventData;
    }

    public int getEventType() {
        return eventType;
    }

    public Object getEventData() {
        return eventData;
    }
}
