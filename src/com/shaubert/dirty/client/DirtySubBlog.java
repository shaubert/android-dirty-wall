package com.shaubert.dirty.client;

import android.content.ContentValues;

import com.shaubert.dirty.db.ContentValuesState;
import com.shaubert.dirty.db.DirtyContract.DirtyBlogEntity;

public class DirtySubBlog {

	protected ContentValuesState values;

	public DirtySubBlog() {
		this(null);
	}

	public DirtySubBlog(ContentValues values) {
		if (values != null) {
			this.values = new ContentValuesState(values);
		} else {
			this.values = new ContentValuesState(new ContentValues());
		}
	}

	public ContentValues getValues() {
        return values.asContentValues();
    }
	
	public long getId() {
		return values.getAsLong(DirtyBlogEntity.ID, -1L);
	}
	
	public void setId(long id) {
		values.put(DirtyBlogEntity.ID, id);
	}
	
	public long getBlogId() {
		return values.getAsLong(DirtyBlogEntity.BLOG_ID, -1L);
	}
	
	public void setBlogId(long blogId) {
		values.put(DirtyBlogEntity.BLOG_ID, blogId);
	}

	public String getAuthor() {
		return values.getAsString(DirtyBlogEntity.AUTHOR);
	}
	
	public void setAuthor(String author) {
		values.put(DirtyBlogEntity.AUTHOR, author);
	}
	
	public long getAuthorId() {
		return values.getAsLong(DirtyBlogEntity.AUTHOR_ID, -1L);
	}
	
	public void setAuthorId(long authorId) {
		values.put(DirtyBlogEntity.AUTHOR_ID, authorId);
	}
	
	public String getTitle() {
		return values.getAsString(DirtyBlogEntity.TITLE);
	}
	
	public void setTitle(String title) {
		values.put(DirtyBlogEntity.TITLE, title);
	}
	
	public String getName() {
		return values.getAsString(DirtyBlogEntity.NAME);
	}
	
	public void setName(String name) {
		values.put(DirtyBlogEntity.NAME, name);
	}
	
	public String getDescription() {
		return values.getAsString(DirtyBlogEntity.DESCRIPTION);
	}
	
	public void setDescription(String description) {
		values.put(DirtyBlogEntity.DESCRIPTION, description);
	}
	
	public String getUrl() {
		return values.getAsString(DirtyBlogEntity.URL);
	}
	
	public void setUrl(String url) {
		values.put(DirtyBlogEntity.URL, url);
	}
	
	public int getReadersCount() {
		return values.getAsInteger(DirtyBlogEntity.READERS_COUNT, 0);
	}
	
	public void setReadersCount(int readersCount) {
		values.put(DirtyBlogEntity.READERS_COUNT, readersCount);
	}
	
	public boolean isFavorite() {
		return values.getAsBoolean(DirtyBlogEntity.FAVORITE, false);
	}
	
	public void setFavorite(boolean favorite) {
		values.putBooleanAsInt(DirtyBlogEntity.READERS_COUNT, favorite);
	}
}