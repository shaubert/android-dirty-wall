package com.shaubert.dirty.client;

import android.text.TextUtils;
import com.shaubert.dirty.db.ContentValuesState;
import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;

import android.content.ContentValues;

public class DirtyPost extends DirtyRecord {
    
    public DirtyPost() {
        this(new ContentValues());
    }

    public DirtyPost(ContentValues values) {
        this.values = new ContentValuesState(values);
    }
        
    public void setGolden(boolean golden) {
        values.putBooleanAsInt(DirtyPostEntity.GOLDEN, golden);
    }
    
    public boolean isGolden() {
        return values.getIntAsBoolean(DirtyPostEntity.GOLDEN, false);
    }
    
    public void setFavorite(boolean favorite) {
        values.putBooleanAsInt(DirtyPostEntity.FAVORITE, favorite);
    }
    
    public boolean isFavorite() {
        return values.getIntAsBoolean(DirtyPostEntity.FAVORITE, false);
    }
    
    public boolean isUnread() {
        return values.getIntAsBoolean(DirtyPostEntity.UNREAD, false);
    }
    
    public void setUnread(boolean unread) {
        values.putBooleanAsInt(DirtyPostEntity.UNREAD, unread);
    }
    
    public int getCommentsCount() {
        return values.getAsInteger(DirtyPostEntity.COMMENTS_COUNT, 0);
    }
    
    public void setCommentsCount(int commentsCount) {
        this.values.put(DirtyPostEntity.COMMENTS_COUNT, commentsCount);
    }
     
    public void setSubBlogName(String subBlogName) {
        this.values.put(DirtyPostEntity.SUB_BLOG_NAME, subBlogName);
    }
    
    public String getSubBlogName() {
        return this.values.getAsString(DirtyPostEntity.SUB_BLOG_NAME, null);
    }

    public String getSubBlogHost() {
        String subBlogName = getSubBlogName();
        if (TextUtils.isEmpty(subBlogName)) {
            return "d3.ru";
        } else {
            return subBlogName + ".d3.ru";
        }
    }
}