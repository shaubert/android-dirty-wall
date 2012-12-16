package com.shaubert.dirty.db;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.shaubert.dirty.db.DirtyContract.DirtyBlogEntity;

public class BlogsCursor extends CursorWrapper {

    private int id;
    private int blogId;
    private int title;
    private int name;
    private int description;
    private int url;
    private int author;
    private int authorId;
    private int readersCount;
    private int favorite;
    
	public BlogsCursor(Cursor cursor) {
		super(cursor);
        findColumnIndecies(cursor);
    }

    private void findColumnIndecies(Cursor cursor) {
    	id = cursor.getColumnIndex(DirtyBlogEntity.ID);
    	blogId = cursor.getColumnIndex(DirtyBlogEntity.BLOG_ID);
    	title = cursor.getColumnIndex(DirtyBlogEntity.TITLE);
    	name = cursor.getColumnIndex(DirtyBlogEntity.NAME);
    	description = cursor.getColumnIndex(DirtyBlogEntity.DESCRIPTION);
    	url = cursor.getColumnIndex(DirtyBlogEntity.URL);
    	author = cursor.getColumnIndex(DirtyBlogEntity.AUTHOR);
    	authorId = cursor.getColumnIndex(DirtyBlogEntity.AUTHOR_ID);
    	readersCount = cursor.getColumnIndex(DirtyBlogEntity.READERS_COUNT);
    	favorite = cursor.getColumnIndex(DirtyBlogEntity.FAVORITE);
    }
    
    public long getId() {
		return getLong(id);
	}
    
    public long getBlogId() {
		return getLong(blogId);
	}
    
    public String getTitle() {
		return getString(title);
	}
    
    public String getName() {
		return getString(name);
	}

    public String getDescription() {
		return getString(description);
	}
    
    public String getUrl() {
		return getString(url);
	}
    
    public String getAuthor() {
		return getString(author);
	}
    
    public long getAuthorId() {
		return getLong(authorId);
	}
    
    public int getReadersCount() {
		return getInt(readersCount);
	}
    
    public boolean isFavorite() {
		return getInt(favorite) != 0;
	}
    
	@Override
    protected void finalize() throws Throwable {
        //fix for motorola devices, which closes original cursor here 
    }
	
}
