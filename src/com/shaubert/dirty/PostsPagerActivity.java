package com.shaubert.dirty;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.LinearLayout;
import com.shaubert.dirty.DirtyPostFragmentsAdapter.OnLoadCompleteListener;
import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;
import com.shaubert.util.AsyncTasks;
import com.shaubert.util.AsyncTasks.Task;
import com.shaubert.util.Shlog;
import com.shaubert.util.Versions;

public class PostsPagerActivity extends DirtyActivityWithPosts {

	public static final Shlog SHLOG = new Shlog(PostsPagerActivity.class.getSimpleName());

	public static final String EXTRA_POST_ID = "post-id-extra";

	private ViewPager postPager;
	private DirtyPostFragmentsAdapter postFragmentsAdapter;

	private Handler handler = new Handler();
	private Runnable markPostAsReadTask;

	@TargetApi(11)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        initContent();

		if (Versions.isApiLevelAvailable(11)) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		tryMoveToLastViewedPost(savedInstanceState);
	}

    private void initContent() {
        ViewStub stub = (ViewStub) findViewById(R.id.content_stub);
        stub.setLayoutResource(R.layout.l_posts_pager);
        View view = stub.inflate();
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.weight = 1;

        postPager = (ViewPager) findViewById(R.id.post_pager);
        postFragmentsAdapter = new DirtyPostFragmentsAdapter(this);
        postPager.setAdapter(postFragmentsAdapter);
        postFragmentsAdapter.setShowOnlyFavorites(dirtyPreferences.isShowingOnlyFavorites());
        postFragmentsAdapter.setEmptyView(dirtyTv);
        postFragmentsAdapter.setSubBlogUrl(subBlogUrl);
        postPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int pageIndex) {
                markCurrentPostAsRead(pageIndex);
            }
        });

        getSupportLoaderManager().initLoader(Loaders.DIRTY_POST_IDS_LOADER, null, postFragmentsAdapter);
    }

    @Override
    protected void onSubBlogChanged(String prevSubBlog) {
        saveCurrentPostId(prevSubBlog);
        postFragmentsAdapter.setSubBlogUrl(subBlogUrl);
        getSupportLoaderManager().restartLoader(Loaders.DIRTY_POST_IDS_LOADER, null, postFragmentsAdapter);
        navigateToPost(dirtyPreferences.getLastViewedPostId(subBlogUrl));
    }

    private void tryMoveToLastViewedPost(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			if (getIntent() != null) {
				long postId = getIntent().getLongExtra(EXTRA_POST_ID, -1);
				navigateToPost(postId);
			}
		} else {
			long lastPostId = savedInstanceState.getLong("last-post-id", -1);
			navigateToPost(lastPostId);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong("last-post-id", tryGetCurrentPostId());
	}

	protected void markCurrentPostAsRead(int pageIndex) {
		removeMarkPostAsReadCallbacks();
		long postId = tryGetCurrentPostId();
		if (postId >= 0) {
			scheduleMarkPostAsRead(postId, pageIndex);
		}
	}

	private void scheduleMarkPostAsRead(final long postId, final int pos) {
		markPostAsReadTask = new Runnable() {
			@Override
			public void run() {
				if (postFragmentsAdapter.getStableId(pos) == postId 
						&& postFragmentsAdapter.isUnread(pos)) {
					final ContentValues values = new ContentValues();
					values.put(DirtyPostEntity.UNREAD, 0);
					AsyncTasks.executeInBackground(new Task<Context>(PostsPagerActivity.this) {
						@Override
						public void run(Context context) {
							context.getContentResolver()
									.update(ContentUris.withAppendedId(DirtyPostEntity.URI, postId), values, null, null);
						}
					});
				}
			}
		};
		handler.postDelayed(markPostAsReadTask, 600);
	}

	private void removeMarkPostAsReadCallbacks() {
		if (markPostAsReadTask != null) {
			handler.removeCallbacks(markPostAsReadTask);
			markPostAsReadTask = null;
		}
	}

	private void navigateToPost(final long postId) {
		postFragmentsAdapter.setLoadCompleteListener(new OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(DirtyPostFragmentsAdapter adapter) {
				if (postId >= 0) {
					int pos = adapter.findPosition(postId, -1);
					if (pos >= 0) {
                        if (postPager.getCurrentItem() != pos) {
						    postPager.setCurrentItem(pos, false);
                        }
                        markCurrentPostAsRead(pos);
					}
				}
				adapter.setLoadCompleteListener(null);
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		if (postFragmentsAdapter.getCount() > 0 && postPager.getCurrentItem() >= 0) {
			markCurrentPostAsRead(postPager.getCurrentItem());
		}
	}
	
	@Override
	protected void onPause() {
		saveCurrentPostId(subBlogUrl);
		removeMarkPostAsReadCallbacks();
		super.onPause();
	}

	private void saveCurrentPostId(String subBlogUrl) {
		long curPostId = tryGetCurrentPostId();
		if (curPostId >= 0) {
			SHLOG.d("saving current post id (" + curPostId + ")");
			dirtyPreferences.setLastViewedPostId(subBlogUrl, curPostId);
		}
	}

	private long tryGetCurrentPostId() {
		if (postFragmentsAdapter.getCount() > 0) {
			int currentItem = postPager.getCurrentItem();
			return postFragmentsAdapter.getStableId(currentItem);
		} else {
			return -1;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;

		case R.id.go_to_first_menu_item:
			if (postFragmentsAdapter.getCount() > 0) {
				postPager.setCurrentItem(0, true);
			}
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void toggleShowFavorites() {
		super.toggleShowFavorites();
		boolean show = dirtyPreferences.isShowingOnlyFavorites();
		postFragmentsAdapter.setShowOnlyFavorites(show);
		getSupportLoaderManager().restartLoader(Loaders.DIRTY_POST_IDS_LOADER, null, postFragmentsAdapter);
	}

	public static void startMe(Context context, long postId) {
		Intent intent = new Intent(context, PostsPagerActivity.class);
		intent.putExtra(PostsPagerActivity.EXTRA_POST_ID, postId);
		context.startActivity(intent);
	}

	@Override
	public void onBackPressed() {
		Intent data = new Intent();
		data.putExtra(EXTRA_POST_ID, postFragmentsAdapter.getStableId(postPager.getCurrentItem()));
		data.putExtra(EXTRA_DIRTY_SUB_BLOG_URL, subBlogUrl);
		setResult(RESULT_OK, data);

		super.onBackPressed();
	}
}