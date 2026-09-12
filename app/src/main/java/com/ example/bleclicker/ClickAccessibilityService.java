package com.example.bleclicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class ClickAccessibilityService extends AccessibilityService {

    private static final String TAG = "ClickA11y";
    private static ClickAccessibilityService instance;

    private volatile boolean armed = false;

    public static ClickAccessibilityService getInstance() { return instance; }
    public static boolean isRunning() { return instance != null; }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;

        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
        Log.i(TAG, "无障碍服务已连接，音量键过滤已开启");
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    public void onBleCrossed(int rssi) {
        armed = true;
        SharedPreferences sp = Prefs.get(this);
        if (sp.getBoolean(Prefs.KEY_AUTO, false)) {
            clickWidget();
            armed = false;
        }
    }

    public void onBleLost() {
        armed = false;
    }

    public boolean isArmed() { return armed; }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getRepeatCount() == 0
                    && armed) {
                Log.i(TAG, "音量键触发点击");
                clickWidget();
                armed = false;
                return true;
            }
            return false;
        }
        return super.onKeyEvent(event);
    }

    private void clickWidget() {
        SharedPreferences sp = Prefs.get(this);
        float x, y;
        try {
            x = Float.parseFloat(sp.getString(Prefs.KEY_X, "540"));
            y = Float.parseFloat(sp.getString(Prefs.KEY_Y, "1200"));
        } catch (NumberFormatException e) {
            x = 540f; y = 1200f;
        }
        performClick(x, y);
        MonitorState.clickCount++;
    }

    private void performClick(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 80);

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();

        boolean ok = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription g) {
                Log.i(TAG, "点击完成");
            }
            @Override
            public void onCancelled(GestureDescription g) {
                Log.w(TAG, "点击被取消");
            }
        }, null);

        Log.i(TAG, "dispatchGesture=" + ok + " @(" + x + "," + y + ")");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }
}
