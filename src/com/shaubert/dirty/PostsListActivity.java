package com.shaubert.dirty;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.shaubert.dirty.DirtyPostCompactAdapter.OnLoadCompleteListener;
import com.shaubert.util.DatabaseCleaner;
import com.shaubert.widget.FasterScrollerView;
import com.shaubert.util.Files;
import com.shaubert.util.Views;

public class PostsListActivity extends DirtyActivityWithPosts {

	private static final int PAGER_REQUEST_CODE = 41;
	public static final String EXTRA_FROM_NOTIFICATION = "from-notification";

    private static final String LAST_TOP_POST_ID = "last-top-post-id";

    private ListView postsCompactList;
    private DirtyPostCompactAdapter postCompactAdapter;

	private boolean isPaused;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initContent();
        
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(DirtyNewPostsStatusBarNotification.NEW_POSTS_NOTIFICATION_ID);
        dirtyPreferences.setNewPostsCount(0);
        
        if (savedInstanceState == null && !getIntent().hasExtra(EXTRA_DIRTY_SUB_BLOG_URL)) {
            Files.startCleanUpCacheIfNeeded(this, Files.PREFFERED_MAX_CACHE_SIZE);
            DatabaseCleaner.startCleanUpIfNeeded(this);
        }

        if (savedInstanceState == null) {
            restoreLastViewedPost();
        } else {
            final long postId = savedInstanceState.getLong(LAST_TOP_POST_ID, -1);
            if (postId >= 0) {
                postCompactAdapter.setLoadCompleteListener(new OnLoadCompleteListener() {
                    @Override
                    public void onLoadComplete(DirtyPostCompactAdapter adapter) {
                        moveTo(postId);
                        postCompactAdapter.setLoadCompleteListener(null);
                    }
                });
            }
        }
    }
    
    private void initContent() {
    	ViewStub stub = (ViewStub) findViewById(R.id.content_stub);
    	stub.setLayoutResource(R.layout.l_posts_list);
    	stub.inflate();
    	
    	FasterScrollerView fasterScroller = (FasterScrollerView) findViewById(R.id.fastscroll);
		fasterScroller.setFastScrollEnabled(true);
    	
        postsCompactList = (ListView) findViewById(R.id.posts_list);
        postsCompactList.setEmptyView(dirtyTv);
        postCompactAdapter = new DirtyPostCompactAdapter(this);
        postCompactAdapter.setShowOnlyFavorites(dirtyPreferences.isShowingOnlyFavorites());
        postCompactAdapter.setFasterScrollerView(fasterScroller);
        postCompactAdapter.setSubBlogUrl(subBlogUrl);
        postsCompactList.addHeaderView(Views.createVerticalSpacer(this, 10), null, false);
        postsCompactList.addFooterView(Views.createVerticalSpacer(this, 30), null, false);
        postsCompactList.setAdapter(postCompactAdapter);
        postsCompactList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				openPager(id);
			}
		});
        
        getSupportLoaderManager().restartLoader(Loaders.DIRTY_POSTS_LOADER, null, postCompactAdapter);
    }

    @Override
    protected void onSubBlogChanged(String prevBlogUrl) {
        long itemId = getFirstVisiblePostId();
        if (itemId >= 0) {
            dirtyPreferences.setLastListVisiblePostId(prevBlogUrl, itemId);
        }

        postCompactAdapter.setSubBlogUrl(subBlogUrl);
        getSupportLoaderManager().restartLoader(Loaders.DIRTY_POSTS_LOADER, null, postCompactAdapter);
        postCompactAdapter.setLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(DirtyPostCompactAdapter adapter) {
                long  postId = dirtyPreferences.getLastListVisiblePostId(subBlogUrl);
                if (postId >= 0) {
                    moveTo(postId);
                }
                postCompactAdapter.setLoadCompleteListener(null);
            }
        });
    }

    private long getFirstVisiblePostId() {
        int position = postsCompactList.getFirstVisiblePosition();
        if (position > 0) {
            position--;
        }
        return postCompactAdapter.getItemId(position);
    }

    private void restoreLastViewedPost() {
    	long id = dirtyPreferences.getLastViewedPostId(subBlogUrl);
        if (id >= 0) {
        	openPager(id);
        }
    }

    @Override
    public void startGertrudaRefresh(String gertrudaUrl) {
    	if (!isPaused) {
    		super.startGertrudaRefresh(gertrudaUrl);
    	}
    }
    
    @Override
    public void showLoadingPostsError() {
    	if (!isPaused) {
    		super.showLoadingPostsError();
    	}
    }
    
    @Override
    public void showPostLoadedMessage(int newCount) {
    	if (!isPaused) {
    		super.showPostLoadedMessage(newCount);
    	}
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            dirtyPreferences.setLastSubBlog(subBlogUrl);
        }
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	
    	boolean onlyFav = dirtyPreferences.isShowingOnlyFavorites();
    	if (onlyFav != postCompactAdapter.isShowOnlyFavorites()) {
    		postCompactAdapter.setShowOnlyFavorites(onlyFav);
    		getSupportLoaderManager().restartLoader(Loaders.DIRTY_POSTS_LOADER, null, postCompactAdapter);
    	}
    	postCompactAdapter.notifyDataSetChanged();
    }
    
    private void openPager(long postId) {
    	Intent intent = new Intent(this, PostsPagerActivity.class);
		intent.putExtra(PostsPagerActivity.EXTRA_POST_ID, postId);
        intent.putExtra(PostsPagerActivity.EXTRA_DIRTY_SUB_BLOG_URL, subBlogUrl);
		attachRequestsIds(intent);
		this.startActivityForResult(intent, PAGER_REQUEST_CODE);
		isPaused = true;
    }
     
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == PAGER_REQUEST_CODE) {
    		isPaused = false;
    		if (data != null) {
    			final long postId = data.getLongExtra(PostsPagerActivity.EXTRA_POST_ID, -1);
                String subBlog = data.getStringExtra(PostsPagerActivity.EXTRA_DIRTY_SUB_BLOG_URL);
                boolean newSubBlog = !TextUtils.equals(subBlog, subBlogUrl);
                setSubBlog(subBlog);
    			if (postId >= 0) {
    				if (newSubBlog || !moveTo(postId)) {
    					postCompactAdapter.setLoadCompleteListener(new OnLoadCompleteListener() {
							@Override
							public void onLoadComplete(DirtyPostCompactAdapter adapter) {
								moveTo(postId);
			    				postCompactAdapter.setLoadCompleteListener(null);
							}
						});
    				}
    			}
    		}
    	}
    	super.onActivityResult(requestCode, resultCode, data);
    }
    
	private boolean moveTo(final long postId) {
    	final int pos = postCompactAdapter.getPostPosition(postId);
    	if (pos >= 0) {
    		postsCompactList.postDelayed(new Runnable() {
                public void run() {
                    postsCompactList.setSelection(pos + 1);
                }
            }, 100);
    		return true;
    	} else {
    		return false;
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.go_to_first_menu_item:
           		postsCompactList.setSelection(0);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void toggleShowFavorites() {
    	super.toggleShowFavorites();
        boolean show = dirtyPreferences.isShowingOnlyFavorites();
        postCompactAdapter.setShowOnlyFavorites(show);
        getSupportLoaderManager().restartLoader(Loaders.DIRTY_POSTS_LOADER, null, postCompactAdapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        long itemId = getFirstVisiblePostId();
        if (itemId >= 0) {
            outState.putLong(LAST_TOP_POST_ID, itemId);
        }
    }

}