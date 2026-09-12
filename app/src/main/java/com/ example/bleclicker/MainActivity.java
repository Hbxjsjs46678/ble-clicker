package com.example.bleclicker;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvThreshold, tvHysteresis, tvLive;
    private SeekBar  sbThreshold, sbHysteresis;
    private EditText etMac, etX, etY;
    private CheckBox cbAuto;
    private Button   btnStart, btnStop, btnA11y, btnSave;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int REQ_PERM = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus     = findViewById(R.id.tvStatus);
        tvThreshold  = findViewById(R.id.tvThreshold);
        tvHysteresis = findViewById(R.id.tvHysteresis);
        tvLive       = findViewById(R.id.tvLive);
        sbThreshold  = findViewById(R.id.sbThreshold);
        sbHysteresis = findViewById(R.id.sbHysteresis);
        etMac        = findViewById(R.id.etMac);
        etX          = findViewById(R.id.etX);
        etY          = findViewById(R.id.etY);
        cbAuto       = findViewById(R.id.cbAuto);
        btnStart     = findViewById(R.id.btnStart);
        btnStop      = findViewById(R.id.btnStop);
        btnA11y      = findViewById(R.id.btnA11y);
        btnSave      = findViewById(R.id.btnSave);

        loadPrefs();

        sbThreshold.setMax(70);
        sbThreshold.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {
                tvThreshold.setText("触发阈值：" + (-100 + p) + " dBm");
            }
        });

        sbHysteresis.setMax(20);
        sbHysteresis.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {
                tvHysteresis.setText("滞回区间：" + p + " dBm");
            }
        });

        btnSave.setOnClickListener(v -> { savePrefs(); toast("参数已保存"); });

        btnStart.setOnClickListener(v -> {
            savePrefs();
            if (!hasBlePermission()) { requestPermissions(); return; }
            BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
            if (a == null || !a.isEnabled()) { toast("请先开启蓝牙"); return; }
            if (!ClickAccessibilityService.isRunning()) {
                toast("请先开启无障碍服务");
                openA11ySettings();
                return;
            }
            startForegroundService(new Intent(this, BleScanService.class));
            toast("监测已启动");
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, BleScanService.class));
            toast("监测已停止");
        });

        btnA11y.setOnClickListener(v -> openA11ySettings());

        requestPermissions();
    }

    private void openA11ySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            toast("无法打开无障碍设置");
        }
    }

    private boolean hasBlePermission() {
        List<String> need = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            need.add(Manifest.permission.BLUETOOTH_SCAN);
            need.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            need.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        for (String p : need) {
            if (ContextCompat.checkSelfPermission(this, p)
                    != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private void requestPermissions() {
        List<String> need = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            need.add(Manifest.permission.BLUETOOTH_SCAN);
            need.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            need.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            need.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> missing = new ArrayList<>();
        for (String p : need) {
            if (ContextCompat.checkSelfPermission(this, p)
                 
