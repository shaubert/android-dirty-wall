package com.shaubert.dirty;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import com.commonsware.cwac.endless.EndlessAdapter;
import com.shaubert.dirty.db.BlogsCursor;
import com.shaubert.dirty.net.DirtyBlogsLoadRequest;
import com.shaubert.net.core.RequestStateBase;
import com.shaubert.net.nutshell.Request;
import com.shaubert.util.Versions;
import com.shaubert.util.Views;


public class BlogsListActivity extends DirtyBaseActivity {

	private static final String DIRTY_BLOGS_LOAD_REQUEST_ID = "blogs-load-request";

    private class DirtyBlogsLoadRequestListener extends DefaultStatusListener {
        public DirtyBlogsLoadRequestListener() {
			super(BlogsListActivity.this);
		}

		@Override
        public void onFinished(Request request) {
            super.onFinished(request);
            allBlogsAreLoaded = ((RequestStateBase) request.getState()).getBoolean(
                    DirtyBlogsLoadRequest.END_PARAM, false);
            hideLoadingAnimation();
        }

        @Override
        public void onError(Request request) {
            super.onError(request);
            showLoadingBlogsError();
            hideLoadingAnimation();
        }
    }

	protected long dirtyBlogsLoadRequestId;
    protected DirtyBlogsLoadRequest dirtyBlogsLoadRequest;

	private ListView blogList;
	private DirtyBlogsAdapter blogsAdapter;
	private DirtyBlogsAdapter favoriteBlogsAdapter;
	private EndlessAdapter blogsEndlessAdapter;

    private boolean allBlogsAreLoaded;

