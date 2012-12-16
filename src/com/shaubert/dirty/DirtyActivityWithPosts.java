package com.shaubert.dirty;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.shaubert.dirty.net.DirtyPostLoadRequest;
import com.shaubert.net.core.RequestStateBase;
import com.shaubert.net.nutshell.Request;
import com.shaubert.net.nutshell.RequestStatus;
import com.shaubert.util.Versions;

public class DirtyActivityWithPosts extends DirtyBaseActivity {

    public static final String EXTRA_DIRTY_SUB_BLOG_URL = "dirty-sub-blog-url";

    public static final String DIRTY_POST_LOAD_REQUEST_ID = "dirty-post-load-request-id";

    private class DirtyPostLoadRequestListener extends DefaultStatusListener {
        public DirtyPostLoadRequestListener() {
			super(DirtyActivityWithPosts.this);
		}

		@Override
        public void onFinished(Request request) {
            super.onFinished(request);
            RequestStateBase state = (RequestStateBase) request.getState();
            String gertrudaUrl = state.getString(DirtyPostLoadRequest.GERTRUDA_URL_PARAM);
            if (!TextUtils.isEmpty(gertrudaUrl)) {
                startGertrudaRefresh(gertrudaUrl);
            }
            boolean cancelledByTimeThrottle = state.getBoolean(DirtyPostLoadRequest.CANCELLED_BY_TIME_PARAM, false);
            if (!cancelledByTimeThrottle) {
                int newCount = state.getInt(DirtyPostLoadRequest.NEW_POSTS_NUMBER_PARAM, 0);
                showPostLoadedMessage(newCount);
            } else {
            	getGertruda().loadGertrudaFromCacheIfNeeded();
            }
            hideRefreshAnimation();
        }

        @Override
        public void onError(Request request) {
            super.onError(request);
            showLoadingPostsError();
            hideRefreshAnimation();
            getGertruda().loadGertrudaFromCacheIfNeeded();
        }
    }

    protected long dirtyPostLoadRequestId;
    protected DirtyPostLoadRequest dirtyPostLoadRequest;

    protected MenuItem refreshMenuItem;
	private MenuItem favoriteMenuItem;

    protected String subBlogUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        subBlogUrl = getIntent().getStringExtra(EXTRA_DIRTY_SUB_BLOG_URL);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStartInitialRequests() {
        super.onStartInitialRequests();
        
    	dirtyPostLoadRequestId = getIntent().getLongExtra(DIRTY_POST_LOAD_REQUEST_ID, -1);
    	restoreRequests();
        startLoadPostsRequestIfNotStarted(false);
    }

    @Override
    public void onRestoreRequestState(Bundle savedInstanceState) {
        super.onRestoreRequestState(savedInstanceState);
        dirtyPostLoadRequestId = savedInstanceState.getLong(DIRTY_POST_LOAD_REQUEST_ID, -1);
        restoreRequests();
    }

	private void restoreRequests() {
		if (dirtyPostLoadRequestId > 0) {
        	dirtyPostLoadRequest = restoreAndRegisterIfNotFinished(dirtyPostLoadRequestId);
        	dirtyPostLoadRequest.setFullStateChangeListener(new DirtyPostLoadRequestListener());
        }
	}
	
