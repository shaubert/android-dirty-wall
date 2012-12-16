package com.shaubert.dirty.net;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import com.shaubert.blogadapter.client.Pager;
import com.shaubert.dirty.client.DirtyBlog;
import com.shaubert.dirty.client.DirtySubBlog;
import com.shaubert.dirty.db.DirtyContract;
import com.shaubert.dirty.db.DirtyContract.DirtyBlogEntity;
import com.shaubert.net.core.RequestBase;
import com.shaubert.net.core.RequestStateBase;
import com.shaubert.net.nutshell.ExecutionContext;
import com.shaubert.util.Shlog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DirtyBlogsLoadRequest extends RequestBase {

	public static final String OFFSET_PARAM = "offset";
    public static final String COUNT_PARAM = "count";
	public static final String END_PARAM = "end";
	
	public static final Shlog SHLOG = new Shlog(DirtyBlogsLoadRequest.class.getSimpleName());
	
	public DirtyBlogsLoadRequest() {
		this(null);
	}
	
	public DirtyBlogsLoadRequest(RequestStateBase state) {
		super(state);
	}

	@Override
	public void execute(ExecutionContext executionContext) throws Exception {
		int offset = getState().getInt(OFFSET_PARAM, -1);
		if (offset == -1) {
			offset = getBlogsCount(executionContext);
		}
        int count = getState().getInt(COUNT_PARAM, -1);

        int loadedCounter = 0;
        do {
            int loadedCount = loadBlogFromOffset(executionContext, offset);
            if (loadedCount == 0) {
                break;
            }
            loadedCounter += loadedCount;
        } while (loadedCounter < count);
	}

    private int loadBlogFromOffset(ExecutionContext executionContext, int offset) throws IOException, RemoteException, OperationApplicationException {
        String timerName = "overall blogs loading and parsing time";
        SHLOG.resetTimer(timerName);
        Pager<DirtySubBlog> pager = createPager(offset);
        SHLOG.d("loading blogs from offset: " + offset);
        List<DirtySubBlog> blogs = pager.loadNext();
        SHLOG.logTimer(timerName);
        if (!pager.hasNext()) {
            getState().put(END_PARAM, true);
            SHLOG.d("loaded all blogs");
        }
        ArrayList<ContentProviderOperation> ops = convertToOperations(executionContext, blogs);
        timerName = "apply batch of " + ops.size() + " cpo";
        SHLOG.resetTimer(timerName);
        executionContext.getContext().getContentResolver().applyBatch(DirtyContract.AUTHORITY, ops);
        SHLOG.logTimer(timerName);
        return blogs.size();
    }

    private int getBlogsCount(ExecutionContext executionContext) {
		Cursor currentBlogs = executionContext.getContext().getContentResolver().query(
                DirtyBlogEntity.URI, new String [] { DirtyBlogEntity.ID }, null, null, null);
		int res = currentBlogs.getCount();
		currentBlogs.close();
		return res;
	}

	private ArrayList<ContentProviderOperation> convertToOperations(ExecutionContext executionContext, List<DirtySubBlog> blogs) {
		ArrayList<ContentProviderOperation> result = new ArrayList<ContentProviderOperation>();
		Cursor currentBlogs = executionContext.getContext().getContentResolver().query(
                DirtyBlogEntity.URI, new String [] { DirtyBlogEntity.ID, DirtyBlogEntity.BLOG_ID }, null, null, null);
        try {
            for (DirtySubBlog blog : blogs) {
            	if (moveCursorToPostWithBlogId(currentBlogs, blog.getBlogId())) {
            		blog.setId(currentBlogs.getLong(0));
            	}
                result.add(convertToOperation(blog));
            }
        } finally {
            currentBlogs.close();
        }
        return result;
    }

    private ContentProviderOperation convertToOperation(DirtySubBlog blog) {
        if (blog.getId() >= 0) {
            return ContentProviderOperation
                    .newUpdate(ContentUris.withAppendedId(DirtyBlogEntity.URI, blog.getId()))
                    .withValues(blog.getValues())
                    .build();
        } else {
            return ContentProviderOperation
                    .newInsert(DirtyBlogEntity.URI)
                    .withValues(blog.getValues())
                    .build();
        }
    }

    private boolean moveCursorToPostWithBlogId(Cursor currentBlogs, long blogId) {
        if (currentBlogs.moveToFirst()) {
            do {
                if (currentBlogs.getLong(1) == blogId) {
                    return true;
                }
            } while (currentBlogs.moveToNext());
        }
        return false;
    }


	private Pager<DirtySubBlog> createPager(int offset) {
		return DirtyBlog.getInstance().createBlogsPager(offset);
	}

}
