package com.shaubert.util;

import android.os.Build;

public class Versions {

    public static boolean isApiLevelAvailable(int level) {
        return Build.VERSION.SDK_INT >= level; 
    }
    
}
