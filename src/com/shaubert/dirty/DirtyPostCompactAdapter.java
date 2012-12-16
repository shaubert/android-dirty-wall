package com.shaubert.dirty;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SectionIndexer;
import com.shaubert.dirty.db.DirtyContract.DirtyCommentEntity;
import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;
import com.shaubert.dirty.db.PostsCursor;
import com.shaubert.dirty.db.SqlHelper;
import com.shaubert.util.Dates;
import com.shaubert.util.FasterScrollerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DirtyPostCompactAdapter extends CursorAdapter implements LoaderCallbacks<Cursor>, SectionIndexer {

    public interface OnLoadCompleteListener {
        void onLoadComplete(DirtyPostCompactAdapter adapter);
    }
	
	private FragmentActivity fragmentActivity;
	private PostsCursor postsCursor;
	private boolean showOnlyFavorites;
    private String subBlogUrl;
	
	private DateFormat dateFormat;
	private Date[] sections;
	private String[] sectionLabels;
	private FasterScrollerView fasterScrollerView;
	
	private OnLoadCompleteListener loadCompleteListener;
	
	public DirtyPostCompactAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity, null, 0);
        this.fragmentActivity = fragmentActivity;
        this.dateFormat = new SimpleDateFormat(fragmentActivity.getString(R.string.post_list_header_date_format), new Locale("ru"));
        this.dateFormat.setTimeZone(Dates.GMT);
        
        fragmentActivity.getSupportLoaderManager().initLoader(Loaders.DIRTY_POSTS_LOADER, null, this);
	}
	
	public void setFasterScrollerView(FasterScrollerView fasterScrollerView) {
		this.fasterScrollerView = fasterScrollerView;
	}

    public String getSubBlogUrl() {
        return subBlogUrl;
    }

    public void setSubBlogUrl(String subBlogUrl) {
        this.subBlogUrl = subBlogUrl;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new DirtyPostCompactView(context);
    }
    
    public void setLoadCompleteListener(OnLoadCompleteListener loadCompleteListener) {
		this.loadCompleteListener = loadCompleteListener;
	}

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    	DirtyPostCompactView dirtyPostCompactView = (DirtyPostCompactView)view;
    	dirtyPostCompactView.swapData(postsCursor);
    	int pos = postsCursor.getPosition(); 
    	int section = getSectionForPosition(pos);
		if (pos == getPositionForSection(section)) {
    		postsCursor.moveToPosition(pos);
    		dirtyPostCompactView.setHeader(sectionLabels[section]);
    		dirtyPostCompactView.setHeaderVisible(true);
    	} else {
    		dirtyPostCompactView.setHeaderVisible(false);
    	}
    }
    
    private String formatHeaderText(PostsCursor postsCursor) {
    	Date creationDate = postsCursor.getCreationDate();
        if (creationDate.getTime() > 0) {
            if (Dates.isToday(creationDate, Dates.GMT)) {
                return fragmentActivity.getString(R.string.today);
            } else if (Dates.isYesterday(creationDate, Dates.GMT)) {
                return fragmentActivity.getString(R.string.yesterday);
            } else {
                return dateFormat.format(creationDate);
            }
        } else {
            return "?";
        }
	}

	public void setShowOnlyFavorites(boolean showOnlyFavorites) {
        this.showOnlyFavorites = showOnlyFavorites;
    }
	
	public boolean isShowOnlyFavorites() {
		return showOnlyFavorites;
	}

    public int getPostPosition(long postId) {
    	if (postsCursor != null && postsCursor.moveToFirst()) {
    		do {
    			if (postsCursor.getId() == postId) {
    				return postsCursor.getPosition();
    			}
    		} while (postsCursor.moveToNext());
    	}
    	return -1;
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(fragmentActivity, 
                DirtyPostEntity.URI, 
                new String[] { DirtyPostEntity.ID, DirtyPostEntity.COMMENTS_COUNT, DirtyPostEntity.GOLDEN,
        			DirtyPostEntity.FAVORITE, DirtyPostEntity.SUB_BLOG_NAME, DirtyPostEntity.UNREAD, DirtyCommentEntity.AUTHOR, 
        			DirtyCommentEntity.AUTHOR_LINK, DirtyCommentEntity.CREATION_DATE, 
        			DirtyCommentEntity.SERVER_ID, DirtyCommentEntity.VOTES_COUNT, 
        			DirtyCommentEntity.MESSAGE},
                    SqlHelper.buildAndSelection(showOnlyFavorites ? (DirtyPostEntity.FAVORITE + " != 0") : null,
                            !TextUtils.isEmpty(subBlogUrl) ? (DirtyPostEntity.SUB_BLOG_NAME + " = ?") : null),
                    TextUtils.isEmpty(subBlogUrl) ? null : new String[] {subBlogUrl},
                    DirtyPostEntity.CREATION_DATE + " DESC");
        loader.setUpdateThrottle(1000);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    	postsCursor = new PostsCursor(data);
    	refreshSections();
    	if (fasterScrollerView != null) {
    		fasterScrollerView.updateSections(false);
    	}
    	if (loadCompleteListener != null && data != null && data.getCount() > 0) {
    		loadCompleteListener.onLoadComplete(this);
    	}
    	swapCursor(postsCursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    	postsCursor = null;
    	refreshSections();
        swapCursor(null);
    }

    private void refreshSections() {
    	List<Date> days = new ArrayList<Date>();
    	List<String> headers = new ArrayList<String>();
		if (postsCursor != null && postsCursor.moveToFirst()) {
			Date prevDate = null;
			do {
				Date newDate = postsCursor.getCreationDate();
				if (prevDate == null || !Dates.isSameDay(newDate, prevDate, Dates.GMT)) {
					days.add(newDate);
					headers.add(formatHeaderText(postsCursor));
				}
				prevDate = newDate; 
			} while (postsCursor.moveToNext());
		}
		sections = new Date[days.size()];
		days.toArray(sections);
		sectionLabels = new String[headers.size()];
		headers.toArray(sectionLabels);
    }
    
	@Override
	public Object[] getSections() {
		return sectionLabels;
	}

	@Override
	public int getPositionForSection(int section) {
		Date date = sections[Math.min(section, sections.length - 1)];
		if (postsCursor != null && postsCursor.moveToFirst()) {
			do {
				if (Dates.isSameDay(postsCursor.getCreationDate(), date, Dates.GMT)) {
					return postsCursor.getPosition();
				}
			} while (postsCursor.moveToNext());
		}
		return section;
	}

	@Override
	public int getSectionForPosition(int position) {
		if (postsCursor != null && postsCursor.moveToPosition(position)) {
			Date date = postsCursor.getCreationDate();
			for (int i = 0; i < sections.length; i++) {
				if (Dates.isSameDay(date, sections[i], Dates.GMT)) {
					return i;
				}
			}
		}
		return sections.length - 1;
	}
	
}
