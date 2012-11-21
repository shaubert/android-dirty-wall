package com.shaubert.dirty;

import android.app.Application;
import android.preference.PreferenceManager;

import com.shaubert.util.Shlog;

public class DirtyApp extends Application {

	public static final Shlog SHLOG = new Shlog(DirtyApp.class.getSimpleName());
	
	private DirtyPreferences dirtyPreferences;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		this.dirtyPreferences = new DirtyPreferences(
				PreferenceManager.getDefaultSharedPreferences(this), this);
		setupBackroundSync();
	}

	private void setupBackroundSync() {
		SHLOG.d("setupBackroundSync()");
		if (dirtyPreferences.isBackgroundSyncEnabled()) {
			BackgroundPostLoaderReceiver.scheduleSync(this, 
					dirtyPreferences.getBackgroundSyncInterval());
		}
	}
	
}