    private MenuItem favoriteMenuItem;
    private MenuItem refreshMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Versions.isApiLevelAvailable(11)) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initContent();
    }

    private void initContent() {
    	ViewStub stub = (ViewStub) findViewById(R.id.content_stub);
    	stub.setLayoutResource(R.layout.l_blogs_list);
    	stub.inflate();

    	blogList = (ListView) findViewById(R.id.blogs_list);
    	blogList.setEmptyView(dirtyTv);
    	blogList.addHeaderView(Views.createVerticalSpacer(this, 10), null, false);
    	blogList.addFooterView(Views.createVerticalSpacer(this, 30), null, false);
    	blogsAdapter = new DirtyBlogsAdapter(this);
        favoriteBlogsAdapter = new DirtyBlogsAdapter(this);
        favoriteBlogsAdapter.setLoadOnlyFavorites(true);
        blogsEndlessAdapter = new EndlessAdapter(this, blogsAdapter) {
            @Override
            protected void onLoadMore() {
                tryToLoadMore();
            }
        };
        blogList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (id == DirtyBlogsAdapter.MAIN_PAGE_ID) {
                    setResultAndFinish(null);
                } else {
                    BlogsCursor blog = (BlogsCursor) blogList.getAdapter().getItem(position);
                    if (blog != null) {
                        setResultAndFinish(blog.getUrl());
                    }
                }
            }
        });
        setupListAdapter();

    	getGertruda().loadGertrudaFromCacheIfNeeded();
    }

    private void setResultAndFinish(String subBlog) {
        Intent data = new Intent();
        data.putExtra(PostsListActivity.EXTRA_DIRTY_SUB_BLOG_URL, subBlog);
        setResult(RESULT_OK, data);

        onBackPressed();
        overridePendingTransition(0, android.R.anim.slide_out_right);
    }

    private void setupListAdapter() {
        if (dirtyPreferences.isShowingOnlyFavoriteBlogs()) {
            if (blogList.getAdapter() != favoriteBlogsAdapter) {
                favoriteBlogsAdapter.refresh();
                blogList.setAdapter(favoriteBlogsAdapter);
            }
        } else {
            if (blogList.getAdapter() != blogsEndlessAdapter) {
                blogsAdapter.refresh();
                pauseEndlessAdapterBeforeLoadFinished();
                blogList.setAdapter(blogsEndlessAdapter);
            }
        }
    }

    public void showLoadingBlogsError() {
        blogsEndlessAdapter.setHasError(true);
	}

	public void hideLoadingAnimation() {
        if (!allBlogsAreLoaded) {
            pauseEndlessAdapterBeforeLoadFinished();
        } else {
            blogsEndlessAdapter.onDataReady();
            blogsEndlessAdapter.setAdapterIsFull();
        }
        hideRefreshAnimation();
    }

    private void pauseEndlessAdapterBeforeLoadFinished() {
        blogsEndlessAdapter.setAdapterIsFull();
        blogsAdapter.setLoadCompleteListener(new DirtyBlogsAdapter.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(DirtyBlogsAdapter adapter) {
                blogsAdapter.setLoadCompleteListener(null);
                blogsEndlessAdapter.onDataReady();
                blogsEndlessAdapter.restartAppending();
            }
        });
    }

    public void showLoadingAnimation() {
	}
	
	protected void tryToLoadMore() {
		startLoadBlogsRequestIfNotStarted(blogsAdapter.getCount());
	}
    
    @Override
    public void onStartInitialRequests() {
        super.onStartInitialRequests();
        
    	dirtyBlogsLoadRequestId = getIntent().getLongExtra(DIRTY_BLOGS_LOAD_REQUEST_ID, -1);
    	restoreRequests();
    }

    @Override
    public void onRestoreRequestState(Bundle savedInstanceState) {
        super.onRestoreRequestState(savedInstanceState);
        dirtyBlogsLoadRequestId = savedInstanceState.getLong(DIRTY_BLOGS_LOAD_REQUEST_ID, -1);
        restoreRequests();
    }

	private void restoreRequests() {
		if (dirtyBlogsLoadRequestId > 0) {
        	dirtyBlogsLoadRequest = restoreAndRegisterIfNotFinished(dirtyBlogsLoadRequestId);
        	dirtyBlogsLoadRequest.setFullStateChangeListener(new DirtyBlogsLoadRequestListener());
        }
	}
	
	protected Intent attachRequestsIds(Intent intent) {
		Intent result = super.attachRequestsIds(intent);
		result.putExtra(DIRTY_BLOGS_LOAD_REQUEST_ID, dirtyBlogsLoadRequestId);
		return result;
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DIRTY_BLOGS_LOAD_REQUEST_ID, dirtyBlogsLoadRequestId);
    }

    protected void startLoadBlogsRequestIfNotStarted(int from) {
        startLoadBlogsRequestIfNotStarted(from, -1);
    }

    protected void startLoadBlogsRequestIfNotStarted(int from, int to) {
        if (dirtyBlogsLoadRequest == null || dirtyBlogsLoadRequest.isCancelled() 
                || isFinished(dirtyBlogsLoadRequest)) {
            dirtyBlogsLoadRequest = new DirtyBlogsLoadRequest();
            dirtyBlogsLoadRequest.setFullStateChangeListener(new DirtyBlogsLoadRequestListener());
            dirtyBlogsLoadRequest.getState().put(DirtyBlogsLoadRequest.OFFSET_PARAM, from);
            if (to > 0) {
                dirtyBlogsLoadRequest.getState().put(DirtyBlogsLoadRequest.COUNT_PARAM, to);
            }
            dirtyBlogsLoadRequestId = startRequest(dirtyBlogsLoadRequest);
            showLoadingAnimation();
        }
    }

    @Override
    protected void onPause() {
    	super.onPause();
        if (isFinishing()) {
        	if (dirtyBlogsLoadRequest != null) {
        		unregisterForUpdates(dirtyBlogsLoadRequest);
        	}        	
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.blog_list_menu, menu);
        favoriteMenuItem = menu.findItem(R.id.favorites_menu_item);
        refreshFavoriteMenuItem();
        refreshMenuItem = menu.findItem(R.id.refresh_blogs_menu_item);
        return super.onCreateOptionsMenu(menu);
    }

    private void refreshFavoriteMenuItem() {
        if (favoriteMenuItem != null) {
            if (dirtyPreferences.isShowingOnlyFavoriteBlogs()) {
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
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.refresh_blogs_menu_item:
                reloadBlogs();
                return true;

            case R.id.favorites_menu_item:
                toggleShowFavorites();
                refreshFavoriteMenuItem();
                return true;

            case R.id.settings_menu_item:
                openSettings();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void reloadBlogs() {
        showRefreshAnimation();
        startLoadBlogsRequestIfNotStarted(0, blogsAdapter.getCount());
    }

    protected void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    protected void toggleShowFavorites() {
        boolean show = !dirtyPreferences.isShowingOnlyFavoriteBlogs();
        dirtyPreferences.setShowOnlyFavoriteBlogs(show);
        setupListAdapter();
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
}