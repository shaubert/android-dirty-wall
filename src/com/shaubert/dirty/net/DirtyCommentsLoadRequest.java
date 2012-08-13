package com.shaubert.dirty.net;

import com.shaubert.blogadapter.client.Pager;
import com.shaubert.dirty.client.DirtyBlog;
import com.shaubert.dirty.client.DirtyComment;
import com.shaubert.dirty.client.DirtyPost;
import com.shaubert.dirty.db.DirtyContract;
import com.shaubert.dirty.db.DirtyContract.DirtyCommentEntity;
import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;
import com.shaubert.net.core.RequestBase;
import com.shaubert.net.core.RequestStateBase;
import com.shaubert.net.nutshell.ExecutionContext;
import com.shaubert.util.Shlog;
import com.shaubert.util.TimePreferences;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class DirtyCommentsLoadRequest extends RequestBase {

    private static final Shlog SHLOG = new Shlog(DirtyCommentsLoadRequest.class.getSimpleName());
    private static final int THRESHOLD = 10 * 60 * 1000;
    
    public DirtyCommentsLoadRequest() {
        this(null);
    }
    
    public DirtyCommentsLoadRequest(RequestStateBase state) {
        super(state);
    }

    @Override
    public void execute(ExecutionContext executionContext) throws Exception {  
        long postId = getState().getLong("post-id", -1);
        TimePreferences timePreferences = new TimePreferences(PreferenceManager.getDefaultSharedPreferences(
                executionContext.getContext()), DirtyCommentsLoadRequest.class.getSimpleName() + postId, THRESHOLD);
        boolean force = getState().getBoolean("force", false);
        if (force || timePreferences.shouldPerformOperation()) {
            DirtyPost post = selectPost(executionContext);
            Pager<DirtyComment> pager = DirtyBlog.getInstance().createPager(post);
            List<DirtyComment> dirtyComments = pager.loadNext();
            SHLOG.d("comments processed, converting to cpo");
            ArrayList<ContentProviderOperation> operations = convertToOperations(executionContext, dirtyComments, post);
            String timerName = "apply batch of " + operations.size() + " cpo";
            SHLOG.resetTimer(timerName);
            executionContext.getContext().getContentResolver().applyBatch(DirtyContract.AUTHORITY, operations);
            SHLOG.logTimer(timerName);
            timePreferences.commit();
        } else {
            SHLOG.d("skipping " + DirtyCommentsLoadRequest.class.getSimpleName() + ": last sync was too recent");
        }
    }

    private ArrayList<ContentProviderOperation> convertToOperations(ExecutionContext executionContext, 
            List<DirtyComment> dirtyComments, DirtyPost post) {
        Uri baseUri = DirtyPostEntity.getCommentsUri(post.getId());
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        Cursor currentPosts = executionContext.getContext().getContentResolver().query(
                baseUri, new String [] { DirtyPostEntity.ID, DirtyPostEntity.SERVER_ID }, null, null, null);
        try {
            for (DirtyComment comment : dirtyComments) {
                comment.setId(findId(currentPosts, comment.getServerId()));
                comment.setInsertTime(System.currentTimeMillis());
                operations.add(convertToOperation(comment, baseUri));
            }
        } finally {
            currentPosts.close();
        }
        return operations;
    }

    private ContentProviderOperation convertToOperation(DirtyComment comment, Uri baseUri) {
        if (comment.getId() != null) {
            return ContentProviderOperation
                    .newUpdate(ContentUris.withAppendedId(DirtyCommentEntity.URI, comment.getId()))
                    .withValues(comment.getValues())
                    .build();
        } else {
            return ContentProviderOperation
                    .newInsert(baseUri)
                    .withValues(comment.getValues())
                    .build();
        }
    }

    private Long findId(Cursor currentComments, long serverId) {
        if (currentComments.moveToFirst()) {
            do {
                if (currentComments.getLong(1) == serverId) {
                    return currentComments.getLong(0);
                }
            } while (currentComments.moveToNext());
        }
        return null;
    }

    private DirtyPost selectPost(ExecutionContext executionContext) {
        long postId = getState().getLong("post-id", -1);
        Cursor postCursor = executionContext.getContext().getContentResolver().query(
                ContentUris.withAppendedId(DirtyPostEntity.URI, postId), null, null, null, null);
        if (postCursor != null) {
            try {
                if (postCursor.moveToFirst()) {
                    ContentValues values = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(postCursor, values);
                    return new DirtyPost(values);
                }
            } finally {
                postCursor.close();
            }
        }
        throw new IllegalStateException("unable to select post: " + postId);
    }

}
