package com.shaubert.dirty;

import com.shaubert.util.Shlog;

import android.content.Context;
import android.content.SharedPreferences;

public class DirtyPreferences {
    
    private static final Shlog SHLOG = new Shlog(DirtyPreferences.class.getSimpleName());
    
    private final SharedPreferences preferences;
    
    private String petrPrefName;
    private String fontSizePrefName;
    private String imageLoadingAlwaysPrefName;
	private String bakgroundSyncIntervalPrefName;
	private String bakgroundSyncPrefName;

    public DirtyPreferences(SharedPreferences preferences, Context context) {
        this.preferences = preferences;
        
        petrPrefName = context.getString(R.string.petr_pref_key);
        fontSizePrefName = context.getString(R.string.font_size_pref_key);
        imageLoadingAlwaysPrefName = context.getString(R.string.load_images_only_with_wifi_key);
        bakgroundSyncPrefName = context.getString(R.string.posts_background_sync_key);
        bakgroundSyncIntervalPrefName = context.getString(R.string.background_sync_period_key);
    }

    public long getLastViewedPostId() {
        return preferences.getLong("last-post-id", -1);
    }
    
    public void setLastViewedPostId(long id) {
        preferences.edit().putLong("last-post-id", id).commit();
    }
    
    public boolean isShowingOnlyFavorites() {
        return preferences.getBoolean("only-favorites", false);
    }
    
    public void setShowOnlyFavorites(boolean show) {
        preferences.edit().putBoolean("only-favorites", show).commit();
    }
    
    public boolean isPetrEnabled() {
        return preferences.getBoolean(petrPrefName, true);
    }
    
    public int getFontSize() {
        try {
            return Integer.parseInt(preferences.getString(fontSizePrefName, "14"));
        } catch (NumberFormatException ex) {
            SHLOG.w(ex);
            return 14;
        }
    }
    
    public float getSummarySize() {
        return getFontSize() * 0.7f;
    }
    
    public boolean shouldLoadImagesOnlyWithWiFi() {
    	return !preferences.getBoolean(imageLoadingAlwaysPrefName, true);
    }
    
    public void setLoadImagesOnlyWithWiFi(boolean onlyWithWiFi) {
    	preferences.edit().putBoolean(imageLoadingAlwaysPrefName, !onlyWithWiFi).commit();
    }

    public boolean isBackgroundSyncEnabled() {
    	return preferences.getBoolean(bakgroundSyncPrefName, false);
    }
    
	public long getBackgroundSyncInterval() {
		try {
            return Long.parseLong(preferences.getString(bakgroundSyncIntervalPrefName, "86400000"));
        } catch (NumberFormatException ex) {
            SHLOG.w(ex);
            return 86400000;
        }
	}
	
	public int getNewPostsCount() {
		return preferences.getInt("new-posts-count", 0);
	}
	
	public void setNewPostsCount(int value) {
		preferences.edit().putInt("new-posts-count", value).commit();
	}
}
