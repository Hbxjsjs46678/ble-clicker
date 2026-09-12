package com.example.bleclicker;

public class MonitorState {
    public static volatile int     rssi           = -127;
    public static volatile boolean crossed        = false;
    public static volatile boolean serviceRunning = false;
    public static volatile int     clickCount     = 0;
}
