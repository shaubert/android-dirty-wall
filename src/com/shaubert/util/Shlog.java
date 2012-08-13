package com.shaubert.util;

import android.os.SystemClock;
import android.util.Log;

import java.util.HashMap;

public class Shlog {

    public static final int LOG_LEVEL = Log.VERBOSE;
    
    private final String tag;
    private HashMap<String, Long> timers;

    public Shlog(String tag) {
        this.tag = tag;
        timers = new HashMap<String, Long>();
    }

    public boolean isLoggable(int level) {
        return level >= LOG_LEVEL;
    }
    
    public void v(String msg) {
        if (Log.VERBOSE >= LOG_LEVEL) {
            Log.v(tag, msg);
        }
    }

    public void v(String msg, Throwable tr) {
        if (Log.VERBOSE >= LOG_LEVEL) {
            Log.v(tag, msg, tr);
        }
    }

    public void d(String msg) {
        if (Log.DEBUG >= LOG_LEVEL) {
            Log.d(tag, msg);
        }
    }

    public void d(String msg, Throwable tr) {
        if (Log.DEBUG >= LOG_LEVEL) {
            Log.d(tag, msg, tr);
        }
    }

    public void i(String msg) {
        if (Log.INFO >= LOG_LEVEL) {
            Log.i(tag, msg);
        }
    }

    public void i(String msg, Throwable tr) {
        if (Log.INFO >= LOG_LEVEL) {
            Log.i(tag, msg, tr);
        }
    }

    public void w(String msg) {
        if (Log.WARN >= LOG_LEVEL) {
            Log.w(tag, msg);
        }
    }

    public void w(String msg, Throwable tr) {
        if (Log.WARN >= LOG_LEVEL) {
            Log.w(tag, msg, tr);
        }
    }

        
    public void w(Throwable tr) {
        if (Log.WARN >= LOG_LEVEL) {
            Log.w(tag, tr);
        }
    }

    public void e(String msg) {
        if (Log.ERROR >= LOG_LEVEL) {
            Log.e(tag, msg);
        }
    }

    public void e(String msg, Throwable tr) {
        if (Log.ERROR >= LOG_LEVEL) {
            Log.e(tag, msg, tr);
        }
    }
    
    public void resetTimer(String timerName) {
        timers.put(timerName, SystemClock.uptimeMillis());
    }
    
    public void logTimer(String timerName) {
        Long startTime = timers.get(timerName);
        if (startTime != null) {
            long diff = SystemClock.uptimeMillis() - startTime;
            d(timerName + " = " + diff + "ms");
        }
    }
    
}