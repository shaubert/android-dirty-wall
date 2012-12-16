package com.shaubert.dirty;

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

import com.shaubert.dirty.db.BlogsCursor;
import com.shaubert.dirty.db.DirtyContract.DirtyBlogEntity;

public class DirtyBlogsAdapter extends CursorAdapter implements LoaderCallbacks<Cursor> {

    public interface OnLoadCompleteListener {
        void onLoadComplete(DirtyBlogsAdapter adapter);
    }

	private FragmentActivity fragmentActivity;
	private BlogsCursor blogsCursor;

    private OnLoadCompleteListener loadCompleteListener;

    private boolean loadOnlyFavorites;

	public DirtyBlogsAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity, null, 0);
        this.fragmentActivity = fragmentActivity;
	}

    public void refresh() {
        cleanup();
        fragmentActivity.getSupportLoaderManager().restartLoader(Loaders.DIRTY_BLOGS_LOADER, null, this);
    }

    public boolean isLoadOnlyFavorites() {
        return loadOnlyFavorites;
    }

    public void setLoadOnlyFavorites(boolean loadOnlyFavorites) {
        this.loadOnlyFavorites = loadOnlyFavorites;
    }

    public void setLoadCompleteListener(OnLoadCompleteListener loadCompleteListener) {
        this.loadCompleteListener = loadCompleteListener;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new DirtyBlogView(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    	DirtyBlogView dirtyBlogView = (DirtyBlogView)view;
    	dirtyBlogView.swapData(blogsCursor);
    }

    public int getBlogPosition(long blogId) {
    	if (blogsCursor != null && blogsCursor.moveToFirst()) {
    		do {
    			if (blogsCursor.getBlogId() == blogId) {
    				return blogsCursor.getPosition();
    			}
    		} while (blogsCursor.moveToNext());
    	}
    	return -1;
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(fragmentActivity, 
                DirtyBlogEntity.URI,
                null,
                loadOnlyFavorites ? (DirtyBlogEntity.FAVORITE + " != 0") : null,
                null,
                DirtyBlogEntity.READERS_COUNT + " DESC");
        loader.setUpdateThrottle(1000);
        return loader;
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    	blogsCursor = new BlogsCursor(data);
    	swapCursor(blogsCursor);

        if (loadCompleteListener != null) {
            loadCompleteListener.onLoadComplete(this);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cleanup();
    }

    private void cleanup() {
        blogsCursor = null;
        swapCursor(null);
    }

}
