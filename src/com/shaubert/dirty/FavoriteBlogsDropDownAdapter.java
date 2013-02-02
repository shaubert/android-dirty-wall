package com.shaubert.dirty;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import com.shaubert.dirty.db.DirtyContract;

public class FavoriteBlogsDropDownAdapter extends DirtyBlogsAdapter {

    private FragmentActivity activity;
    private String alwaysVisibleSubBlog;

    public FavoriteBlogsDropDownAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.activity = fragmentActivity;
        setLoadOnlyFavorites(true);
    }

    public void setAlwaysVisibleSubBlog(String alwaysVisibleSubBlog) {
        this.alwaysVisibleSubBlog = alwaysVisibleSubBlog;
    }

    public String getSubBlogUrl(long blogId) {
        if (blogId == MAIN_PAGE_ID) {
            return null;
        }
        if (blogsCursor != null && blogsCursor.moveToFirst()) {
            do {
                if (blogsCursor.getBlogId() == blogId) {
                    return blogsCursor.getUrl();
                }
            } while (blogsCursor.moveToNext());
        }
        return null;
    }

    public long getBlogId(String subBlogUrl) {
        if (TextUtils.isEmpty(subBlogUrl)){
            return MAIN_PAGE_ID;
        }
        if (blogsCursor != null && blogsCursor.moveToFirst()) {
            do {
                if (blogsCursor.getUrl().equals(subBlogUrl)) {
                    return blogsCursor.getBlogId();
                }
            } while (blogsCursor.moveToNext());
        }
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DirtyBlogDropDownView result = (DirtyBlogDropDownView) super.getDropDownView(position, convertView, parent);
        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            result.hideSummary();
        }
        return result;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new DirtyBlogDropDownView(activity);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        DirtyBlogDropDownView dropDownView = (DirtyBlogDropDownView) view;
        if (cursor == null) {
            dropDownView.setMainPage();
        } else {
            dropDownView.swapData(blogsCursor);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader result = (CursorLoader) super.onCreateLoader(id, args);
        String selection = DirtyContract.DirtyBlogEntity.FAVORITE + " != 0";
        String[] selectionArgs = null;
        if (alwaysVisibleSubBlog != null) {
            selection += " OR " + DirtyContract.DirtyBlogEntity.URL + " = ?";
            selectionArgs = new String[] { alwaysVisibleSubBlog };
        }
        result.setSelectionArgs(selectionArgs);
        result.setSelection(selection);
        return result;
    }
}
