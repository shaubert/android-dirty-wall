package com.shaubert.dirty;

import android.text.TextUtils;
import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;
import com.shaubert.dirty.db.SqlHelper;
import com.shaubert.util.FragmentStatePagerAdapterWithDatasetChangeSupport;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;

public class DirtyPostFragmentsAdapter extends FragmentStatePagerAdapterWithDatasetChangeSupport implements LoaderCallbacks<Cursor> {

    public interface OnLoadCompleteListener {
        void onLoadComplete(DirtyPostFragmentsAdapter adapter);
    }

    private final FragmentActivity fragmentActivity;
    private Cursor postIds;

    private boolean saveStateWasCalled;
    private View emptyView;
    private boolean showOnlyFavorites;
    private String subBlogUrl;

    private OnLoadCompleteListener loadCompleteListener;
    
    public DirtyPostFragmentsAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity.getSupportFragmentManager());
        this.fragmentActivity = fragmentActivity;
    }
    
    public void setLoadCompleteListener(OnLoadCompleteListener loadCompleteListener) {
        this.loadCompleteListener = loadCompleteListener;
    }

    public void setShowOnlyFavorites(boolean showOnlyFavorites) {
        this.showOnlyFavorites = showOnlyFavorites;
    }

    public String getSubBlogUrl() {
        return subBlogUrl;
    }

    public void setSubBlogUrl(String subBlogUrl) {
        this.subBlogUrl = subBlogUrl;
    }

    @Override
    public Fragment getItem(int position) {
        long id = getStableId(position);
        return DirtyPostFragment.newInstance(id);
    }
    
    @Override
    public CharSequence getPageTitle(int position) {
        if (postIds != null && position < getCount()) {
            postIds.moveToPosition(position);
            return postIds.getString(1) + "…";
        }
        return "";
    }
    
    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
        refreshEmptyViewState();
    }
    
    private void refreshEmptyViewState() {
        if (emptyView != null) {
            if (getCount() == 0) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public long getStableId(int position) {
        if (postIds != null && postIds.getCount() > 0) {
            postIds.moveToPosition(position);
            return postIds.getLong(0);
        } else {
            return -1;
        }
    }
    
    public boolean isUnread(int position) {
        if (postIds != null && postIds.getCount() > 0) {
            postIds.moveToPosition(position);
            return postIds.getInt(2) != 0;
        } else {
            return false;
        }
    }
    
    @Override
    public int getCount() {
        return postIds == null ? 0 : postIds.getCount();
    }

    @Override
    public int getItemPosition(Object object) {
        DirtyPostFragment fragment = (DirtyPostFragment)object;
        long id = fragment.getPostId();
        int foundPosition = findPosition(id, POSITION_NONE);
        return foundPosition;
    }

    public int findPosition(long id, int def) {
        if (postIds != null && postIds.moveToFirst()) {
            do {
                if (postIds.getLong(0) == id) {
                    return postIds.getPosition();
                }
            } while (postIds.moveToNext());
        }
        return def;
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(fragmentActivity, DirtyPostEntity.URI, 
                new String[] { DirtyPostEntity.ID, 
	        		"substr(" + DirtyPostEntity.MESSAGE + ", 1, 15)",
	        		DirtyPostEntity.UNREAD },
                SqlHelper.buildAndSelection(showOnlyFavorites ? (DirtyPostEntity.FAVORITE + " != 0") : null,
                        !TextUtils.isEmpty(subBlogUrl) ? (DirtyPostEntity.SUB_BLOG_NAME + " = ?") : null),
                TextUtils.isEmpty(subBlogUrl) ? null : new String[] {subBlogUrl},
                DirtyPostEntity.CREATION_DATE + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        postIds = data;
        notifyDataSetChanged();
        refreshEmptyViewState();
        
        if (loadCompleteListener != null && postIds != null && postIds.getCount() > 0) {
            loadCompleteListener.onLoadComplete(this);
        }
    }

    @Override
    public Parcelable saveState() {
        saveStateWasCalled = true;
        return super.saveState();
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        postIds = null;
        if (!saveStateWasCalled && !fragmentActivity.isFinishing()) {
            notifyDataSetChanged();
        }
        refreshEmptyViewState();
    }

}