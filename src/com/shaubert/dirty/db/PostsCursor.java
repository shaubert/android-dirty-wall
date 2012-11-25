package com.shaubert.dirty.db;

import java.util.Date;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.shaubert.dirty.client.DirtyPost;
import com.shaubert.dirty.client.DirtyRecord.Image;
import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;

public class PostsCursor extends CursorWrapper {
	
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
    private int golden;
    private int unread;
    private int favorite;
    private int commentsCount;
    private int subBlogName;
    
    public PostsCursor(Cursor cursor) {
        super(cursor);
        findColumnIndecies(cursor);
    }

    private void findColumnIndecies(Cursor cursor) {
        id = cursor.getColumnIndex(DirtyPostEntity.ID);
        title = cursor.getColumnIndex(DirtyPostEntity.TITLE);
        message = cursor.getColumnIndex(DirtyPostEntity.MESSAGE);
        imageUrls = cursor.getColumnIndex(DirtyPostEntity.IMAGE_URLS);
        videoUrl = cursor.getColumnIndex(DirtyPostEntity.VIDEO_URL);
        creationDate = cursor.getColumnIndex(DirtyPostEntity.CREATION_DATE);
        author = cursor.getColumnIndex(DirtyPostEntity.AUTHOR);
        authorLink = cursor.getColumnIndex(DirtyPostEntity.AUTHOR_LINK);
        serverId = cursor.getColumnIndex(DirtyPostEntity.SERVER_ID);
        votesCount = cursor.getColumnIndex(DirtyPostEntity.VOTES_COUNT);
        formattedMessage = cursor.getColumnIndex(DirtyPostEntity.FORMATTED_MESSAGE);
        insertionTime = cursor.getColumnIndex(DirtyPostEntity.INSERT_TIME);
        commentsCount = cursor.getColumnIndex(DirtyPostEntity.COMMENTS_COUNT);
        golden = cursor.getColumnIndex(DirtyPostEntity.GOLDEN);
        unread = cursor.getColumnIndex(DirtyPostEntity.UNREAD);
        favorite = cursor.getColumnIndex(DirtyPostEntity.FAVORITE);
        subBlogName = cursor.getColumnIndexOrThrow(DirtyPostEntity.SUB_BLOG_NAME);
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

    public int getCommentsCount() {
		return getInt(commentsCount);
	}
    
    public boolean isGolden() {
		return getInt(golden) != 0;
	}
    
    public boolean isFavorite() {
		return getInt(favorite) != 0;
	}
    
    public boolean isUnread() {
    	return getInt(unread) != 0;
    }
    
    public String getSubBlogName() {
		return getString(subBlogName);
	}
    
    @Override
    protected void finalize() throws Throwable {
        //fix for motorola devices, which closes original cursor here 
    }
}
