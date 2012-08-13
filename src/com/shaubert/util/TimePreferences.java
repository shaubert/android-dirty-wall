package com.shaubert.util;

import android.content.SharedPreferences;

public class TimePreferences {

    private SharedPreferences preferences;
    private final String prefName;
    private final long threshold;

    public TimePreferences(SharedPreferences preferences, String prefName, long threshold) {
        this.preferences = preferences;
        this.prefName = prefName;
        this.threshold = threshold;
    }
    
    public boolean shouldPerformOperation() {
        long curTime = System.currentTimeMillis();
        long lastTime = preferences.getLong(prefName, curTime);
        return curTime <= lastTime || curTime - lastTime > threshold;  
    }
    
    public void commit() {
        preferences.edit().putLong(prefName, System.currentTimeMillis()).commit();
    }
    
}
