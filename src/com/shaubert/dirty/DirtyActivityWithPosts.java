package com.shaubert.dirty;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.shaubert.dirty.db.BlogsCursor;
import com.shaubert.dirty.net.DirtyPostLoadRequest;
import com.shaubert.net.core.RequestStateBase;
import com.shaubert.net.nutshell.Request;
import com.shaubert.net.nutshell.RequestStatus;
import com.shaubert.util.Versions;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class DirtyActivityWithPosts extends DirtyBaseActivity {

    public static final String EXTRA_DIRTY_SUB_BLOG_URL = "dirty-sub-blog-url";

    public static final String DIRTY_POST_LOAD_REQUEST_ID = "dirty-post-load-request-id";
    public static final int BLOG_LIST_REQUEST_CODE = 55;

    private class DirtyPostLoadRequestListener extends DefaultStatusListener {
        public DirtyPostLoadRequestListener() {
			super(DirtyActivityWithPosts.this);
		}

		@Override
        public void onFinished(Request request) {
            super.onFinished(request);
            RequestStateBase state = (RequestStateBase) request.getState();
            if (isResponseForCurrentBlog(state)) {
                String gertrudaUrl = state.getString(DirtyPostLoadRequest.GERTRUDA_URL_PARAM);
                if (!TextUtils.isEmpty(gertrudaUrl)) {
                    startGertrudaRefresh(gertrudaUrl);
                }
                boolean cancelledByTimeThrottle = state.getBoolean(DirtyPostLoadRequest.CANCELLED_BY_TIME_PARAM, false);
                if (!cancelledByTimeThrottle && !isOldState(state)) {
                    int newCount = state.getInt(DirtyPostLoadRequest.NEW_POSTS_NUMBER_PARAM, 0);
                    showPostLoadedMessage(newCount);
                } else {
                    getGertruda().loadGertrudaFromCacheIfNeeded();
                }
                hideRefreshAnimation();
            }
        }

        private boolean isOldState(RequestStateBase state) {
            long endTIme = state.getLong(DirtyPostLoadRequest.END_TIME_PARAM, System.currentTimeMillis());
            long diff = System.currentTimeMillis() - endTIme;
            return diff > 2000;
        }

        private boolean isResponseForCurrentBlog(RequestStateBase state) {
            String url = state.getString(DirtyPostLoadRequest.URL_TO_LOAD_PARAM);
            return TextUtils.equals(url, subBlogUrl);
        }

        @Override
        public void onError(Request request) {
            super.onError(request);
            if (isResponseForCurrentBlog((RequestStateBase) request.getState())) {
                showLoadingPostsError();
                hideRefreshAnimation();
                getGertruda().loadGertrudaFromCacheIfNeeded();
            }
        }
    }

    protected long dirtyPostLoadRequestId;
    protected DirtyPostLoadRequest dirtyPostLoadRequest;

    protected MenuItem refreshMenuItem;
	private MenuItem favoriteMenuItem;

    protected String subBlogUrl;
    private FavoriteBlogsDropDownAdapter favoriteBlogsDropDownAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_DIRTY_SUB_BLOG_URL)) {
            subBlogUrl = savedInstanceState.getString(EXTRA_DIRTY_SUB_BLOG_URL);
        } else if (getIntent().hasExtra(EXTRA_DIRTY_SUB_BLOG_URL)) {
            subBlogUrl = getIntent().getStringExtra(EXTRA_DIRTY_SUB_BLOG_URL);
        } else {
            subBlogUrl = dirtyPreferences.getLastSubBlog();
        }
        getGertruda().setSubBlog(subBlogUrl);

        favoriteBlogsDropDownAdapter = new FavoriteBlogsDropDownAdapter(this);
        favoriteBlogsDropDownAdapter.setAlwaysVisibleSubBlog(subBlogUrl);
        favoriteBlogsDropDownAdapter.setLoadCompleteListener(new DirtyBlogsAdapter.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(DirtyBlogsAdapter adapter) {
                favoriteBlogsDropDownAdapter.setLoadCompleteListener(null);
                if (Versions.isApiLevelAvailable(11)) {
                    getActionBar().setDisplayShowTitleEnabled(false);
                    getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                    getActionBar().setListNavigationCallbacks(favoriteBlogsDropDownAdapter, new ActionBar.OnNavigationListener() {
                        @Override
                        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                            setSubBlog(favoriteBlogsDropDownAdapter.getSubBlogUrl(itemId));
                            return true;
                        }
                    });
                }
                refreshTitle();
            }
        });
        favoriteBlogsDropDownAdapter.refresh();

        if (!TextUtils.isEmpty(subBlogUrl)) {
            setTitle(subBlogUrl);
        }
    }

    private void refreshTitle() {
        int blogPosition = favoriteBlogsDropDownAdapter.getBlogPosition(
                favoriteBlogsDropDownAdapter.getBlogId(subBlogUrl));
        if (Versions.isApiLevelAvailable(11)) {
            getActionBar().setSelectedNavigationItem(blogPosition);
        }  else {
            Object item = favoriteBlogsDropDownAdapter.getItem(
                    blogPosition);
            if (item == favoriteBlogsDropDownAdapter.MAIN_PAGE_ITEM) {
                setTitle(R.string.sub_blog_main);
            } else {
                BlogsCursor blog = (BlogsCursor) item;
                setTitle(TextUtils.isEmpty(blog.getTitle()) ? blog.getUrl() : blog.getTitle());
            }
        }
    }

    protected void setSubBlog(final String subBlogUrl) {
        if (!TextUtils.equals(this.subBlogUrl, subBlogUrl)) {
            String prevSubBlog = this.subBlogUrl;
            this.subBlogUrl = subBlogUrl;

            if (dirtyPostLoadRequest != null) {
                dirtyPostLoadRequest.cancel();
            }
            hideRefreshAnimation();

            getGertruda().setSubBlog(subBlogUrl);
            getGertruda().loadGertrudaFromCacheIfNeeded();

            favoriteBlogsDropDownAdapter.setAlwaysVisibleSubBlog(subBlogUrl);
            int blogPosition = favoriteBlogsDropDownAdapter.getBlogPosition(
                    favoriteBlogsDropDownAdapter.getBlogId(subBlogUrl));
            if (blogPosition >= 0) {
                refreshTitle();
            } else {
                favoriteBlogsDropDownAdapter.setLoadCompleteListener(new DirtyBlogsAdapter.OnLoadCompleteListener() {
                    @Override
                    public void onLoadComplete(DirtyBlogsAdapter adapter) {
                        refreshTitle();
                        favoriteBlogsDropDownAdapter.setLoadCompleteListener(null);
                    }
                });
                favoriteBlogsDropDownAdapter.refresh();
            }

            startLoadPostsRequestIfNotStarted(false);

            onSubBlogChanged(prevSubBlog);
        }
    }

    protected void onSubBlogChanged(String prevSubBlog) {
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
        outState.putString(EXTRA_DIRTY_SUB_BLOG_URL, subBlogUrl);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BLOG_LIST_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String subBlog = data.getStringExtra(PostsPagerActivity.EXTRA_DIRTY_SUB_BLOG_URL);
                setSubBlog(subBlog);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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
            if (dirtyPreferences.isUseCrouton()) {
                Crouton.clearCroutonsForActivity(this);
                Crouton.makeText(this, dirtyMessagesProvider.getSimpleMessageForNewPosts(newCount), Style.CONFIRM).show();
            } else {
                DirtyToast.show(this, dirtyMessagesProvider.getRandomFaceImageId(),
                        dirtyMessagesProvider.getMessageForNewPosts(newCount));
            }
        }
    }

    public void showLoadingPostsError() {
        if (dirtyPreferences.isUseCrouton()) {
            Crouton.clearCroutonsForActivity(this);
            Crouton.makeText(this, dirtyMessagesProvider.getSimpleErrorMessage(), Style.ALERT).show();
        } else {
            DirtyToast.show(this, dirtyMessagesProvider.getRandomFaceImageId(),
                    dirtyMessagesProvider.getErrorMessage());
        }
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
        startActivityForResult(new Intent(this, BlogsListActivity.class), BLOG_LIST_REQUEST_CODE);
	}

	protected void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    protected void toggleShowFavorites() {
        boolean show = !dirtyPreferences.isShowingOnlyFavorites();
        dirtyPreferences.setShowOnlyFavorites(show);
    }

    @Override
    protected void onDestroy() {
        Crouton.cancelAllCroutons();
        super.onDestroy();
    }
}
