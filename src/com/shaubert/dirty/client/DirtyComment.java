package com.shaubert.dirty.client;

import com.shaubert.dirty.db.DirtyContract.DirtyCommentEntity;

import android.content.ContentValues;

public class DirtyComment extends DirtyRecord {

    public DirtyComment() {
        super();
    }

    public DirtyComment(ContentValues values) {
        super(values);
    }

    public int getIndentLevel() {
        return values.getAsInteger(DirtyCommentEntity.INDENT_LEVEL, 0);
    }
    
    public void setIndentLevel(int level) {
        values.put(DirtyCommentEntity.INDENT_LEVEL, level);
    }
    
    public int getOrder() {
        return values.getAsInteger(DirtyCommentEntity.COMMENTS_ORDER, 0);
    }
    
    public void setOrder(int order) {
        values.put(DirtyCommentEntity.COMMENTS_ORDER, order);
    }
}
