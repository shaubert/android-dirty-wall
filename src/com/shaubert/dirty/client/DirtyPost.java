package com.shaubert.dirty.client;

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
        
    public int getCommentsCount() {
        return values.getAsInteger(DirtyPostEntity.COMMENTS_COUNT, 0);
    }
    
    public void setCommentsCount(int commentsCount) {
        this.values.put(DirtyPostEntity.COMMENTS_COUNT, commentsCount);
    }
                
} 