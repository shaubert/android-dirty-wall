package com.shaubert.dirty;

import com.shaubert.dirty.db.CommentsCursor;
import com.shaubert.dirty.db.DirtyContract.DirtyCommentEntity;
import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

public class DirtyCommentsAdapter extends CursorAdapter implements LoaderCallbacks<Cursor> {

    private final long postId;
    private final int loaderId;
    private final FragmentActivity fragmentActivity;
    
    private CommentsCursor commentsCursor;

    public DirtyCommentsAdapter(FragmentActivity fragmentActivity, long postId) {
        super(fragmentActivity, null, 0);
        this.fragmentActivity = fragmentActivity;
        this.postId = postId;
        this.loaderId = Loaders.COMMENTS_LOADER_MAPPER.getLoaderIdFrom(postId);
        
        initLoader();
    }

    public void initLoader() {
        fragmentActivity.getSupportLoaderManager().initLoader(loaderId, null, this);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new DirtyCommentView(fragmentActivity);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        DirtyCommentView dirtyCommentView = (DirtyCommentView)view;
        dirtyCommentView.swapData(commentsCursor);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(fragmentActivity, 
                DirtyPostEntity.getCommentsUri(postId), 
                new String[] { DirtyCommentEntity.ID, DirtyCommentEntity.INDENT_LEVEL, DirtyCommentEntity.POST,
                    DirtyCommentEntity.AUTHOR, DirtyCommentEntity.AUTHOR_LINK, DirtyCommentEntity.CREATION_DATE,
                    DirtyCommentEntity.IMAGE_URLS, DirtyCommentEntity.SERVER_ID, DirtyCommentEntity.VIDEO_URL, 
                    DirtyCommentEntity.VOTES_COUNT, DirtyCommentEntity.FORMATTED_MESSAGE, 
                    DirtyCommentEntity.MESSAGE}, null, null, DirtyCommentEntity.COMMENTS_ORDER);
        loader.setUpdateThrottle(1000);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swapCursor(commentsCursor = new CommentsCursor(data));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swapCursor(null);
    }

    public void destroyLoader() {
        fragmentActivity.getLoaderManager().destroyLoader(loaderId);
    }
}