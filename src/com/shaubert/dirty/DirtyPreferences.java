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
	private String useCroutonPrefName;
    private String showAllOrMainPrefName;
    private String useSerifFontFamilyPrefName;

    public DirtyPreferences(SharedPreferences preferences, Context context) {
        this.preferences = preferences;
        
        petrPrefName = context.getString(R.string.petr_pref_key);
        fontSizePrefName = context.getString(R.string.font_size_pref_key);
        imageLoadingAlwaysPrefName = context.getString(R.string.load_images_only_with_wifi_key);
        bakgroundSyncPrefName = context.getString(R.string.posts_background_sync_key);
        bakgroundSyncIntervalPrefName = context.getString(R.string.background_sync_period_key);
        useCroutonPrefName = context.getString(R.string.use_crouron_key);
        showAllOrMainPrefName = context.getString(R.string.main_page_show_all_key);
        useSerifFontFamilyPrefName = context.getString(R.string.use_serif_font_family_key);
    }

    public long getLastViewedPostId(String subBlog) {
        return preferences.getLong(getLastPostIdKey(subBlog), -1);
    }

    private String getLastPostIdKey(String subBlog) {
        return "last-post-id" + (subBlog == null ? "" : ("-" + subBlog));
    }

    public void setLastViewedPostId(String subBlog, long id) {
        preferences.edit().putLong(getLastPostIdKey(subBlog), id).commit();
    }

    public String getLastSubBlog() {
        return preferences.getString("last-sub-blog", null);
    }

    public void setLastSubBlog(String lastSubBlog) {
        preferences.edit().putString("last-sub-blog", lastSubBlog).commit();
    }

    public long getLastListVisiblePostId(String subBlog) {
        return preferences.getLong(getLastListVisiblePostIdKey(subBlog), -1);
    }

    private String getLastListVisiblePostIdKey(String subBlog) {
        return "last-visible-post-id" + (subBlog == null ? "" : ("-" + subBlog));
    }

    public void setLastListVisiblePostId(String subBlog, long id) {
        preferences.edit().putLong(getLastListVisiblePostIdKey(subBlog), id).commit();
    }

    public boolean isShowingOnlyFavorites() {
        return preferences.getBoolean("only-favorites", false);
    }
    
    public void setShowOnlyFavorites(boolean show) {
        preferences.edit().putBoolean("only-favorites", show).commit();
    }

    public boolean isShowingOnlyFavoriteBlogs() {
        return preferences.getBoolean("only-favorite-blogs", false);
    }

    public void setShowOnlyFavoriteBlogs(boolean show) {
        preferences.edit().putBoolean("only-favorite-blogs", show).commit();
    }

    public boolean isPetrEnabled() {
        return preferences.getBoolean(petrPrefName, false);
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
        return getFontSize() * 0.85f;
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

    public boolean isUseCrouton() {
        return preferences.getBoolean(useCroutonPrefName, true);
    }

    public void setUseCrouton(boolean use) {
        preferences.edit().putBoolean(useCroutonPrefName, use).commit();
    }

    public boolean isShowAllOnMainPage() {
        return preferences.getBoolean(showAllOrMainPrefName, false);
    }

    public void setShowAllOnMainPage(boolean all) {
        preferences.edit().putBoolean(showAllOrMainPrefName, all).commit();
    }

    public boolean isUseSerifFontFamily() {
        return preferences.getBoolean(useSerifFontFamilyPrefName, true);
    }

    public void setUseSerifFontFamily(boolean serif) {
        preferences.edit().putBoolean(useSerifFontFamilyPrefName, serif).commit();
    }
}
