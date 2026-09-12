package com.example.bleclicker;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static final String NAME = "ble_clicker_prefs";

    public static final String KEY_MAC        = "mac";
    public static final String KEY_THRESHOLD  = "threshold";
    public static final String KEY_HYSTERESIS = "hysteresis";
    public static final String KEY_X          = "x";
    public static final String KEY_Y          = "y";
    public static final String KEY_AUTO       = "auto_click";

    public static SharedPreferences get(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }
}
