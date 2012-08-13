package com.shaubert.dirty.db;

import java.util.Arrays;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.MetricAffectingSpan;

import com.shaubert.dirty.Loaders;
import com.shaubert.dirty.client.DirtyPost;
import com.shaubert.dirty.client.DirtyRecord.Image;
import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;
import com.shaubert.util.Files;
import com.shaubert.util.Shlog;
import com.shaubert.util.Spannables;
import com.shaubert.util.Versions;

public abstract class DirtyPostLoaderCallbacks implements LoaderCallbacks<Cursor> {
    
    private static final Shlog SHLOG = new Shlog(DirtyPostLoaderCallbacks.class.getSimpleName());
    
    private static final String REFRESING_SPANNED_TEXT_TIMER = "refresing spanned text for ";
    
    private final long postId;
    private DirtyPost dirtyPost;
    private AsyncTask<?, ?, ?> postProcessTask;

    private final FragmentActivity activity;
    private Handler handler;

    public DirtyPostLoaderCallbacks(FragmentActivity activity, long postId) {
        this.activity = activity;
        this.postId = postId;
        this.handler = new Handler();
    }
        
    public void initLoader() {
        activity.getSupportLoaderManager().initLoader(getLoaderId(postId), null, this);
    }
    
    public void restartLoader() {
        activity.getSupportLoaderManager().restartLoader(getLoaderId(postId), null, this);
    }
    
    public static int getLoaderId(long postid) {
        return Loaders.POST_LOADER_MAPPER.getLoaderIdFrom(postid);
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(activity, 
                ContentUris.withAppendedId(DirtyPostEntity.URI, postId), null, null, null, null);
        loader.setUpdateThrottle(1000);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && !data.isClosed()) {
            SHLOG.d("post cursor refreshed for " + postId);
            if (data.moveToFirst()) {
                ContentValues values = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(data, values);
                dirtyPost = new DirtyPost(values);
                postProcess(dirtyPost);
            }
        } else {
            SHLOG.d("post cursor is closed, restarting loader for " + postId);
            handler.post(new Runnable() {
                public void run() {
                    restartLoader();
                }
            });
        }
    }

    private void postProcess(DirtyPost post) {
        if (postProcessTask != null) {
            SHLOG.d("cancelling old processing task for " + postId);
            postProcessTask.cancel(true);
        }
        postProcessTask = new AsyncTask<DirtyPost, Void, DirtyPost>() {
            @Override
            protected DirtyPost doInBackground(DirtyPost... params) {
                DirtyPost post = params[0];
                
                SHLOG.resetTimer(REFRESING_SPANNED_TEXT_TIMER + postId);
                Spanned text = post.getSpannedText();
                Spannables.clearMetrictAffectingSpansIfJB(text);
                SHLOG.logTimer(REFRESING_SPANNED_TEXT_TIMER + postId);
                
                Image[] images = dirtyPost.getImages();
                if (images != null && images.length > 0) {
                    SHLOG.d("image urls = " + Arrays.toString(images));
                    String imageUrl = images[0].src;
                    post.setCachedImagePath(Files.getPostImageCache(activity, dirtyPost, imageUrl));
                }
                
                return post;
            };
            
            protected void onPostExecute(DirtyPost result) {
                if (result == dirtyPost) {
                    onDirtyPostLoaded(dirtyPost);
                } else {
                    SHLOG.d("ignoring processing task result for " + postId);
                }
            }
        }.execute(post);
    }
    
    public abstract void onDirtyPostLoaded(DirtyPost dirtyPost);
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
    
}
