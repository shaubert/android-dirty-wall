package com.shaubert.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Networks {

	public static boolean hasWiFiConnection(Context ctx) {
		ConnectivityManager connectivityManager = (ConnectivityManager) ctx
	            .getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    return info != null && info.isConnectedOrConnecting();
	}
	
	public static boolean hasInternet(Context ctx) {
	    ConnectivityManager connectivityManager = (ConnectivityManager) ctx
	            .getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = (NetworkInfo) connectivityManager.getActiveNetworkInfo();
	    return info != null && info.isConnectedOrConnecting();
	}
	
}
