package com.shaubert.dirty;

import android.app.Application;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;

import android.support.v4.util.LruCache;
import com.shaubert.dirty.client.DirtyBlog;
import com.shaubert.util.Shlog;

public class DirtyApp extends Application {

	public static final Shlog SHLOG = new Shlog(DirtyApp.class.getSimpleName());

    private LruCache<String, Bitmap> imageCache = new LruCache<String, Bitmap>(5 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value == null ? 0 : value.getRowBytes() * value.getHeight();
        }
    };

	private DirtyPreferences dirtyPreferences;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		this.dirtyPreferences = new DirtyPreferences(
				PreferenceManager.getDefaultSharedPreferences(this), this);
        DirtyBlog.init(dirtyPreferences);
		setupBackroundSync();
	}

	private void setupBackroundSync() {
		SHLOG.d("setupBackroundSync()");
		if (dirtyPreferences.isBackgroundSyncEnabled()) {
			BackgroundPostLoaderReceiver.scheduleSync(this, 
					dirtyPreferences.getBackgroundSyncInterval());
		}
	}

    public LruCache<String, Bitmap> getImageCache() {
        return imageCache;
    }
}