	protected Intent attachRequestsIds(Intent intent) {
		Intent result = super.attachRequestsIds(intent);
		result.putExtra(DIRTY_POST_LOAD_REQUEST_ID, dirtyPostLoadRequestId);
		return result;
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DIRTY_POST_LOAD_REQUEST_ID, dirtyPostLoadRequestId);
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	refreshFavoriteMenuItem();
    }
    
    protected void startLoadPostsRequestIfNotStarted(boolean force) {
        if (dirtyPostLoadRequest == null || dirtyPostLoadRequest.isCancelled() 
                || isFinished(dirtyPostLoadRequest)) {
            dirtyPostLoadRequest = new DirtyPostLoadRequest();
            if (!TextUtils.isEmpty(subBlogUrl)) {
                dirtyPostLoadRequest.getState().put(DirtyPostLoadRequest.URL_TO_LOAD_PARAM, subBlogUrl);
            }
            dirtyPostLoadRequest.getState().put(DirtyPostLoadRequest.FORCE_PARAM, force);
            dirtyPostLoadRequest.setFullStateChangeListener(new DirtyPostLoadRequestListener());
            dirtyPostLoadRequestId = startRequest(dirtyPostLoadRequest);
            showRefreshAnimation();
        }
    }

    @Override
    protected void onPause() {
    	super.onPause();
        if (isFinishing()) {
        	if (dirtyPostLoadRequest != null) {
        		unregisterForUpdates(dirtyPostLoadRequest);
        	}        	
        }
    }
    
    public void showRefreshAnimation() {
        if (!Versions.isApiLevelAvailable(11)) {
            setProgressBarIndeterminate(true);
            setProgressBarIndeterminateVisibility(true);
        } else if (refreshMenuItem != null) {
            AnimationDrawable animationDrawable = (AnimationDrawable)getResources().getDrawable(R.drawable.ic_popup_sync);
            refreshMenuItem.setIcon(animationDrawable);
            animationDrawable.start();
            animationDrawable.stop();
            animationDrawable.start();
        }
    }
    
    public void hideRefreshAnimation() {
        if (!Versions.isApiLevelAvailable(11)) {
            setProgressBarIndeterminateVisibility(false);
        } else if (refreshMenuItem != null) {
            refreshMenuItem.setIcon(R.drawable.ic_menu_refresh);
        }
    }
    
    public void showPostLoadedMessage(int newCount) {
        if (newCount > 0) {
            DirtyToast.show(this, dirtyMessagesProvider.getRandomFaceImageId(), 
                    dirtyMessagesProvider.getMessageForNewPosts(newCount));
        }
    }

    public void showLoadingPostsError() {
        DirtyToast.show(this, dirtyMessagesProvider.getRandomFaceImageId(), 
                dirtyMessagesProvider.getErrorMessage());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.main_menu, menu);
        favoriteMenuItem = menu.findItem(R.id.favorites_menu_item);
		refreshFavoriteMenuItem();
        this.refreshMenuItem = menu.findItem(R.id.refresh_posts_menu_item);
        showRefreshAnimationIfNeeded();
        return super.onCreateOptionsMenu(menu);
    }

    private void showRefreshAnimationIfNeeded() {
        if (dirtyPostLoadRequest != null 
                && RequestStatus.isWaitingOrProcessing(dirtyPostLoadRequest.getState().getStatus())) {
            showRefreshAnimation();
        }
    }
        
    private void refreshFavoriteMenuItem() {
    	if (favoriteMenuItem != null) { 
	        if (dirtyPreferences.isShowingOnlyFavorites()) {
	            favoriteMenuItem.setTitle(R.string.show_all);
	            favoriteMenuItem.setIcon(R.drawable.ic_menu_show_only_favorites);
	        } else {
	            favoriteMenuItem.setTitle(R.string.only_favorites);
	            favoriteMenuItem.setIcon(R.drawable.ic_menu_show_all);
	        }
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {        
            case R.id.refresh_posts_menu_item:                
                startLoadPostsRequestIfNotStarted(true);
                return true;
                
            case R.id.favorites_menu_item:                
                toggleShowFavorites();
                refreshFavoriteMenuItem();
                return true;
                
            case R.id.settings_menu_item:
                openSettings();
                return true;
                
            case R.id.blogs_menu_item:
            	openBlogsActivity();
            	return true;
                
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openBlogsActivity() {
        startActivity(new Intent(this, BlogsListActivity.class));
	}

	protected void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    protected void toggleShowFavorites() {
        boolean show = !dirtyPreferences.isShowingOnlyFavorites();
        dirtyPreferences.setShowOnlyFavorites(show);
    }

}
