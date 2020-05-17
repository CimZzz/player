package com.virtualightning.transport.control;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/05/17 18:00:23
 * Project : player
 * Since Version : Alpha
 */
public class ControlEvent {
    public static final int Type_Closed = 0;
    public static final int Type_Started = 1;
    public static final int Type_Send = 2;

    public static final int Type_Play_RTSP = 100;
    public static final int Type_Stop_RTSP = 101;
    public static final int Type_Play_Video = 102;
    public static final int Type_Stop_Video = 102;

    public static final ControlEvent Event_Started = new ControlEvent(Type_Started);
    public static final ControlEvent Event_Closed = new ControlEvent(Type_Closed);

    private final int eventType;
    private final Object eventData;

    public ControlEvent(int eventType) {
        this(eventType, null);
    }

    public ControlEvent(int eventType, Object eventData) {
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
