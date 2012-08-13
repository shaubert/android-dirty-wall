package com.shaubert.dirty.db;

import com.shaubert.dirty.client.DirtyPost;
import com.shaubert.dirty.client.DirtyRecord.Image;
import com.shaubert.dirty.db.DirtyContract.DirtyCommentEntity;

import android.database.Cursor;
import android.database.CursorWrapper;

import java.util.Date;

public class CommentsCursor extends CursorWrapper {

    private int id;
    private int title;
    private int message;
    private int imageUrls;
    private int videoUrl;
    private int creationDate;
    private int author;
    private int authorLink;
    private int serverId;
    private int votesCount;
    private int formattedMessage;
    private int insertionTime;
    private int postLocalId;
    private int indentLevel;
    private int order;
    
    public CommentsCursor(Cursor cursor) {
        super(cursor);
        findColumnIndecies(cursor);
    }

    private void findColumnIndecies(Cursor cursor) {
        id = cursor.getColumnIndex(DirtyCommentEntity.ID);
        title = cursor.getColumnIndex(DirtyCommentEntity.TITLE);
        message = cursor.getColumnIndex(DirtyCommentEntity.MESSAGE);
        imageUrls = cursor.getColumnIndex(DirtyCommentEntity.IMAGE_URLS);
        videoUrl = cursor.getColumnIndex(DirtyCommentEntity.VIDEO_URL);
        creationDate = cursor.getColumnIndex(DirtyCommentEntity.CREATION_DATE);
        author = cursor.getColumnIndex(DirtyCommentEntity.AUTHOR);
        authorLink = cursor.getColumnIndex(DirtyCommentEntity.AUTHOR_LINK);
        serverId = cursor.getColumnIndex(DirtyCommentEntity.SERVER_ID);
        votesCount = cursor.getColumnIndex(DirtyCommentEntity.VOTES_COUNT);
        formattedMessage = cursor.getColumnIndex(DirtyCommentEntity.FORMATTED_MESSAGE);
        insertionTime = cursor.getColumnIndex(DirtyCommentEntity.INSERT_TIME);
        postLocalId = cursor.getColumnIndex(DirtyCommentEntity.POST);
        indentLevel = cursor.getColumnIndex(DirtyCommentEntity.INDENT_LEVEL);
        order = cursor.getColumnIndex(DirtyCommentEntity.COMMENTS_ORDER);
    }

    public long getId() {
        return getLong(id);
    }

    public String getTitle() {
        return getString(title);
    }

    public String getMessage() {
        return getString(message);
    }

    public Image[] getImages() {
        return DirtyPost.parseImages(getString(imageUrls));
    }

    public String getVideoUrl() {
        return getString(videoUrl);
    }
    
    public Date getCreationDate() {
        return new Date(getLong(creationDate));
    }

    public String getAuthor() {
        return getString(author);
    }

    public String getAuthorLink() {
        return getString(authorLink);
    }

    public long getServerId() {
        return getLong(serverId);
    }

    public int getVotesCount() {
        return getInt(votesCount);
    }

    public String getFormattedMessage() {
        return getString(formattedMessage);
    }

    public Date getInsertionTime() {
        return new Date(getLong(insertionTime));
    }

    public long getPostLocalId() {
        return getLong(postLocalId);
    }

    public int getIndentLevel() {
        return getInt(indentLevel);
    }

    public int getOrder() {
        return getInt(order);
    }

    @Override
    protected void finalize() throws Throwable {
        //fix for motorola devices, which closes original cursor here 
    }
    
}
