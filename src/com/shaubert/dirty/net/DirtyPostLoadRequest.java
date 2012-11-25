package com.shaubert.dirty.net;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.shaubert.blogadapter.client.HttpDataLoaderRequest;
import com.shaubert.blogadapter.client.Pager;
import com.shaubert.dirty.DirtyNewPostsStatusBarNotification;
import com.shaubert.dirty.client.DirtyBlog;
import com.shaubert.dirty.client.DirtyPost;
import com.shaubert.dirty.client.DirtyPostParser;
import com.shaubert.dirty.db.DirtyContract;
import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;
import com.shaubert.net.core.RequestBase;
import com.shaubert.net.core.RequestStateBase;
import com.shaubert.net.nutshell.ExecutionContext;
import com.shaubert.util.Shlog;
import com.shaubert.util.TimePreferences;

public class DirtyPostLoadRequest extends RequestBase {

    public static final String URL_TO_LOAD_PARAM = "url";
    public static final String CANCELLED_BY_TIME_PARAM = "cancelled_by_time";
    public static final String GERTRUDA_URL_PARAM = "gertruda";
    public static final String NEW_POSTS_NUMBER_PARAM = "new";
    public static final String FORCE_PARAM = "force";
    public static final String BACKGROUND_NOTIFICATION_PARAM = "background-notification";	

	private static class ContentProviderOperations {
        public ArrayList<ContentProviderOperation> ops;
        public int insertsCount;       
    }
    
    private static final Shlog SHLOG = new Shlog(DirtyPostLoadRequest.class.getSimpleName());
    private static final int THRESHOLD = 30 * 60 * 1000;
    
    public DirtyPostLoadRequest() {
        this(null);
    }
    
    public DirtyPostLoadRequest(RequestStateBase state) {
        super(state);
    }

    @Override
    public void execute(ExecutionContext executionContext) throws Exception {  
        TimePreferences timePreferences = new TimePreferences(PreferenceManager.getDefaultSharedPreferences(
                executionContext.getContext()), DirtyPostLoadRequest.class.getSimpleName(), THRESHOLD);
        boolean force = getState().getBoolean(FORCE_PARAM, false);
        if (force || timePreferences.shouldPerformOperation()) {
            Pager<DirtyPost> pager = createPager(DirtyBlog.getInstance());
            List<DirtyPost> dirtyPosts = pager.loadNext();
            SHLOG.d("posts processed, converting to cpo");
            ContentProviderOperations res = convertToOperations(executionContext, dirtyPosts);
            ArrayList<ContentProviderOperation> operations = res.ops;
            String timerName = "apply batch of " + operations.size() + " cpo";
            SHLOG.resetTimer(timerName);
            executionContext.getContext().getContentResolver().applyBatch(DirtyContract.AUTHORITY, operations);
            SHLOG.logTimer(timerName);
            getState().put(NEW_POSTS_NUMBER_PARAM, res.insertsCount);
            getState().put(GERTRUDA_URL_PARAM, DirtyPostParser.getGertrudaUrl());
            if (getState().getBoolean(BACKGROUND_NOTIFICATION_PARAM, false)) {
            	sendStatusBarNotification(res.insertsCount, executionContext);
            }
            timePreferences.commit();
        } else {
            getState().put(CANCELLED_BY_TIME_PARAM, true);
            SHLOG.d("skipping " + DirtyPostLoadRequest.class.getSimpleName() + ": last sync was too recent");
        }
//        sendStatusBarNotification(new Random().nextInt(80) + 1, executionContext);
    }

	private ContentProviderOperations convertToOperations(ExecutionContext executionContext, List<DirtyPost> dirtyPosts) {
        ContentProviderOperations result = new ContentProviderOperations();
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        Cursor currentPosts = executionContext.getContext().getContentResolver().query(
                DirtyPostEntity.URI, new String [] { DirtyPostEntity.ID, DirtyPostEntity.SERVER_ID, DirtyPostEntity.UNREAD }, null, null, null);
        try {
            for (DirtyPost post : dirtyPosts) {
            	if (moveCursorToPostWithServerId(currentPosts, post.getServerId())) {
            		post.setId(currentPosts.getLong(0));
            		post.setUnread(currentPosts.getInt(2) > 0);
            	} else {
            		post.setUnread(true);
            	}
                post.setInsertTime(System.currentTimeMillis());
                operations.add(convertToOperation(post));
                if (post.getId() == null) {
                    result.insertsCount++;
                }
            }
        } finally {
            currentPosts.close();
        }
        result.ops = operations;
        return result;
    }

    private ContentProviderOperation convertToOperation(DirtyPost post) {
        if (post.getId() != null) {
            return ContentProviderOperation
                    .newUpdate(ContentUris.withAppendedId(DirtyPostEntity.URI, post.getId()))
                    .withValues(post.getValues())
                    .build();
        } else {
            return ContentProviderOperation
                    .newInsert(DirtyPostEntity.URI)
                    .withValues(post.getValues())
                    .build();
        }
    }

    private boolean moveCursorToPostWithServerId(Cursor currentPosts, long serverId) {
        if (currentPosts.moveToFirst()) {
            do {
                if (currentPosts.getLong(1) == serverId) {
                    return true;
                }
            } while (currentPosts.moveToNext());
        }
        return false;
    }

    private Pager<DirtyPost> createPager(DirtyBlog blog) {
        final Pager<DirtyPost> pager;
        String url = getState().getString(URL_TO_LOAD_PARAM);
        if (TextUtils.isEmpty(url)) {
            SHLOG.d("requesting newest posts");
            pager = blog.createPager();
        } else {
            SHLOG.d("requesting posts from " + url);
            HttpDataLoaderRequest request = new HttpDataLoaderRequest();
            request.setUrl(url);
            pager = blog.createPager(request);
        }
        return pager;
    }

    private void sendStatusBarNotification(int insertsCount, ExecutionContext executionContext) {
    	if (insertsCount > 0) {
    		new DirtyNewPostsStatusBarNotification(executionContext.getContext(), insertsCount).post();
    	}
	}

}
