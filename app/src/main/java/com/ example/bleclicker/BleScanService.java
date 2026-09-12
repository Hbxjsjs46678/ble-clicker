package com.example.bleclicker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.LinkedList;

public class BleScanService extends Service {

    private static final String TAG = "BleScanService";
    private static final String CHANNEL_ID = "ble_monitor";
    private static final int NOTIF_ID = 1001;

    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    private boolean scanning = false;

    private final LinkedList<Integer> buffer = new LinkedList<>();
    private static final int BUF_SIZE = 5;

    private boolean crossed = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = buildNotification("正在监测蓝牙信号…");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        } else {
            startForeground(NOTIF_ID, n);
        }

        MonitorState.serviceRunning = true;
        startScan();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopScan();
        MonitorState.serviceRunning = false;
        MonitorState.crossed = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void startScan() {
        if (scanning) return;

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Log.e(TAG, "蓝牙不可用");
            return;
        }
        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            Log.e(TAG, "scanner 为空");
            return;
        }

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                String mac = result.getDevice().getAddress();
                String target = Prefs.get(BleScanService.this)
                        .getString(Prefs.KEY_MAC, "");
                if (target != null && !target.trim().isEmpty()
                        && !target.trim().equalsIgnoreCase(mac)) {
                    return;
                }
                handleRssi(result.getRssi());
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "扫描失败 errorCode=" + errorCode);
            }
        };

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();

        try {
            scanner.startScan(null, settings, scanCallback);
            scanning = true;
            Log.i(TAG, "BLE 扫描已启动");
        } catch (Exception e) {
            Log.e(TAG, "startScan 异常", e);
        }
    }

    private void stopScan() {
        if (scanner != null && scanCallback != null && scanning) {
            try { scanner.stopScan(scanCallback); } catch (Exception ignored) {}
        }
        scanning = false;
    }

    private void handleRssi(int rssi) {
        buffer.addLast(rssi);
        while (buffer.size() > BUF_SIZE) buffer.removeFirst();

        int sum = 0;
        for (int v : buffer) sum += v;
        int smooth = Math.round((float) sum / buffer.size());

        SharedPreferences sp = Prefs.get(this);
        int threshold  = sp.getInt(Prefs.KEY_THRESHOLD,  -70);
        int hysteresis = sp.getInt(Prefs.KEY_HYSTERESIS, 5);

        MonitorState.rssi = smooth;

        ClickAccessibilityService svc = ClickAccessibilityService.getInstance();

        if (!crossed && smooth >= threshold) {
            crossed = true;
            MonitorState.crossed = true;
            if (svc != null) svc.onBleCrossed(smooth);
            Log.i(TAG, "越过阈值 smooth=" + smooth + " threshold=" + threshold);
        } else if (crossed && smooth < threshold - hysteresis) {
            crossed = false;
            MonitorState.crossed = false;
            if (svc != null) svc.onBleLost();
            Log.i(TAG, "信号回落 smooth=" + smooth);
        }

        updateNotification(smooth);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "蓝牙信号
